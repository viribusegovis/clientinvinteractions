package com.viribusegovis.clientinvinteractions.config;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Scrollable options list grouped into sections. Each row shows a label and its control; the hovered row's
 * description is shown in a panel above the bottom buttons.
 */
public final class ConfigScreen extends Screen {
    private static final String LANG = "clientinvinteractions.config.";
    private static final int ROW_WIDTH = 310;
    private static final int ROW_HEIGHT = 24;
    private static final int CONTROL_WIDTH = 90;
    private static final int CONTROL_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 150;
    private static final int LIST_TOP = 32;
    private static final int FOOTER_HEIGHT = 84;
    private static final int DESCRIPTION_LINES = 2;
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int HEADER_COLOR = 0xFFE080;
    private static final int DESCRIPTION_COLOR = 0xA0A0A0;
    private static final Component ON = CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN);
    private static final Component OFF = CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED);

    private final Screen parent;
    /** Values the widgets are built from; carries unsaved edits across resize and holds defaults on reset. */
    private ClientConfig draft;
    private Component hoveredDescription;
    private OptionsList list;

    // Assigned in init(), which the game re-runs on resize.
    private CycleButton<Boolean> merge;
    private CycleButton<Boolean> moveAll;
    private CycleButton<Boolean> takeAll;
    private CycleButton<Boolean> sortContainer;
    private CycleButton<Boolean> sortPlayer;
    private CycleButton<Boolean> includeHotbar;
    private CycleButton<Boolean> middleClickSort;
    private CycleButton<SortOrder> sortOrder;
    private CycleButton<Boolean> replaceSophisticated;
    private CycleButton<Boolean> debugLogging;

    public ConfigScreen(Screen parent) {
        super(Component.translatable(LANG + "title"));
        this.parent = parent;
        this.draft = ConfigStore.current();
    }

    @Override
    protected void init() {
        if (merge != null) {
            draft = edited();
        }
        list = new OptionsList();
        list.header("section.buttons");
        merge = list.toggle("merge_button", draft.mergeButton());
        moveAll = list.toggle("move_all_button", draft.moveAllButton());
        takeAll = list.toggle("take_all_button", draft.takeAllButton());
        sortContainer = list.toggle("sort_container_button", draft.sortContainerButton());
        sortPlayer = list.toggle("sort_player_button", draft.sortPlayerButton());
        list.header("section.behavior");
        includeHotbar = list.toggle("include_hotbar", draft.includeHotbar());
        middleClickSort = list.toggle("middle_click_sort", draft.middleClickSort());
        sortOrder = list.option("sort_order", CycleButton.builder(SortOrder::label)
                .withValues(SortOrder.values())
                .withInitialValue(draft.sortOrder())
                .displayOnlyValue()
                .create(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT, Component.translatable(LANG + "sort_order")));
        list.header("section.sophisticated");
        replaceSophisticated = list.toggle("show_on_sophisticated", draft.showOnSophisticatedScreens());
        list.header("section.debug");
        debugLogging = list.toggle("debug_logging", draft.debugLogging());
        addWidget(list);

        int left = width / 2 - BUTTON_WIDTH - 5;
        int right = width / 2 + 5;
        addRenderableWidget(Button.builder(Component.translatable(LANG + "reset"), b -> resetToDefaults())
                .bounds(left, height - 52, BUTTON_WIDTH, CONTROL_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable(LANG + "controls"),
                        b -> minecraft.setScreen(new KeyBindsScreen(this, minecraft.options)))
                .bounds(right, height - 52, BUTTON_WIDTH, CONTROL_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> onClose())
                .bounds(left, height - 28, BUTTON_WIDTH, CONTROL_HEIGHT).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> saveAndClose())
                .bounds(right, height - 28, BUTTON_WIDTH, CONTROL_HEIGHT).build());
    }

    private void resetToDefaults() {
        draft = ClientConfig.DEFAULT;
        // Null marks the widgets stale so init() builds from the draft instead of reading them back.
        merge = null;
        rebuildWidgets();
    }

    private void saveAndClose() {
        ConfigStore.update(edited());
        onClose();
    }

    private ClientConfig edited() {
        return new ClientConfig(
                merge.getValue(),
                moveAll.getValue(),
                takeAll.getValue(),
                sortContainer.getValue(),
                sortPlayer.getValue(),
                includeHotbar.getValue(),
                middleClickSort.getValue(),
                replaceSophisticated.getValue(),
                sortOrder.getValue(),
                debugLogging.getValue());
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        hoveredDescription = null;
        // The list sets hoveredDescription while rendering the row under the mouse.
        list.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, TEXT_COLOR);
        Component description = hoveredDescription != null
                ? hoveredDescription
                : Component.translatable(LANG + "hover_hint");
        List<FormattedCharSequence> lines = font.split(description, ROW_WIDTH);
        int descriptionTop = height - FOOTER_HEIGHT + 6;
        for (int i = 0; i < Math.min(DESCRIPTION_LINES, lines.size()); i++) {
            graphics.drawString(font, lines.get(i), width / 2 - ROW_WIDTH / 2, descriptionTop + i * 10, DESCRIPTION_COLOR);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private final class OptionsList extends ContainerObjectSelectionList<Row> {
        OptionsList() {
            super(ConfigScreen.this.minecraft, ConfigScreen.this.width, ConfigScreen.this.height,
                    LIST_TOP, ConfigScreen.this.height - FOOTER_HEIGHT, ROW_HEIGHT);
        }

        void header(String key) {
            addEntry(new HeaderRow(Component.translatable(LANG + key).withStyle(ChatFormatting.BOLD)));
        }

        CycleButton<Boolean> toggle(String key, boolean value) {
            return option(key, CycleButton.booleanBuilder(ON, OFF)
                    .withInitialValue(value)
                    .displayOnlyValue()
                    .create(0, 0, CONTROL_WIDTH, CONTROL_HEIGHT, Component.translatable(LANG + key)));
        }

        <T extends AbstractWidget> T option(String key, T control) {
            addEntry(new OptionRow(Component.translatable(LANG + key), Component.translatable(LANG + key + ".tooltip"), control));
            return control;
        }

        @Override
        public int getRowWidth() {
            return ROW_WIDTH;
        }

        @Override
        protected int getScrollbarPosition() {
            return width / 2 + ROW_WIDTH / 2 + 6;
        }
    }

    private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
    }

    private final class HeaderRow extends Row {
        private final Component text;

        HeaderRow(Component text) {
            this.text = text;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawCenteredString(font, text, left + width / 2, top + (height - font.lineHeight) / 2 + 2, HEADER_COLOR);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private final class OptionRow extends Row {
        private final Component label;
        private final Component description;
        private final AbstractWidget control;

        OptionRow(Component label, Component description, AbstractWidget control) {
            this.label = label;
            this.description = description;
            this.control = control;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                hoveredDescription = description;
            }
            graphics.drawString(font, label, left + 4, top + (height - font.lineHeight) / 2, TEXT_COLOR);
            control.setX(left + width - CONTROL_WIDTH);
            control.setY(top + (height - CONTROL_HEIGHT) / 2);
            control.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(control);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(control);
        }
    }
}
