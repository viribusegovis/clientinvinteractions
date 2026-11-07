package com.viribusegovis.clientinvinteractions.config;

public record ClientConfig(
        boolean mergeButton,
        boolean moveAllButton,
        boolean takeAllButton,
        boolean sortContainerButton,
        boolean sortPlayerButton,
        boolean includeHotbar,
        boolean middleClickSort,
        boolean showOnSophisticatedScreens,
        SortOrder sortOrder,
        boolean debugLogging) {

    public static final ClientConfig DEFAULT =
            new ClientConfig(true, true, true, true, true, false, true, false, SortOrder.NAME, false);

    public ClientConfig withSortOrder(SortOrder order) {
        return new ClientConfig(mergeButton, moveAllButton, takeAllButton, sortContainerButton, sortPlayerButton,
                includeHotbar, middleClickSort, showOnSophisticatedScreens, order, debugLogging);
    }
}
