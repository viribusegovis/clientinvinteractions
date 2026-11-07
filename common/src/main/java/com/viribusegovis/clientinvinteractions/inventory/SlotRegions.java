package com.viribusegovis.clientinvinteractions.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

/** Splits a menu's slots into the container side and the player's inventory side. */
public final class SlotRegions {
    private static final int HOTBAR_SIZE = 9;
    private static final int MAIN_INVENTORY_END = 36;

    private SlotRegions() {
    }

    /**
     * Storage slots that belong to the opened block/entity, in menu order. Slots that refuse an ordinary item
     * (horse saddle/armor, machine upgrade, battery and output slots) are equipment, not storage.
     */
    public static List<Slot> containerSlots(AbstractContainerMenu menu, Inventory inventory) {
        ItemStack ordinaryItem = new ItemStack(Items.COBBLESTONE);
        return menu.slots.stream()
                .filter(slot -> slot.container != inventory)
                .filter(slot -> !(slot instanceof ResultSlot))
                .filter(slot -> !(slot.container instanceof CraftingContainer))
                .filter(Slot::isActive)
                .filter(slot -> slot.mayPlace(ordinaryItem))
                .toList();
    }

    /** Player main inventory (the 27 slots above the hotbar), in inventory order. */
    public static List<Slot> playerMainSlots(AbstractContainerMenu menu, Inventory inventory) {
        return playerSlotsInRange(menu, inventory, HOTBAR_SIZE, MAIN_INVENTORY_END);
    }

    public static List<Slot> playerHotbarSlots(AbstractContainerMenu menu, Inventory inventory) {
        return playerSlotsInRange(menu, inventory, 0, HOTBAR_SIZE);
    }

    private static List<Slot> playerSlotsInRange(AbstractContainerMenu menu, Inventory inventory, int start, int end) {
        return menu.slots.stream()
                .filter(slot -> slot.container == inventory)
                .filter(slot -> slot.getContainerSlot() >= start && slot.getContainerSlot() < end)
                .filter(Slot::isActive)
                .sorted(Comparator.comparingInt(Slot::getContainerSlot))
                .toList();
    }
}
