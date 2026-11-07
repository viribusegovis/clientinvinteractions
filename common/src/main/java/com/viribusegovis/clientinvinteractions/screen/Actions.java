package com.viribusegovis.clientinvinteractions.screen;

import com.viribusegovis.clientinvinteractions.config.ClientConfig;
import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.config.SortOrder;
import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import com.viribusegovis.clientinvinteractions.inventory.SlotRegions;
import com.viribusegovis.clientinvinteractions.inventory.Sorter;
import com.viribusegovis.clientinvinteractions.inventory.SophisticatedSort;
import com.viribusegovis.clientinvinteractions.inventory.Transfers;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.List;
import java.util.stream.Stream;

/** The mod's actions, triggered by buttons and hotkeys alike. Config is read at action time. */
final class Actions {
    private Actions() {
    }

    static void sortContainer(Target target) {
        SortOrder order = ConfigStore.current().sortOrder();
        if (SophisticatedSort.isSophisticatedMenu(target.menu())) {
            SophisticatedSort.requestSort(target.menu(), order);
        } else {
            sort(target, target.container(), order);
        }
    }

    static void sortPlayer(Target target) {
        sort(target, target.playerMain(), ConfigStore.current().sortOrder());
    }

    static void takeAll(Target target) {
        Transfers.moveAll(target.menu(), target.container());
    }

    static void moveAll(Target target) {
        Transfers.moveAll(target.menu(), transferSources(target));
    }

    static void merge(Target target) {
        Transfers.merge(target.menu(), transferSources(target), target.container());
    }

    static void cycleSortOrder(LocalPlayer player) {
        ClientConfig config = ConfigStore.current();
        SortOrder[] orders = SortOrder.values();
        SortOrder next = orders[(config.sortOrder().ordinal() + 1) % orders.length];
        ConfigStore.update(config.withSortOrder(next));
        DebugLog.log("sort order changed from={} to={}", config.sortOrder(), next);
        player.displayClientMessage(Component.translatable("clientinvinteractions.sort_order.changed", next.label()), true);
    }

    /** Player main inventory, plus the hotbar when configured or while Ctrl is held. */
    private static List<Slot> transferSources(Target target) {
        boolean includeHotbar = ConfigStore.current().includeHotbar() || Screen.hasControlDown();
        if (!includeHotbar) {
            return target.playerMain();
        }
        List<Slot> hotbar = SlotRegions.playerHotbarSlots(target.menu(), target.player().getInventory());
        return Stream.concat(target.playerMain().stream(), hotbar.stream()).toList();
    }

    private static void sort(Target target, List<Slot> region, SortOrder order) {
        Sorter.Result result = Sorter.sort(target.menu(), target.player(), region, order);
        DebugLog.log("sort result={} slots={} order={}", result, region.size(), order);
        switch (result) {
            case SORTED -> {
            }
            case UNSUPPORTED -> target.player().displayClientMessage(
                    Component.translatable("clientinvinteractions.sort.unsupported"), true);
            case INTERRUPTED -> target.player().displayClientMessage(
                    Component.translatable("clientinvinteractions.sort.interrupted"), true);
            case NEEDS_FREE_SLOT -> target.player().displayClientMessage(
                    Component.translatable("clientinvinteractions.sort.needs_free_slot"), true);
        }
    }
}
