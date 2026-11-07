package com.viribusegovis.clientinvinteractions.screen;

import com.viribusegovis.clientinvinteractions.config.ClientConfig;
import com.viribusegovis.clientinvinteractions.inventory.SlotRegions;
import com.viribusegovis.clientinvinteractions.inventory.SophisticatedSort;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.Optional;

/** A container screen this mod acts on, with its slot regions resolved. Shared by buttons and hotkeys. */
record Target(AbstractContainerScreen<?> screen, LocalPlayer player, List<Slot> container, List<Slot> playerMain) {
    /** Below this many container slots (furnaces, anvils, brewing stands) the actions aren't useful. */
    private static final int MIN_CONTAINER_SLOTS = 9;
    private static final int PLAYER_MAIN_SLOTS = 27;
    private static final String SOPHISTICATED_PACKAGE = "net.p3pp3rf1y.sophisticated";

    static Optional<Target> of(Screen screen, ClientConfig config) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (!(screen instanceof AbstractContainerScreen<?> containerScreen)
                || screen instanceof CreativeModeInventoryScreen
                || player == null) {
            return Optional.empty();
        }
        AbstractContainerMenu menu = containerScreen.getMenu();
        // On Sophisticated screens only the storage itself (not e.g. its settings screen, whose filter slots aren't
        // storage), and only when the user chose to replace Sophisticated's buttons.
        if (isSophisticated(screen)
                && (!config.showOnSophisticatedScreens() || !SophisticatedSort.isSophisticatedMenu(menu))) {
            return Optional.empty();
        }
        Inventory inventory = player.getInventory();
        return Optional.of(new Target(containerScreen, player,
                SlotRegions.containerSlots(menu, inventory), SlotRegions.playerMainSlots(menu, inventory)));
    }

    static boolean isSophisticated(Screen screen) {
        return screen.getClass().getName().startsWith(SOPHISTICATED_PACKAGE);
    }

    AbstractContainerMenu menu() {
        return screen.getMenu();
    }

    boolean hasContainer() {
        return container.size() >= MIN_CONTAINER_SLOTS;
    }

    boolean hasPlayer() {
        return playerMain.size() == PLAYER_MAIN_SLOTS;
    }

    boolean isPlayerSlot(Slot slot) {
        return slot.container == player.getInventory();
    }
}
