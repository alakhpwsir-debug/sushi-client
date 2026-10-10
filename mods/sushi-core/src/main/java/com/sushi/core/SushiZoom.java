package com.sushi.core;

/** Zoom while the zoom key is held. The FOV eases in and out over a few ticks. */
public final class SushiZoom {
    private static float progress; // 0 = normal view, 1 = fully zoomed

    private SushiZoom() {}

    /** Called once per client tick with whether the zoom key is held. */
    static void tick(boolean held) {
        float target = held && SushiConfig.enabled[SushiConfig.ZOOM] ? 1f : 0f;
        progress += (target - progress) * 0.35f;
        if (Math.abs(target - progress) < 0.002f) progress = target;
    }

    /** Multiplier for the field of view this frame. 1 means no change. */
    public static double fovMultiplier() {
        if (progress <= 0f) return 1.0;
        double zoomed = 1.0 / SushiConfig.zoomFactor;
        return 1.0 + (zoomed - 1.0) * progress;
    }
}
