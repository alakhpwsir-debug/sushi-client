package com.sushi.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import static com.sushi.core.SushiTheme.*;

/** Draws each enabled module as its own HUD element at its saved position and scale. */
final class SushiHud {
    static final int KEYS = 2;
    static final int COUNT = SushiConfig.KEYS.length;
    private static final int PAD = 6;
    private static final int KEY_W = 72;
    private static final int KEY_H = 83;

    /** One laid-out element. w/h are unscaled; bw/bh are the on-screen size. */
    record Box(int x, int y, int bw, int bh, float s, int w, int h, String text, int color) {}

    private SushiHud() {}

    static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;
        if (client.currentScreen instanceof SushiMenuScreen || client.currentScreen instanceof HudEditorScreen) return;
        draw(ctx, client, boxes(client, client.textRenderer, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), false));
    }

    /** Works out where every enabled module sits. Used by the game HUD and the layout editor. */
    static Box[] boxes(MinecraftClient client, TextRenderer tr, int sw, int sh, boolean sample) {
        Box[] out = new Box[COUNT];
        int stackY = PAD;
        for (int i = 0; i < COUNT; i++) {
            if (!SushiConfig.enabled[i]) continue;
            float s = SushiConfig.scale[i] / 100f;
            String text = null;
            int color = TEXT;
            int w;
            int h;
            if (i == KEYS) {
                w = KEY_W;
                h = KEY_H;
            } else {
                text = text(i, client, sample);
                color = color(i, client, sample);
                w = tr.getWidth(text) + 14;
                h = 19;
            }
            int bw = Math.round(w * s);
            int bh = Math.round(h * s);
            int x;
            int y;
            if (SushiConfig.x[i] >= 0 && SushiConfig.y[i] >= 0) {
                x = SushiConfig.x[i];
                y = SushiConfig.y[i];
            } else if (i == KEYS) {
                x = PAD;
                y = sh - bh - 40;
            } else {
                x = PAD;
                y = stackY;
                stackY += bh + 4;
            }
            x = clamp(x, 0, Math.max(0, sw - bw));
            y = clamp(y, 0, Math.max(0, sh - bh));
            out[i] = new Box(x, y, bw, bh, s, w, h, text, color);
        }
        return out;
    }

    static void draw(DrawContext ctx, MinecraftClient client, Box[] boxes) {
        TextRenderer tr = client.textRenderer;
        for (int i = 0; i < boxes.length; i++) {
            Box b = boxes[i];
            if (b == null) continue;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(b.x(), b.y(), 0);
            ctx.getMatrices().scale(b.s(), b.s(), 1f);
            if (i == KEYS) {
                drawKeys(ctx, tr, client);
            } else {
                drawText(ctx, tr, i, b);
            }
            ctx.getMatrices().pop();
        }
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static String text(int i, MinecraftClient client, boolean sample) {
        switch (i) {
            case 0:
                return "FPS " + (sample ? "184" : String.valueOf(client.getCurrentFps()));
            case 1: {
                String left = sample ? "6" : String.valueOf(ClickTracker.leftCps());
                if (!SushiConfig.cpsRight) return "CPS " + left;
                String right = sample ? "2" : String.valueOf(ClickTracker.rightCps());
                return "CPS " + left + " | " + right;
            }
            case 3:
                return "Ping " + (sample ? "38 ms" : pingText(client));
            default:
                return coordsText(client, sample);
        }
    }

    static int color(int i, MinecraftClient client, boolean sample) {
        if (i != 0) return TEXT;
        int fps = sample ? 184 : client.getCurrentFps();
        return fps >= 120 ? CYAN : fps >= 60 ? TEXT : PURPLE;
    }

    private static String pingText(MinecraftClient client) {
        if (client.player == null || client.getNetworkHandler() == null) return "-";
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? "-" : entry.getLatency() + " ms";
    }

    private static String coordsText(MinecraftClient client, boolean sample) {
        if (sample) return SushiConfig.coordsY ? "XYZ 128 64 -32" : "XZ 128 -32";
        if (client.player == null) return "XYZ -";
        int x = client.player.getBlockX();
        int y = client.player.getBlockY();
        int z = client.player.getBlockZ();
        return SushiConfig.coordsY ? "XYZ " + x + " " + y + " " + z : "XZ " + x + " " + z;
    }

    private static void drawText(DrawContext ctx, TextRenderer tr, int i, Box b) {
        if (SushiConfig.bg[i]) ctx.fill(0, 0, b.w(), b.h(), HUD_BG);
        ctx.fill(0, 0, 2, b.h(), CYAN);
        ctx.drawText(tr, b.text(), 8, 5, b.color(), true);
    }

    private static void drawKeys(DrawContext ctx, TextRenderer tr, MinecraftClient client) {
        var o = client.options;
        int s = 22;
        int g = 3;
        int bg = SushiConfig.bg[KEYS] ? KEY_BG : 0x40141529;
        key(ctx, tr, "W", s + g, 0, s, s, o.forwardKey.isPressed(), bg);
        key(ctx, tr, "A", 0, s + g, s, s, o.leftKey.isPressed(), bg);
        key(ctx, tr, "S", s + g, s + g, s, s, o.backKey.isPressed(), bg);
        key(ctx, tr, "D", 2 * (s + g), s + g, s, s, o.rightKey.isPressed(), bg);
        key(ctx, tr, "SPACE", 0, 2 * (s + g), 3 * s + 2 * g, 14, o.jumpKey.isPressed(), bg);
        key(ctx, tr, "LMB", 0, 2 * (s + g) + 17, 33, 16, o.attackKey.isPressed(), bg);
        key(ctx, tr, "RMB", 36, 2 * (s + g) + 17, 33, 16, o.useKey.isPressed(), bg);
    }

    private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h, boolean down, int bg) {
        ctx.fill(x, y, x + w, y + h, down ? CYAN : bg);
        int tx = x + (w - tr.getWidth(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        ctx.drawText(tr, label, tx, ty, down ? DARK : MUTED, false);
    }
}
