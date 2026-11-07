package com.viribusegovis.clientinvinteractions.inventory;

import com.viribusegovis.clientinvinteractions.config.SortOrder;
import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Sorts a slot region using only vanilla left-clicks, so it works on servers without the mod.
 *
 * <p>Target layout: one entry per distinct item (same item and NBT), ordered by the chosen {@link SortOrder},
 * split into stacks as large as the slots allow, followed by one remainder stack. Positions are filled in order;
 * position {@code i} is finished before {@code i + 1}, and every item not yet placed always sits in a slot after
 * {@code i}, so each step finds its source stack there.
 *
 * <p>Two strategies share that layout:
 * <ul>
 *   <li>Normal slots (capacity = the item's max stack): a whole stack fits on the cursor, so a wrong stack is
 *   swapped out with one click. Works on completely full containers.</li>
 *   <li>Large slots (capacity above the item's max stack, e.g. stack upgrades): the cursor only carries part of a
 *   slot, so stacks are moved in several pick-up/put-down rounds. Stacks are first merged up to capacity, and a
 *   wrong stack is evicted into an empty slot, so a full container needs one free slot.</li>
 * </ul>
 */
public final class Sorter {
    private Sorter() {
    }

    /** All stacks of one item variant in the region, and how many fit in one of its slots. */
    private record Group(ItemStack prototype, int count, int capacity) {
    }

    public enum Result {
        SORTED,
        /** Nothing was clicked. */
        UNSUPPORTED,
        /** A click behaved unexpectedly; sorting stopped part way. */
        INTERRUPTED,
        /** Large-slot sorting needed an empty slot to move a stack out of the way and found none. */
        NEEDS_FREE_SLOT
    }

    /** Returns UNSUPPORTED without clicking anything if the region can't be sorted with plain clicks. */
    public static Result sort(AbstractContainerMenu menu, Player player, List<Slot> region, SortOrder order) {
        if (!menu.getCarried().isEmpty() || !isClickable(region, player)) {
            return Result.UNSUPPORTED;
        }
        Optional<List<Group>> groups = groups(region);
        if (groups.isEmpty()) {
            return Result.UNSUPPORTED;
        }
        List<ItemStack> targets = targetLayout(groups.get(), order);
        // Slots holding more than their reported limit (edited stacks, removed upgrades, handler slots that report
        // a 64 cap) make the layout meaningless and could need more positions than there are slots.
        if (isOverCapacity(region, groups.get()) || targets.size() > region.size()) {
            return Result.UNSUPPORTED;
        }
        boolean largeSlots = groups.get().stream().anyMatch(g -> g.capacity() > g.prototype().getMaxStackSize());
        DebugLog.log("sort plan slots={} itemGroups={} targetStacks={} largeSlots={}",
                region.size(), groups.get().size(), targets.size(), largeSlots);
        if (!largeSlots) {
            return sortNormalSlots(menu, region, targets);
        }
        // Consolidation can't free a slot when the layout already fills the region, and evicting needs one;
        // report that before clicking anything rather than after a partial rearrangement.
        if (targets.size() == region.size() && !matchesLayout(region, targets)) {
            return Result.NEEDS_FREE_SLOT;
        }
        return sortLargeSlots(menu, region, groups.get(), targets);
    }

    private static boolean isOverCapacity(List<Slot> region, List<Group> groups) {
        return region.stream()
                .filter(Slot::hasItem)
                .anyMatch(slot -> slot.getItem().getCount() > groups.get(indexOfMatch(groups, slot.getItem())).capacity());
    }

    private static boolean matchesLayout(List<Slot> region, List<ItemStack> targets) {
        for (int i = 0; i < targets.size(); i++) {
            ItemStack current = region.get(i).getItem();
            if (!ItemStack.isSameItemSameTags(current, targets.get(i)) || current.getCount() != targets.get(i).getCount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isClickable(List<Slot> region, Player player) {
        for (Slot slot : region) {
            ItemStack stack = slot.getItem();
            // Bundles absorb items on click instead of swapping.
            if (!stack.isEmpty() && (!slot.mayPickup(player) || stack.getItem() instanceof BundleItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Empty if some slot rejects an item of the region or slots disagree on capacity (filtered or mixed slots),
     * since the layout assumes any slot can hold any chunk.
     */
    private static Optional<List<Group>> groups(List<Slot> region) {
        List<Group> groups = new ArrayList<>();
        for (Slot slot : region) {
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            int index = indexOfMatch(groups, stack);
            if (index >= 0) {
                Group existing = groups.get(index);
                groups.set(index, new Group(existing.prototype(), existing.count() + stack.getCount(), existing.capacity()));
                continue;
            }
            ItemStack prototype = stack.copyWithCount(1);
            OptionalInt capacity = uniformCapacity(region, prototype);
            if (capacity.isEmpty()) {
                return Optional.empty();
            }
            groups.add(new Group(prototype, stack.getCount(), capacity.getAsInt()));
        }
        return Optional.of(groups);
    }

    private static OptionalInt uniformCapacity(List<Slot> region, ItemStack prototype) {
        int capacity = region.get(0).getMaxStackSize(prototype);
        for (Slot slot : region) {
            if (!slot.mayPlace(prototype) || slot.getMaxStackSize(prototype) != capacity) {
                return OptionalInt.empty();
            }
        }
        return capacity >= prototype.getMaxStackSize() ? OptionalInt.of(capacity) : OptionalInt.empty();
    }

    private static int indexOfMatch(List<Group> groups, ItemStack stack) {
        for (int i = 0; i < groups.size(); i++) {
            if (ItemStack.isSameItemSameTags(groups.get(i).prototype(), stack)) {
                return i;
            }
        }
        return -1;
    }

    private static List<ItemStack> targetLayout(List<Group> groups, SortOrder order) {
        List<ItemStack> targets = new ArrayList<>();
        groups.stream()
                .sorted(groupComparator(order))
                .forEach(group -> {
                    for (int left = group.count(); left > 0; left -= group.capacity()) {
                        targets.add(group.prototype().copyWithCount(Math.min(group.capacity(), left)));
                    }
                });
        return targets;
    }

    /** COUNT puts the item with the most units first; ties and every other order fall back to the item order. */
    private static Comparator<Group> groupComparator(SortOrder order) {
        Comparator<Group> byItem = Comparator.comparing(Group::prototype, comparator(order));
        return order == SortOrder.COUNT
                ? Comparator.comparingInt(Group::count).reversed().thenComparing(byItem)
                : byItem;
    }

    private static Comparator<ItemStack> comparator(SortOrder order) {
        Comparator<ItemStack> byName = Comparator.comparing(stack -> stack.getHoverName().getString());
        Comparator<ItemStack> byId = Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        Comparator<ItemStack> byMod = Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
        // NBT string as the final tie-break keeps variants (enchanted books, potions) grouped deterministically.
        Comparator<ItemStack> byTag = Comparator.comparing(stack -> Objects.toString(stack.getTag(), ""));
        Comparator<ItemStack> primary = switch (order) {
            case NAME, COUNT -> byName.thenComparing(byId);
            case MOD -> byMod.thenComparing(byName).thenComparing(byId);
            case ID -> byId;
        };
        return primary.thenComparing(byTag);
    }

    private static Optional<Slot> laterSlotWith(List<Slot> region, int position, ItemStack target) {
        return region.subList(position + 1, region.size()).stream()
                .filter(slot -> ItemStack.isSameItemSameTags(slot.getItem(), target))
                .findFirst();
    }

    // ---- Normal slots -------------------------------------------------------------------------------------

    private static Result sortNormalSlots(AbstractContainerMenu menu, List<Slot> region, List<ItemStack> targets) {
        for (int i = 0; i < targets.size(); i++) {
            if (!fillNormalPosition(menu, region, i, targets.get(i))) {
                return Result.INTERRUPTED;
            }
        }
        return Result.SORTED;
    }

    /**
     * Returns false as soon as a click doesn't do what vanilla rules predict (an item with custom click behavior,
     * or a slot with hidden rules), leaving the remaining positions unsorted instead of looping or scrambling.
     */
    private static boolean fillNormalPosition(AbstractContainerMenu menu, List<Slot> region, int position, ItemStack target) {
        Slot destination = region.get(position);
        ItemStack current = destination.getItem();
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, target)) {
            // Pick up the wrong stack, swap it with a later stack of the target item, put that one here.
            Optional<Slot> source = laterSlotWith(region, position, target);
            if (source.isEmpty()) {
                return false;
            }
            Clicks.leftClick(menu, destination);
            Clicks.leftClick(menu, source.get());
            Clicks.leftClick(menu, destination);
        } else if (current.isEmpty()) {
            Optional<Slot> source = laterSlotWith(region, position, target);
            if (source.isEmpty()) {
                return false;
            }
            Clicks.leftClick(menu, source.get());
            Clicks.leftClick(menu, destination);
        }
        if (!menu.getCarried().isEmpty() || !ItemStack.isSameItemSameTags(destination.getItem(), target)) {
            return false;
        }
        while (destination.getItem().getCount() < target.getCount()) {
            int before = destination.getItem().getCount();
            Optional<Slot> source = laterSlotWith(region, position, target);
            if (source.isEmpty()) {
                return false;
            }
            Clicks.leftClick(menu, source.get());
            Clicks.leftClick(menu, destination);
            if (!menu.getCarried().isEmpty()) {
                // Destination filled up; the leftover goes back to the slot it came from, which is now empty.
                Clicks.leftClick(menu, source.get());
            }
            if (destination.getItem().getCount() <= before || !menu.getCarried().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // ---- Large slots --------------------------------------------------------------------------------------

    private static Result sortLargeSlots(AbstractContainerMenu menu, List<Slot> region, List<Group> groups,
                                         List<ItemStack> targets) {
        for (Group group : groups) {
            if (!consolidate(menu, region, group)) {
                return Result.INTERRUPTED;
            }
        }
        for (int i = 0; i < targets.size(); i++) {
            Result result = fillLargePosition(menu, region, i, targets.get(i));
            if (result != Result.SORTED) {
                return result;
            }
        }
        return Result.SORTED;
    }

    /** Merges the group's stacks so every slot of it is at capacity except at most one. */
    private static boolean consolidate(AbstractContainerMenu menu, List<Slot> region, Group group) {
        while (true) {
            List<Slot> slots = region.stream()
                    .filter(slot -> ItemStack.isSameItemSameTags(slot.getItem(), group.prototype()))
                    .toList();
            Optional<Slot> notFull = slots.stream()
                    .filter(slot -> slot.getItem().getCount() < group.capacity())
                    .findFirst();
            if (notFull.isEmpty() || slots.get(slots.size() - 1) == notFull.get()) {
                return true;
            }
            if (!moveStack(menu, slots.get(slots.size() - 1), notFull.get())) {
                return false;
            }
        }
    }

    private static Result fillLargePosition(AbstractContainerMenu menu, List<Slot> region, int position, ItemStack target) {
        Slot destination = region.get(position);
        ItemStack current = destination.getItem();
        if (!current.isEmpty() && !ItemStack.isSameItemSameTags(current, target)) {
            Optional<Slot> free = region.subList(position + 1, region.size()).stream()
                    .filter(slot -> !slot.hasItem())
                    .findFirst();
            if (free.isEmpty()) {
                return Result.NEEDS_FREE_SLOT;
            }
            if (!moveStack(menu, destination, free.get()) || destination.hasItem()) {
                return Result.INTERRUPTED;
            }
        }
        while (destination.getItem().getCount() < target.getCount()) {
            Optional<Slot> source = laterSlotWith(region, position, target);
            if (source.isEmpty() || !moveStack(menu, source.get(), destination)) {
                return Result.INTERRUPTED;
            }
        }
        return Result.SORTED;
    }

    /**
     * Moves the source stack into the target (empty, or the same item with room) in cursor-sized rounds until the
     * source is empty or the target is full. Returns false if a round made no progress or left items on the cursor.
     */
    private static boolean moveStack(AbstractContainerMenu menu, Slot from, Slot to) {
        while (from.hasItem()) {
            int before = to.getItem().getCount();
            Clicks.leftClick(menu, from);
            if (menu.getCarried().isEmpty()) {
                return false;
            }
            Clicks.leftClick(menu, to);
            boolean targetFull = !menu.getCarried().isEmpty();
            if (targetFull) {
                // The leftover goes back where it came from; that slot holds the same item or is empty.
                Clicks.leftClick(menu, from);
            }
            if (!menu.getCarried().isEmpty() || to.getItem().getCount() <= before) {
                return false;
            }
            if (targetFull) {
                return true;
            }
        }
        return true;
    }
}
