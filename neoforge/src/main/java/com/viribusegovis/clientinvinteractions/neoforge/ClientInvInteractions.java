package com.viribusegovis.clientinvinteractions.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.viribusegovis.clientinvinteractions.config.ConfigScreen;
import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.screen.Hotkeys;
import com.viribusegovis.clientinvinteractions.screen.ScreenAccess;
import com.viribusegovis.clientinvinteractions.screen.ScreenButtons;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Optional;

// Client-only: never loaded on a dedicated server, and servers don't need it.
@Mod(value = ClientInvInteractions.MOD_ID, dist = Dist.CLIENT)
public final class ClientInvInteractions {
    public static final String MOD_ID = "clientinvinteractions";

    // getLeftPos/getTopPos/getHoveredSlot are NeoForge additions to AbstractContainerScreen.
    private static final ScreenAccess ACCESS = new ScreenAccess() {
        @Override
        public int left(AbstractContainerScreen<?> screen) {
            return screen.getLeftPos();
        }

        @Override
        public int top(AbstractContainerScreen<?> screen) {
            return screen.getTopPos();
        }

        @Override
        public Optional<Slot> hoveredSlot(AbstractContainerScreen<?> screen) {
            return Optional.ofNullable(screen.getHoveredSlot());
        }
    };

    public ClientInvInteractions(IEventBus modBus, ModContainer container) {
        ConfigStore.init(FMLPaths.CONFIGDIR.get().resolve(MOD_ID + ".json"));
        modBus.addListener((RegisterKeyMappingsEvent event) -> {
            event.registerCategory(Hotkeys.CATEGORY);
            Hotkeys.ALL.forEach(mapping -> {
                // GUI context: only active while a screen is open, so it never clashes with in-world bindings.
                mapping.setKeyConflictContext(KeyConflictContext.GUI);
                event.register(mapping);
            });
        });
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Init.Post event) ->
                ScreenButtons.onInit(event.getScreen(), event::addListener, event::removeListener, ACCESS));
        NeoForge.EVENT_BUS.addListener((ScreenEvent.Render.Pre event) ->
                ScreenButtons.onRender(event.getScreen(), ACCESS));
        // LOW priority: overlays such as JEI handle (and cancel) keys for their own focused search fields first.
        // isActiveAndMatches honours NeoForge key modifiers (Ctrl+M must not fire on plain M) and conflict contexts.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, ScreenEvent.KeyPressed.Pre.class, event -> {
            InputConstants.Key key = InputConstants.getKey(event.getKeyEvent());
            if (Hotkeys.onKeyPressed(event.getScreen(), mapping -> mapping.isActiveAndMatches(key), ACCESS)) {
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, false, ScreenEvent.MouseButtonPressed.Pre.class, event -> {
            InputConstants.Key key = InputConstants.Type.MOUSE.getOrCreate(event.getButton());
            if (Hotkeys.onMouseClicked(event.getScreen(), event.getButton(), mapping -> mapping.isActiveAndMatches(key), ACCESS)) {
                event.setCanceled(true);
            }
        });
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (modContainer, parent) -> new ConfigScreen(parent));
    }
}
