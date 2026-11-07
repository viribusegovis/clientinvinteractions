package com.viribusegovis.clientinvinteractions.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla keeps the GUI origin and hovered slot protected; NeoForge adds public getters, Fabric doesn't. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int clientinvinteractions$leftPos();

    @Accessor("topPos")
    int clientinvinteractions$topPos();

    @Accessor("hoveredSlot")
    @Nullable
    Slot clientinvinteractions$hoveredSlot();
}
