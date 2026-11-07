package com.viribusegovis.clientinvinteractions.screen;

import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hides Sophisticated's own sort, sort-by and transfer buttons when this mod's buttons replace them. The widgets are
 * made invisible (Sophisticated's widgets skip drawing, hover and clicks when invisible) and removed from the
 * screen's input list, but the screen's fields keep pointing at them, so Sophisticated's own code that reads those
 * fields keeps working.
 */
final class SophisticatedButtons {
    private static final String SCREEN_BASE = "net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase";
    private static final List<String> BUTTON_FIELDS =
            List.of("sortButton", "sortByButton", "transferToStorageButton", "transferToInventoryButton");

    private SophisticatedButtons() {
    }

    static void hide(Screen screen, Consumer<GuiEventListener> removeWidget) {
        Class<?> base = screenBase(screen.getClass());
        if (base == null) {
            return;
        }
        for (String name : BUTTON_FIELDS) {
            Object widget = read(base, name, screen);
            // Null when Sophisticated's own config hides that button.
            if (widget != null) {
                setInvisible(widget);
                removeWidget.accept((GuiEventListener) widget);
            }
        }
        DebugLog.log("sophisticated buttons hidden screen={}", screen.getClass().getName());
    }

    private static Class<?> screenBase(Class<?> type) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (current.getName().equals(SCREEN_BASE)) {
                return current;
            }
        }
        return null;
    }

    private static Object read(Class<?> base, String name, Screen screen) {
        try {
            Field field = base.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(screen);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Sophisticated Core changed: can't read " + SCREEN_BASE + "." + name, e);
        }
    }

    private static void setInvisible(Object widget) {
        try {
            widget.getClass().getMethod("setVisible", boolean.class).invoke(widget, false);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Sophisticated Core changed: no setVisible on " + widget.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("setVisible failed on " + widget.getClass().getName(), e.getCause());
        }
    }
}
