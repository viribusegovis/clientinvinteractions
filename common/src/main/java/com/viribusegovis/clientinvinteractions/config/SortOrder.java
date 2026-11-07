package com.viribusegovis.clientinvinteractions.config;

import net.minecraft.network.chat.Component;

public enum SortOrder {
    NAME("name"),
    MOD("mod"),
    ID("id"),
    COUNT("count");

    private final String key;

    SortOrder(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public Component label() {
        return Component.translatable("clientinvinteractions.sort_order." + key);
    }

    public static SortOrder fromKey(String key) {
        for (SortOrder order : values()) {
            if (order.key.equals(key)) {
                return order;
            }
        }
        throw new IllegalArgumentException("Unknown sort order '" + key + "'");
    }
}
