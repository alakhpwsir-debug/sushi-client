package com.sushi.core;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Module and HUD layout settings, stored in config/sushi-core.properties. */
public final class SushiConfig {
    public static final String[] KEYS = {"fps", "cps", "keystrokes", "ping", "coords"};
    public static final String[] NAMES = {"FPS", "CPS", "Keystrokes", "Ping", "Coordinates"};
    public static final String[] DESCS = {"Frames per second", "Clicks per second", "Keys as you press them", "Connection latency", "Your block position"};
    public static final int[] SCALES = {50, 75, 100, 125, 150, 200};

    public static final boolean[] enabled = {true, true, true, true, false};
    /** Screen position in GUI pixels. -1 means "use the default spot". */
    public static final int[] x = {-1, -1, -1, -1, -1};
    public static final int[] y = {-1, -1, -1, -1, -1};
    public static final int[] scale = {100, 100, 100, 100, 100};
    public static final boolean[] bg = {true, true, true, true, true};
    public static boolean cpsRight = true;
    public static boolean coordsY = true;
    /** Replace the vanilla title screen with the Sushi title screen. */
    public static boolean customTitle = true;

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sushi-core.properties");

    private SushiConfig() {}

    public static void load() {
        Properties p = new Properties();
        if (Files.exists(FILE)) {
            try (InputStream in = Files.newInputStream(FILE)) {
                p.load(in);
            } catch (IOException ignored) {
                // Defaults are used if the file is unreadable.
            }
        }
        for (int i = 0; i < KEYS.length; i++) {
            String k = KEYS[i];
            enabled[i] = bool(p, k + ".enabled", enabled[i]);
            x[i] = integer(p, k + ".x", -1);
            y[i] = integer(p, k + ".y", -1);
            scale[i] = Math.max(50, Math.min(200, integer(p, k + ".scale", 100)));
            bg[i] = bool(p, k + ".bg", true);
        }
        cpsRight = bool(p, "cps.right", true);
        coordsY = bool(p, "coords.y", true);
        customTitle = bool(p, "title.custom", true);
    }

    public static void save() {
        Properties p = new Properties();
        for (int i = 0; i < KEYS.length; i++) {
            String k = KEYS[i];
            p.setProperty(k + ".enabled", String.valueOf(enabled[i]));
            p.setProperty(k + ".x", String.valueOf(x[i]));
            p.setProperty(k + ".y", String.valueOf(y[i]));
            p.setProperty(k + ".scale", String.valueOf(scale[i]));
            p.setProperty(k + ".bg", String.valueOf(bg[i]));
        }
        p.setProperty("cps.right", String.valueOf(cpsRight));
        p.setProperty("coords.y", String.valueOf(coordsY));
        p.setProperty("title.custom", String.valueOf(customTitle));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Sushi Core settings");
            }
        } catch (IOException ignored) {
            // Settings simply won't persist if the config folder is not writable.
        }
    }

    private static boolean bool(Properties p, String key, boolean def) {
        return Boolean.parseBoolean(p.getProperty(key, String.valueOf(def)));
    }

    private static int integer(Properties p, String key, int def) {
        try {
            return Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
