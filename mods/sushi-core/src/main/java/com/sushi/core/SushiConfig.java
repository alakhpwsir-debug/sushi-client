package com.sushi.core;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** Module, HUD layout and option settings, stored in config/sushi-core.properties. */
public final class SushiConfig {
    // Module indexes. Each module has one entry in the arrays below.
    public static final int FPS = 0;
    public static final int CPS = 1;
    public static final int KEYSTROKES = 2;
    public static final int PING = 3;
    public static final int COORDS = 4;
    public static final int ZOOM = 5;
    public static final int ARMOR = 6;
    public static final int POTIONS = 7;
    public static final int CROSSHAIR = 8;
    public static final int WAYPOINTS = 9;
    public static final int HIT_COLOR = 10;
    public static final int SCOREBOARD = 11;
    public static final int MOTION_BLUR = 12;
    public static final int SHULKER = 13;
    public static final int MEMORY = 14;
    public static final int MINIMAP = 15;
    public static final int HITBOX = 16;

    public static final String[] KEYS = {
        "fps", "cps", "keystrokes", "ping", "coords",
        "zoom", "armor", "potions", "crosshair", "waypoints", "hitcolor", "scoreboard",
        "motionblur", "shulker", "memory", "minimap", "hitbox",
    };
    public static final String[] NAMES = {
        "FPS", "CPS", "Keystrokes", "Ping", "Coordinates",
        "Zoom", "Armor Status", "Potion Effects", "Crosshair", "Waypoints", "Hit Color", "Scoreboard",
        "Motion Blur", "Shulker Preview", "Memory Usage", "Minimap", "Hitbox",
    };
    public static final String[] DESCS = {
        "Frames per second", "Clicks per second", "Keys as you press them", "Connection latency", "Your block position",
        "Hold C to zoom in", "Your worn armor", "Active potion effects", "Custom centre crosshair", "Saved points and distance",
        "Colour of the hurt flash", "Show or hide the sidebar", "Blur while you move", "Contents of held shulker",
        "Java heap in use", "Map of explored land", "Shows entity hitboxes",
    };
    public static final int[] SCALES = {50, 75, 100, 125, 150, 200};

    /** Module state. For Scoreboard, enabled means the sidebar is shown. */
    public static final boolean[] enabled = {
        true, true, true, true, false,
        true, false, false, false, false, false, true,
        false, false, false, false, false,
    };
    /** Screen position in GUI pixels. -1 means "use the default spot". */
    public static final int[] x = new int[KEYS.length];
    public static final int[] y = new int[KEYS.length];
    public static final int[] scale = new int[KEYS.length];
    public static final boolean[] bg = new boolean[KEYS.length];

    public static boolean cpsRight = true;
    public static boolean coordsY = true;
    /** Replace the vanilla title screen with the Sushi title screen. */
    public static boolean customTitle = true;

    /** Colours shared by the crosshair and hit colour. ARGB. */
    public static final int[] COLORS = {0xFF22D3EE, 0xFFFFFFFF, 0xFFA855F7, 0xFF4ADE80, 0xFFFACC15, 0xFFF43F5E};
    public static final String[] COLOR_NAMES = {"Cyan", "White", "Purple", "Green", "Yellow", "Red"};
    public static final int[] ZOOM_FACTORS = {2, 3, 4, 6, 8};
    public static final int[] MOTION_STEPS = {10, 20, 30, 40, 50};
    public static final int[] MINIMAP_SIZES = {96, 128, 160};
    public static final String[] CROSSHAIR_STYLES = {"Dot", "Plus", "Cross"};

    public static int zoomFactor = 4;
    public static int crosshairStyle = 1;
    public static int crosshairColor = 0;
    public static int hitColor = 0;
    public static int motionBlur = 30;
    public static int minimapSize = 128;

    public static final List<SushiWaypoints.Waypoint> waypoints = new ArrayList<>();

    static {
        for (int i = 0; i < KEYS.length; i++) {
            x[i] = -1;
            y[i] = -1;
            scale[i] = 100;
            bg[i] = true;
        }
        bg[COORDS] = true;
    }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sushi-core.properties");

    private SushiConfig() {}

    public static boolean hasBox(int i) {
        return i != ZOOM && i != CROSSHAIR && i != HIT_COLOR && i != SCOREBOARD && i != MOTION_BLUR && i != HITBOX;
    }

    public static boolean hasOptions(int i) {
        return hasBox(i) || i == ZOOM || i == CROSSHAIR || i == HIT_COLOR || i == MOTION_BLUR;
    }

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
        zoomFactor = pick(ZOOM_FACTORS, integer(p, "zoom.factor", zoomFactor));
        crosshairStyle = Math.max(0, Math.min(CROSSHAIR_STYLES.length - 1, integer(p, "crosshair.style", crosshairStyle)));
        crosshairColor = Math.max(0, Math.min(COLORS.length - 1, integer(p, "crosshair.color", crosshairColor)));
        hitColor = Math.max(0, Math.min(COLORS.length - 1, integer(p, "hitcolor.index", hitColor)));
        motionBlur = pick(MOTION_STEPS, integer(p, "motionblur.amount", motionBlur));
        minimapSize = pick(MINIMAP_SIZES, integer(p, "minimap.size", minimapSize));

        waypoints.clear();
        int count = Math.min(200, integer(p, "wp.count", 0));
        for (int i = 0; i < count; i++) {
            String name = p.getProperty("wp." + i + ".name", "Waypoint");
            String dim = p.getProperty("wp." + i + ".dim", "minecraft:overworld");
            waypoints.add(new SushiWaypoints.Waypoint(name, integer(p, "wp." + i + ".x", 0),
                    integer(p, "wp." + i + ".y", 64), integer(p, "wp." + i + ".z", 0), dim));
        }
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
        p.setProperty("zoom.factor", String.valueOf(zoomFactor));
        p.setProperty("crosshair.style", String.valueOf(crosshairStyle));
        p.setProperty("crosshair.color", String.valueOf(crosshairColor));
        p.setProperty("hitcolor.index", String.valueOf(hitColor));
        p.setProperty("motionblur.amount", String.valueOf(motionBlur));
        p.setProperty("minimap.size", String.valueOf(minimapSize));
        p.setProperty("wp.count", String.valueOf(waypoints.size()));
        for (int i = 0; i < waypoints.size(); i++) {
            SushiWaypoints.Waypoint w = waypoints.get(i);
            p.setProperty("wp." + i + ".name", w.name());
            p.setProperty("wp." + i + ".x", String.valueOf(w.x()));
            p.setProperty("wp." + i + ".y", String.valueOf(w.y()));
            p.setProperty("wp." + i + ".z", String.valueOf(w.z()));
            p.setProperty("wp." + i + ".dim", w.dimension());
        }
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                p.store(out, "Sushi Core settings");
            }
        } catch (IOException ignored) {
            // Settings simply won't persist if the config folder is not writable.
        }
    }

    private static int pick(int[] allowed, int value) {
        for (int v : allowed) {
            if (v == value) return v;
        }
        return allowed[0];
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
