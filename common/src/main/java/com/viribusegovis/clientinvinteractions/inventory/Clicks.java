package com.viribusegovis.clientinvinteractions.inventory;

import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Sends vanilla container clicks, exactly as if the player clicked by hand. The client applies each click to its
 * own copy of the menu immediately, so callers can read the predicted slot contents right after a click.
 */
final class Clicks {
    private static final int LEFT_BUTTON = 0;

    private Clicks() {
    }

    static void leftClick(AbstractContainerMenu menu, Slot slot) {
        send(menu, slot, ClickType.PICKUP);
    }

    static void shiftClick(AbstractContainerMenu menu, Slot slot) {
        send(menu, slot, ClickType.QUICK_MOVE);
    }

    private static void send(AbstractContainerMenu menu, Slot slot, ClickType type) {
        Minecraft minecraft = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        LocalPlayer player = minecraft.player;
        if (gameMode == null || player == null) {
            throw new IllegalStateException("Container click without an active game session");
        }
        ItemStack slotBefore = slot.getItem().copy();
        ItemStack carriedBefore = menu.getCarried().copy();
        gameMode.handleInventoryMouseClick(menu.containerId, slot.index, LEFT_BUTTON, type, player);
        DebugLog.log("click container={} slot={} type={} slotBefore={} carriedBefore={} slotAfter={} carriedAfter={}",
                menu.containerId, slot.index, type, slotBefore, carriedBefore, slot.getItem(), menu.getCarried());
    }
}
