package com.viribusegovis.clientinvinteractions.screen;

import com.mojang.blaze3d.platform.InputConstants;
import com.viribusegovis.clientinvinteractions.config.ClientConfig;
import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Key bindings that work inside container screens, plus middle-click sort. All bindings start unbound: a vanilla
 * key binding can't share a key (middle-click would take over Pick Block on Fabric), so middle-click sort is a
 * config toggle instead of a default binding. The loader supplies how a binding matches the pressed input, since
 * Forge/NeoForge add key modifiers and conflict contexts that plain matching ignores.
 */
public final class Hotkeys {
    private static final String CATEGORY = "key.categories.clientinvinteractions";

    public static final KeyMapping SORT = unbound("key.clientinvinteractions.sort");
    public static final KeyMapping CYCLE_SORT_ORDER = unbound("key.clientinvinteractions.cycle_sort_order");
    public static final KeyMapping MERGE = unbound("key.clientinvinteractions.merge");
    public static final KeyMapping MOVE_ALL = unbound("key.clientinvinteractions.move_all");
    public static final KeyMapping TAKE_ALL = unbound("key.clientinvinteractions.take_all");

    public static final List<KeyMapping> ALL = List.of(SORT, CYCLE_SORT_ORDER, MERGE, MOVE_ALL, TAKE_ALL);

    private Hotkeys() {
    }

    private static KeyMapping unbound(String name) {
        return new KeyMapping(name, InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY);
    }

    /** Returns true if a binding matched and ran, so the loader can consume the key press. */
    public static boolean onKeyPressed(Screen screen, Predicate<KeyMapping> pressed, ScreenAccess access) {
        if (isTyping(screen)) {
            return false;
        }
        return Target.of(screen, ConfigStore.current())
                .map(target -> runBinding(target, pressed, access))
                .orElse(false);
    }

    /** Returns true if middle-click sort or a mouse binding ran, so the loader can consume the click. */
    public static boolean onMouseClicked(Screen screen, int button, Predicate<KeyMapping> pressed, ScreenAccess access) {
        ClientConfig config = ConfigStore.current();
        Optional<Target> found = Target.of(screen, config);
        if (found.isEmpty()) {
            return false;
        }
        Target target = found.get();
        if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && config.middleClickSort()) {
            Optional<Slot> hovered = access.hoveredSlot(target.screen());
            // Only over a slot, so clicks elsewhere (e.g. on item-list overlays) pass through; and never on a filled
            // slot in creative, where middle-click clones the stack.
            boolean cloning = target.player().getAbilities().instabuild && hovered.map(Slot::hasItem).orElse(false);
            if (hovered.isPresent() && !cloning && sortSide(target, hovered.get())) {
                DebugLog.log("middle-click sort slot={}", hovered.get().index);
                return true;
            }
        }
        return runBinding(target, pressed, access);
    }

    /** True while a text field has focus, so typed letters don't trigger bindings. */
    private static boolean isTyping(Screen screen) {
        GuiEventListener focused = screen.getFocused();
        while (focused instanceof ContainerEventHandler parent && parent.getFocused() != null) {
            focused = parent.getFocused();
        }
        if (focused == null) {
            return false;
        }
        String name = focused.getClass().getSimpleName();
        return focused instanceof EditBox
                // The recipe book takes focus as a whole when open; its search box isn't reachable from here.
                || (focused instanceof RecipeBookComponent book && book.isVisible())
                // Sophisticated's search box wraps an EditBox without being one.
                || name.endsWith("TextBox") || name.endsWith("SearchBox");
    }

    private static boolean runBinding(Target target, Predicate<KeyMapping> pressed, ScreenAccess access) {
        if (pressed.test(SORT)) {
            DebugLog.log("hotkey sort");
            return sortUnderMouse(target, access);
        }
        if (pressed.test(CYCLE_SORT_ORDER)) {
            DebugLog.log("hotkey cycle_sort_order");
            Actions.cycleSortOrder(target.player());
            return true;
        }
        if (target.hasContainer() && pressed.test(TAKE_ALL)) {
            DebugLog.log("hotkey take_all");
            Actions.takeAll(target);
            return true;
        }
        if (target.hasContainer() && target.hasPlayer() && pressed.test(MOVE_ALL)) {
            DebugLog.log("hotkey move_all");
            Actions.moveAll(target);
            return true;
        }
        if (target.hasContainer() && target.hasPlayer() && pressed.test(MERGE)) {
            DebugLog.log("hotkey merge");
            Actions.merge(target);
            return true;
        }
        return false;
    }

    /** With no slot under the mouse, sorts the container (or the inventory if there is none). */
    private static boolean sortUnderMouse(Target target, ScreenAccess access) {
        Optional<Slot> hovered = access.hoveredSlot(target.screen());
        if (hovered.isPresent()) {
            return sortSide(target, hovered.get());
        }
        if (target.hasContainer()) {
            Actions.sortContainer(target);
            return true;
        }
        if (target.hasPlayer()) {
            Actions.sortPlayer(target);
            return true;
        }
        return false;
    }

    /** Sorts the side the slot belongs to; false if that side isn't sortable here. */
    private static boolean sortSide(Target target, Slot slot) {
        if (target.isPlayerSlot(slot) && target.hasPlayer()) {
            Actions.sortPlayer(target);
            return true;
        }
        if (!target.isPlayerSlot(slot) && target.hasContainer() && target.container().contains(slot)) {
            Actions.sortContainer(target);
            return true;
        }
        return false;
    }
}
