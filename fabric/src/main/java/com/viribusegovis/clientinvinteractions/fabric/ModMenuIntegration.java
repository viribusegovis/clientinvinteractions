package com.viribusegovis.clientinvinteractions.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.viribusegovis.clientinvinteractions.config.ConfigScreen;

/** Loaded by Mod Menu through the "modmenu" entrypoint, so the mod runs fine without Mod Menu installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
