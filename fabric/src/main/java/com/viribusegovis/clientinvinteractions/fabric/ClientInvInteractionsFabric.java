package com.viribusegovis.clientinvinteractions.fabric;

import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.screen.Hotkeys;
import com.viribusegovis.clientinvinteractions.screen.ScreenAccess;
import com.viribusegovis.clientinvinteractions.screen.ScreenButtons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

public final class ClientInvInteractionsFabric implements ClientModInitializer {
    private static final String MOD_ID = "clientinvinteractions";

    // leftPos/topPos/hoveredSlot are opened up by clientinvinteractions.accesswidener.
    private static final ScreenAccess ACCESS = new ScreenAccess() {
        @Override
        public int left(AbstractContainerScreen<?> screen) {
            return screen.leftPos;
        }

        @Override
        public int top(AbstractContainerScreen<?> screen) {
            return screen.topPos;
        }

        @Override
        public Optional<Slot> hoveredSlot(AbstractContainerScreen<?> screen) {
            return Optional.ofNullable(screen.hoveredSlot);
        }
    };

    @Override
    public void onInitializeClient() {
        ConfigStore.init(FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".json"));
        Hotkeys.ALL.forEach(KeyBindingHelper::registerKeyBinding);
        // Fabric recreates per-screen events on every init, so registering them here doesn't stack listeners.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            ScreenButtons.onInit(screen,
                    widget -> Screens.getButtons(screen).add((AbstractWidget) widget),
                    widget -> Screens.getButtons(screen).remove(widget),
                    ACCESS);
            ScreenEvents.beforeRender(screen).register((s, graphics, mouseX, mouseY, tickDelta) ->
                    ScreenButtons.onRender(s, ACCESS));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                    !Hotkeys.onKeyPressed(s, mapping -> mapping.matches(key, scancode), ACCESS));
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                    !Hotkeys.onMouseClicked(s, button, mapping -> mapping.matchesMouse(button), ACCESS));
        });
    }
}
