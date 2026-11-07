package com.viribusegovis.clientinvinteractions.debug;

import com.viribusegovis.clientinvinteractions.config.ConfigStore;
import net.minecraft.network.Connection;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debug logging behind the config toggle. Mod events go to latest.log at INFO; turning the toggle on also raises
 * the vanilla network connection logger to DEBUG so the full exception behind "Connection Lost / Internal
 * Exception" disconnects (normally dropped) is logged, cause chain included: to debug.log on Forge/NeoForge, and to
 * latest.log on Fabric (whose log config has no debug.log).
 */
public final class DebugLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("clientinvinteractions");
    // Vanilla loggers are named after their class; resolved from the class so it matches Fabric's runtime names.
    private static final String CONNECTION_LOGGER = Connection.class.getName();

    // The level the pack configured before we touched it, so turning the toggle off restores it exactly.
    private static Level originalConnectionLevel;

    private DebugLog() {
    }

    public static boolean isEnabled() {
        return ConfigStore.current().debugLogging();
    }

    /** Logs "[debug] message" with SLF4J {} placeholders when the toggle is on. */
    public static void log(String format, Object... args) {
        if (isEnabled()) {
            LOGGER.info("[debug] " + format, args);
        }
    }

    public static void applyNetworkLogging(boolean enabled) {
        if (originalConnectionLevel == null) {
            originalConnectionLevel = LogManager.getLogger(CONNECTION_LOGGER).getLevel();
        }
        Level level = enabled ? Level.DEBUG : originalConnectionLevel;
        Configurator.setLevel(CONNECTION_LOGGER, level);
        LOGGER.info("Debug logging {} (network connection logger level={})", enabled ? "on" : "off", level);
    }
}
