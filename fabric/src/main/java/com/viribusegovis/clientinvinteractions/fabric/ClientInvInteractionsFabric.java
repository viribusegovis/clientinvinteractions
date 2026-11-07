package com.viribusegovis.clientinvinteractions.fabric;

import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.fabric.mixin.AbstractContainerScreenAccessor;
import com.viribusegovis.clientinvinteractions.screen.Hotkeys;
import com.viribusegovis.clientinvinteractions.screen.ScreenAccess;
import com.viribusegovis.clientinvinteractions.screen.ScreenButtons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public final class ClientInvInteractionsFabric implements ClientModInitializer {
    public static final String MOD_ID = "clientinvinteractions";

    private static final ScreenAccess ACCESS = new ScreenAccess() {
        @Override
        public int left(AbstractContainerScreen<?> screen) {
            return ((AbstractContainerScreenAccessor) screen).clientinvinteractions$leftPos();
        }

        @Override
        public int top(AbstractContainerScreen<?> screen) {
            return ((AbstractContainerScreenAccessor) screen).clientinvinteractions$topPos();
        }

        @Override
        public Optional<Slot> hoveredSlot(AbstractContainerScreen<?> screen) {
            return Optional.ofNullable(((AbstractContainerScreenAccessor) screen).clientinvinteractions$hoveredSlot());
        }
    };

    @Override
    public void onInitializeClient() {
        ConfigStore.init(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
        // Vanilla's registry for key binding categories; Fabric hooks it to sort modded categories after vanilla's.
        KeyMapping.Category.register(Hotkeys.CATEGORY.id());
        Hotkeys.ALL.forEach(KeyMappingHelper::registerKeyMapping);
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            ScreenButtons.onInit(screen,
                    widget -> Screens.getWidgets(screen).add((AbstractWidget) widget),
                    widget -> Screens.getWidgets(screen).remove(widget),
                    ACCESS);
            // Per-screen events must be registered on every init; Fabric clears them when the screen re-inits.
            ScreenEvents.beforeExtract(screen).register((s, graphics, mouseX, mouseY, partialTick) ->
                    ScreenButtons.onRender(s, ACCESS));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) ->
                    !Hotkeys.onKeyPressed(s, mapping -> mapping.matches(event), ACCESS));
            ScreenMouseEvents.allowMouseClick(screen).register((s, event) ->
                    !Hotkeys.onMouseClicked(s, event.button(), mapping -> mapping.matchesMouse(event), ACCESS));
        });
    }
}
