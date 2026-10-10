package com.sushi.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;

/** Draws the Sushi crosshair in the middle of the screen. The vanilla one is hidden while this is on. */
final class SushiCrosshair {
    private SushiCrosshair() {}

    static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!SushiConfig.enabled[SushiConfig.CROSSHAIR] || client.player == null) return;
        if (client.options.hudHidden || client.currentScreen != null) return;
        if (client.options.getPerspective() != Perspective.FIRST_PERSON) return;

        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;
        int color = SushiConfig.COLORS[SushiConfig.crosshairColor];
        int shadow = 0xC0000000;

        switch (SushiConfig.crosshairStyle) {
            case 0 -> { // dot
                ctx.fill(cx - 2, cy - 2, cx + 2, cy + 2, shadow);
                ctx.fill(cx - 1, cy - 1, cx + 1, cy + 1, color);
            }
            case 1 -> { // plus: two arms crossing at the centre, dark outline under the colour
                bar(ctx, cx, cy, 4, 1, shadow);
                bar(ctx, cx, cy, 4, 0, shadow);
                bar(ctx, cx, cy, 3, 1, color);
                bar(ctx, cx, cy, 3, 0, color);
            }
            default -> { // cross, thicker
                ctx.fill(cx - 4, cy - 1, cx + 5, cy + 2, shadow);
                ctx.fill(cx - 1, cy - 4, cx + 2, cy + 5, shadow);
                ctx.fill(cx - 3, cy, cx + 4, cy + 1, color);
                ctx.fill(cx, cy - 3, cx + 1, cy + 4, color);
            }
        }
    }

    /** Draws one arm of the plus. axis 0 is horizontal, axis 1 is vertical. */
    private static void bar(DrawContext ctx, int cx, int cy, int len, int axis, int colour) {
        if (axis == 0) {
            ctx.fill(cx - len, cy, cx + len + 1, cy + 1, colour);
        } else {
            ctx.fill(cx, cy - len, cx + 1, cy + len + 1, colour);
        }
    }
}
