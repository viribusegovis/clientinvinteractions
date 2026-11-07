package com.viribusegovis.clientinvinteractions.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Shift-click based transfers between the container and the player inventory. */
public final class Transfers {
    private Transfers() {
    }

    /** Shift-clicks every source stack whose item the container already holds. */
    public static void merge(AbstractContainerMenu menu, List<Slot> sources, List<Slot> container) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        for (Slot source : sources) {
            ItemStack stack = source.getItem();
            if (!stack.isEmpty() && containsMatch(container, stack)) {
                Clicks.shiftClick(menu, source);
            }
        }
    }

    /** Shift-clicks every non-empty source stack. */
    public static void moveAll(AbstractContainerMenu menu, List<Slot> sources) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        for (Slot source : sources) {
            if (source.hasItem()) {
                Clicks.shiftClick(menu, source);
            }
        }
    }

    private static boolean containsMatch(List<Slot> slots, ItemStack stack) {
        return slots.stream().anyMatch(slot -> ItemStack.isSameItemSameComponents(slot.getItem(), stack));
    }
}
