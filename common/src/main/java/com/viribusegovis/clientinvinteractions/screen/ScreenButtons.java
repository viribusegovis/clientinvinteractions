package com.viribusegovis.clientinvinteractions.screen;

import com.viribusegovis.clientinvinteractions.config.ClientConfig;
import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import com.viribusegovis.clientinvinteractions.inventory.SophisticatedSort;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Adds the interaction buttons to container screens and keeps them aligned with the slots. Positions and slot
 * regions are re-read every frame and on every press, because some screens (Sophisticated storage when upgrades
 * change) move or add slots without re-running init.
 */
public final class ScreenButtons {
    private static final int SLOT_SIZE = 16;
    private static final int GAP = 1;
    /** Space between the button row's bottom edge and the top of the slots below it. */
    private static final int ROW_LIFT = 3;
    private static final String LANG = "clientinvinteractions.button.";

    private enum Side {
        CONTAINER,
        PLAYER
    }

    /** {@code index} counts from the right end of its side's row. */
    private record Placed(IconButton button, Side side, int index) {
    }

    // Only one screen is open at a time; holding just that one (replaced on the next init) keeps closed screens
    // collectable, which a map from screen to buttons wouldn't, since the buttons' actions reference their screen.
    private static Screen currentScreen;
    private static List<Placed> currentButtons = List.of();

    private ScreenButtons() {
    }

    /** Call after a screen's init() (also re-run on resize). */
    public static void onInit(Screen screen, Consumer<GuiEventListener> addWidget, Consumer<GuiEventListener> removeWidget,
                              ScreenAccess access) {
        currentScreen = null;
        currentButtons = List.of();
        ClientConfig config = ConfigStore.current();
        DebugLog.log("screen init screen={}", screen.getClass().getName());
        Target.of(screen, config).ifPresent(target -> {
            if (Target.isSophisticated(screen)) {
                SophisticatedButtons.hide(screen, removeWidget);
            }
            List<Placed> placed = buttons(target, config);
            placed.forEach(p -> addWidget.accept(p.button()));
            currentScreen = screen;
            currentButtons = placed;
            layout(target, access);
        });
    }

    /** Call before a screen renders. Follows GUI shifts after init, e.g. the recipe book or Sophisticated upgrades. */
    public static void onRender(Screen screen, ScreenAccess access) {
        if (screen == currentScreen) {
            Target.of(screen, ConfigStore.current()).ifPresent(target -> layout(target, access));
        }
    }

    private static void layout(Target target, ScreenAccess access) {
        for (Placed p : currentButtons) {
            List<Slot> region = p.side() == Side.CONTAINER ? target.container() : target.playerMain();
            p.button().visible = !region.isEmpty();
            if (region.isEmpty()) {
                continue;
            }
            // Right-aligned just above the region's top slot row, on the vanilla title/label line.
            int right = region.stream().mapToInt(slot -> slot.x).max().orElseThrow() + SLOT_SIZE;
            int top = region.stream().mapToInt(slot -> slot.y).min().orElseThrow();
            p.button().setX(access.left(target.screen()) + right - (p.index() + 1) * IconButton.SIZE - p.index() * GAP);
            p.button().setY(access.top(target.screen()) + top - IconButton.SIZE - ROW_LIFT);
        }
    }

    private static List<Placed> buttons(Target target, ClientConfig config) {
        Screen screen = target.screen();
        boolean sophisticatedMenu = SophisticatedSort.isSophisticatedMenu(target.menu());
        List<Placed> placed = new ArrayList<>();
        if (target.hasContainer()) {
            // Listed right to left.
            List<IconButton> row = new ArrayList<>();
            if (config.sortContainerButton()) {
                String detail = sophisticatedMenu ? "sort.sophisticated" : "sort";
                row.add(new IconButton(Icon.SORT, title("sort_container"),
                        () -> tooltip("sort_container", List.of(sortOrderLine(), detail(detail), detail("sort.right_click"))),
                        action(screen, "sort_container", Actions::sortContainer),
                        action(screen, "cycle_sort_order", t -> Actions.cycleSortOrder(t.player()))));
            }
            if (config.takeAllButton()) {
                row.add(new IconButton(Icon.TAKE_ALL_DOWN, title("take_all"),
                        () -> tooltip("take_all", List.of(detail("take_all"))),
                        action(screen, "take_all", Actions::takeAll)));
            }
            placed.addAll(side(row, Side.CONTAINER));
        }
        if (target.hasPlayer()) {
            List<IconButton> row = new ArrayList<>();
            if (config.sortPlayerButton()) {
                row.add(new IconButton(Icon.SORT, title("sort_player"),
                        () -> tooltip("sort_player", List.of(sortOrderLine(), detail("sort.right_click"))),
                        action(screen, "sort_player", Actions::sortPlayer),
                        action(screen, "cycle_sort_order", t -> Actions.cycleSortOrder(t.player()))));
            }
            if (target.hasContainer() && config.moveAllButton()) {
                row.add(new IconButton(Icon.MOVE_ALL_UP, title("move_all"),
                        () -> tooltip("move_all", List.of(detail("move_all"), hotbarLine())),
                        action(screen, "move_all", Actions::moveAll)));
            }
            if (target.hasContainer() && config.mergeButton()) {
                row.add(new IconButton(Icon.MERGE_UP, title("merge"),
                        () -> tooltip("merge", List.of(detail("merge"), hotbarLine())),
                        action(screen, "merge", Actions::merge)));
            }
            placed.addAll(side(row, Side.PLAYER));
        }
        DebugLog.log("buttons containerSlots={} playerMainSlots={} sophisticatedMenu={} buttons={}",
                target.container().size(), target.playerMain().size(), sophisticatedMenu,
                placed.stream().map(p -> p.button().getMessage().getString()).toList());
        return placed;
    }

    private static List<Placed> side(List<IconButton> row, Side side) {
        List<Placed> placed = new ArrayList<>();
        for (int i = 0; i < row.size(); i++) {
            placed.add(new Placed(row.get(i), side, i));
        }
        return placed;
    }

    /** Resolves the screen's slots at press time, so actions see the current layout. */
    private static Runnable action(Screen screen, String name, Consumer<Target> run) {
        return () -> {
            DebugLog.log("button pressed name={}", name);
            Target.of(screen, ConfigStore.current()).ifPresent(run);
        };
    }

    private static Component title(String name) {
        return Component.translatable(LANG + name);
    }

    private static Component detail(String name) {
        return Component.translatable(LANG + name + ".detail");
    }

    private static Component sortOrderLine() {
        return Component.translatable(LANG + "sort.order", ConfigStore.current().sortOrder().label());
    }

    private static Component hotbarLine() {
        return Component.translatable(LANG + (ConfigStore.current().includeHotbar() ? "hotbar_included" : "hotbar_excluded"));
    }

    private static Component tooltip(String name, List<Component> details) {
        Component tooltip = title(name);
        for (Component line : details) {
            tooltip = tooltip.copy().append("\n").append(line.copy().withStyle(ChatFormatting.GRAY));
        }
        return tooltip;
    }
}
