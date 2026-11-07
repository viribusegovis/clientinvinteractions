package com.viribusegovis.clientinvinteractions.screen;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

import java.util.Optional;

/** The loader's way to read protected container-screen state. */
public interface ScreenAccess {
    int left(AbstractContainerScreen<?> screen);

    int top(AbstractContainerScreen<?> screen);

    Optional<Slot> hoveredSlot(AbstractContainerScreen<?> screen);
}
