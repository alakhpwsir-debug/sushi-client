package com.sushi.core;

import net.minecraft.client.MinecraftClient;

/**
 * Drops the frame rate to a low cap while the game window is not focused (another app is in front,
 * or the window is minimized). Frames nobody is looking at cost GPU and CPU time, and that load is
 * what makes the other apps lag. Focus back and the normal frame cap returns.
 */
final class FocusFpsLimiter {
    /** Frame cap while the game is in the background. */
    static final int BACKGROUND_FPS = 30;

    private static boolean limited;

    private FocusFpsLimiter() {}

    static void tick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        boolean wantLimit = !client.isWindowFocused();
        if (wantLimit == limited) return;
        limited = wantLimit;
        int fps = wantLimit ? BACKGROUND_FPS : client.options.getMaxFps().getValue();
        client.getWindow().setFramerateLimit(fps);
    }
}
