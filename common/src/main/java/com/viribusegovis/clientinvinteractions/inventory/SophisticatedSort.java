package com.viribusegovis.clientinvinteractions.inventory;

import com.viribusegovis.clientinvinteractions.config.SortOrder;
import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Sophisticated Storage/Backpacks menus sort server-side with their own rules (stack upgrades, no-sort and memory
 * slots). Called on the client, their {@code setSortBy(SortBy)} and {@code sort()} only send those requests to the
 * server, the same as their own sort-by and sort buttons. Reflection keeps Sophisticated Core an optional
 * dependency; these are the mod's own method names, so Minecraft's obfuscation doesn't affect them.
 */
public final class SophisticatedSort {
    private static final String MENU_BASE = "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase";

    private SophisticatedSort() {
    }

    public static boolean isSophisticatedMenu(AbstractContainerMenu menu) {
        for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(MENU_BASE)) {
                return true;
            }
        }
        return false;
    }

    /** Sophisticated has no item-ID order; ID falls back to its name order. */
    private static String sortByName(SortOrder order) {
        return switch (order) {
            case NAME, ID -> "NAME";
            case MOD -> "MOD";
            case COUNT -> "COUNT";
        };
    }

    public static void requestSort(AbstractContainerMenu menu, SortOrder order) {
        DebugLog.log("sophisticated sort requested menu={} order={} sortBy={}",
                menu.getClass().getName(), order, sortByName(order));
        try {
            Method getSortBy = menu.getClass().getMethod("getSortBy");
            Class<?> sortByType = getSortBy.getReturnType();
            Object sortBy = enumConstant(sortByType, sortByName(order));
            menu.getClass().getMethod("setSortBy", sortByType).invoke(menu, sortBy);
            menu.getClass().getMethod("sort").invoke(menu);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException("Sophisticated Core changed: sort API missing on " + menu.getClass().getName(), e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Sophisticated Core sort failed on " + menu.getClass().getName(), e.getCause());
        }
    }

    private static Object enumConstant(Class<?> enumType, String name) {
        for (Object constant : enumType.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(name)) {
                return constant;
            }
        }
        throw new IllegalStateException("Sophisticated Core changed: " + enumType.getName() + " has no " + name);
    }
}
