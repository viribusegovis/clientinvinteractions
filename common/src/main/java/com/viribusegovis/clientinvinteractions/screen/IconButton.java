package com.viribusegovis.clientinvinteractions.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

import java.util.function.Supplier;

/** Small bevelled button in the container-GUI palette with a pixel glyph. */
final class IconButton extends Button {
    static final int SIZE = 11;

    private static final int RIGHT_BUTTON = 1;
    private static final int BORDER = 0xFF373737;
    private static final int FACE = 0xFFC6C6C6;
    private static final int FACE_HOVERED = 0xFFE2E2E2;
    private static final int HIGHLIGHT = 0xFFFFFFFF;
    private static final int SHADOW = 0xFF8B8B8B;
    private static final int GLYPH = 0xFF3F3F3F;
    private static final int GLYPH_OFFSET = 2;

    private final Icon icon;
    private final Supplier<Component> tooltip;
    private final Runnable rightClick;
    private Component shownTooltip;

    /** Left-click only. */
    IconButton(Icon icon, Component title, Supplier<Component> tooltip, Runnable leftClick) {
        this(icon, title, tooltip, leftClick, () -> {
        });
    }

    IconButton(Icon icon, Component title, Supplier<Component> tooltip, Runnable leftClick, Runnable rightClick) {
        super(0, 0, SIZE, SIZE, title, b -> leftClick.run(), DEFAULT_NARRATION);
        this.icon = icon;
        this.tooltip = tooltip;
        this.rightClick = rightClick;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == RIGHT_BUTTON && active && visible && isMouseOver(event.x(), event.y())) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            rightClick.run();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** The button keeps focus after a click; without this, Space/Enter would repeat the action. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        return false;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        // Tooltips depend on config (sort order, hotbar) that can change while the screen is open.
        Component current = tooltip.get();
        if (!current.equals(shownTooltip)) {
            shownTooltip = current;
            setTooltip(Tooltip.create(current));
        }
        int x = getX();
        int y = getY();
        int right = x + SIZE;
        int bottom = y + SIZE;
        graphics.fill(x, y, right, bottom, BORDER);
        // Hover only (not focus): the button keeps focus after a click and would otherwise stay lit.
        graphics.fill(x + 1, y + 1, right - 1, bottom - 1, isHovered() ? FACE_HOVERED : FACE);
        graphics.fill(x + 1, y + 1, right - 1, y + 2, HIGHLIGHT);
        graphics.fill(x + 1, y + 1, x + 2, bottom - 1, HIGHLIGHT);
        graphics.fill(x + 1, bottom - 2, right - 1, bottom - 1, SHADOW);
        graphics.fill(right - 2, y + 1, right - 1, bottom - 1, SHADOW);
        for (int py = 0; py < Icon.SIZE; py++) {
            for (int px = 0; px < Icon.SIZE; px++) {
                if (icon.isLit(px, py)) {
                    int gx = x + GLYPH_OFFSET + px;
                    int gy = y + GLYPH_OFFSET + py;
                    graphics.fill(gx, gy, gx + 1, gy + 1, GLYPH);
                }
            }
        }
    }
}
