package com.viribusegovis.clientinvinteractions.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.viribusegovis.clientinvinteractions.config.ConfigScreen;
import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.screen.Hotkeys;
import com.viribusegovis.clientinvinteractions.screen.ScreenAccess;
import com.viribusegovis.clientinvinteractions.screen.ScreenButtons;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.Optional;

final class ClientSetup {
    private static final ScreenAccess ACCESS = new ScreenAccess() {
        @Override
        public int left(AbstractContainerScreen<?> screen) {
            return screen.getGuiLeft();
        }

        @Override
        public int top(AbstractContainerScreen<?> screen) {
            return screen.getGuiTop();
        }

        @Override
        public Optional<Slot> hoveredSlot(AbstractContainerScreen<?> screen) {
            return Optional.ofNullable(screen.getSlotUnderMouse());
        }
    };

    private ClientSetup() {
    }

    static void init() {
        ConfigStore.init(FMLPaths.CONFIGDIR.get().resolve(ClientInvInteractions.MOD_ID + ".json"));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterKeyMappingsEvent event) ->
                Hotkeys.ALL.forEach(mapping -> {
                    // GUI context: only active while a screen is open, so it never clashes with in-world bindings.
                    mapping.setKeyConflictContext(KeyConflictContext.GUI);
                    event.register(mapping);
                }));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) ->
                ScreenButtons.onInit(event.getScreen(), event::addListener, event::removeListener, ACCESS));
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event) ->
                ScreenButtons.onRender(event.getScreen(), ACCESS));
        // LOW priority: overlays such as JEI handle (and cancel) keys for their own focused search fields first.
        // isActiveAndMatches honours Forge key modifiers (Ctrl+M must not fire on plain M) and conflict contexts.
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, ScreenEvent.KeyPressed.Pre.class, event -> {
            InputConstants.Key key = InputConstants.getKey(event.getKeyCode(), event.getScanCode());
            if (Hotkeys.onKeyPressed(event.getScreen(), mapping -> mapping.isActiveAndMatches(key), ACCESS)) {
                event.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, false, ScreenEvent.MouseButtonPressed.Pre.class, event -> {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
            if (Hotkeys.onMouseClicked(event.getScreen(), event.getButton(), mapping -> mapping.isActiveAndMatches(key), ACCESS)) {
                event.setCanceled(true);
            }
        });
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
    }
}
