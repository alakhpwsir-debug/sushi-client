package com.sushi.core;

/**
 * Zoom while the zoom key is held. The FOV changes in one step rather than easing frame by frame:
 * every frame that the FOV changes forces the renderer to redo its visibility work, so a single
 * change is much cheaper than a slide that runs for many frames.
 */
public final class SushiZoom {
    private static boolean zoomed;

    private SushiZoom() {}

    /** Called once per client tick with whether the zoom key is held. */
    static void tick(boolean held) {
        zoomed = held && SushiConfig.enabled[SushiConfig.ZOOM];
    }

    /** Multiplier for the field of view this frame. 1 means no change. */
    public static double fovMultiplier() {
        return zoomed ? 1.0 / SushiConfig.zoomFactor : 1.0;
    }
}
