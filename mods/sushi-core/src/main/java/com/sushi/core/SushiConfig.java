package com.sushi.core;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** HUD module toggles, stored in config/sushi-core.properties. */
public final class SushiConfig {
    public static boolean fps = true;
    public static boolean cps = true;
    public static boolean keystrokes = true;
    public static boolean ping = true;
    public static boolean coords = false;

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sushi-core.properties");

    private SushiConfig() {}

    public static void load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                p.load(in);
            } catch (IOException ignored) {
                // Fall back to defaults if the file is unreadable.
            }
        }
        fps = Boolean.parseBoolean(p.getProperty("fps", "true"));
        cps = Boolean.parseBoolean(p.getProperty("cps", "true"));
        keystrokes = Boolean.parseBoolean(p.getProperty("keystrokes", "true"));
        ping = Boolean.parseBoolean(p.getProperty("ping", "true"));
        coords = Boolean.parseBoolean(p.getProperty("coords", "false"));
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("fps", String.valueOf(fps));
        p.setProperty("cps", String.valueOf(cps));
        p.setProperty("keystrokes", String.valueOf(keystrokes));
        p.setProperty("ping", String.valueOf(ping));
        p.setProperty("coords", String.valueOf(coords));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Sushi Core settings");
            }
        } catch (IOException ignored) {
            // Settings simply won't persist if the config folder is not writable.
        }
    }
}
