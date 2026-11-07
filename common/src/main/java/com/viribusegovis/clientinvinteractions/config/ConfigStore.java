package com.viribusegovis.clientinvinteractions.config;

import com.google.gson.Gson;
import com.viribusegovis.clientinvinteractions.debug.DebugLog;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads and saves {@link ClientConfig} as JSON and holds the active instance. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path;
    private static volatile ClientConfig current = ClientConfig.DEFAULT;

    private ConfigStore() {
    }

    public static void init(Path configPath) {
        path = configPath;
        if (Files.exists(configPath)) {
            current = read(configPath);
        }
        // Rewrite so keys added in newer versions show up in the file.
        write(configPath, current);
        DebugLog.applyNetworkLogging(current.debugLogging());
    }

    public static ClientConfig current() {
        return current;
    }

    public static void update(ClientConfig config) {
        ClientConfig previous = current;
        write(path, config);
        current = config;
        // Only on change: re-applying resets log4j loggers and logs a line, too noisy for every sort-order cycle.
        if (config.debugLogging() != previous.debugLogging()) {
            DebugLog.applyNetworkLogging(config.debugLogging());
        }
    }

    private static ClientConfig read(Path configPath) {
        try {
            JsonObject json = JsonParser.parseString(Files.readString(configPath, StandardCharsets.UTF_8)).getAsJsonObject();
            ClientConfig d = ClientConfig.DEFAULT;
            return new ClientConfig(
                    bool(json, "mergeButton", d.mergeButton()),
                    bool(json, "moveAllButton", d.moveAllButton()),
                    bool(json, "takeAllButton", d.takeAllButton()),
                    bool(json, "sortContainerButton", d.sortContainerButton()),
                    bool(json, "sortPlayerButton", d.sortPlayerButton()),
                    bool(json, "includeHotbar", d.includeHotbar()),
                    bool(json, "middleClickSort", d.middleClickSort()),
                    bool(json, "showOnSophisticatedScreens", d.showOnSophisticatedScreens()),
                    json.has("sortOrder") ? SortOrder.fromKey(json.get("sortOrder").getAsString()) : d.sortOrder(),
                    bool(json, "debugLogging", d.debugLogging()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read config " + configPath, e);
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid config " + configPath + ": " + e.getMessage()
                    + " (fix or delete the file to regenerate it)", e);
        }
    }

    /** Missing keys fall back to defaults so older config files keep working. */
    private static boolean bool(JsonObject json, String key, boolean defaultValue) {
        JsonElement element = json.get(key);
        return element == null ? defaultValue : element.getAsBoolean();
    }

    private static void write(Path configPath, ClientConfig config) {
        JsonObject json = new JsonObject();
        json.addProperty("mergeButton", config.mergeButton());
        json.addProperty("moveAllButton", config.moveAllButton());
        json.addProperty("takeAllButton", config.takeAllButton());
        json.addProperty("sortContainerButton", config.sortContainerButton());
        json.addProperty("sortPlayerButton", config.sortPlayerButton());
        json.addProperty("includeHotbar", config.includeHotbar());
        json.addProperty("middleClickSort", config.middleClickSort());
        json.addProperty("showOnSophisticatedScreens", config.showOnSophisticatedScreens());
        json.addProperty("sortOrder", config.sortOrder().key());
        json.addProperty("debugLogging", config.debugLogging());
        try {
            Files.writeString(configPath, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write config " + configPath, e);
        }
    }
}
