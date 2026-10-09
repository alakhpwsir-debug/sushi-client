package com.sushi.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

/** Draws the Sushi HUD: stats box (top-left) and keystroke board (bottom-left). */
final class SushiHud {
    private static final int BOX_BG = 0xB00D1410;
    private static final int KEY_BG = 0xB015201A;
    private static final int ACCENT = 0xFFFF7F5C;
    private static final int WASABI = 0xFF9CCC65;
    private static final int TEXT = 0xFFF4EFE6;
    private static final int MUTED = 0xFF8EA597;
    private static final int DARK = 0xFF0D1410;

    private SushiHud() {}

    static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        TextRenderer tr = client.textRenderer;

        renderStats(ctx, tr, client);
        renderKeystrokes(ctx, tr, client);
    }

    private static void renderStats(DrawContext ctx, TextRenderer tr, MinecraftClient client) {
        int fps = client.getCurrentFps();
        int fpsColor = fps >= 120 ? WASABI : fps >= 60 ? TEXT : ACCENT;

        int ping = -1;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }

        String[] labels = {
                "FPS " + fps,
                "CPS " + ClickTracker.leftCps() + " | " + ClickTracker.rightCps(),
                "Ping " + (ping < 0 ? "-" : ping + " ms"),
                "XYZ " + client.player.getBlockX() + " " + client.player.getBlockY() + " " + client.player.getBlockZ(),
        };

        int width = 0;
        for (String s : labels) width = Math.max(width, tr.getWidth(s));
        int x = 6, y = 6;
        int h = labels.length * 11 + 8;
        ctx.fill(x, y, x + width + 14, y + h, BOX_BG);
        ctx.fill(x, y, x + 2, y + h, ACCENT);

        for (int i = 0; i < labels.length; i++) {
            int color = i == 0 ? fpsColor : TEXT;
            ctx.drawText(tr, labels[i], x + 8, y + 5 + i * 11, color, true);
        }
    }

    private static void renderKeystrokes(DrawContext ctx, TextRenderer tr, MinecraftClient client) {
        var o = client.options;
        int ox = 6;
        int oy = ctx.getScaledWindowHeight() - 120;
        int s = 22, g = 3;

        key(ctx, tr, "W", ox + s + g, oy, s, s, o.forwardKey.isPressed());
        key(ctx, tr, "A", ox, oy + s + g, s, s, o.leftKey.isPressed());
        key(ctx, tr, "S", ox + s + g, oy + s + g, s, s, o.backKey.isPressed());
        key(ctx, tr, "D", ox + 2 * (s + g), oy + s + g, s, s, o.rightKey.isPressed());
        key(ctx, tr, "SPACE", ox, oy + 2 * (s + g), 3 * s + 2 * g, 14, o.jumpKey.isPressed());
        key(ctx, tr, "LMB", ox, oy + 2 * (s + g) + 17, 33, 16, o.attackKey.isPressed());
        key(ctx, tr, "RMB", ox + 36, oy + 2 * (s + g) + 17, 33, 16, o.useKey.isPressed());
    }

    private static void key(DrawContext ctx, TextRenderer tr, String label, int x, int y, int w, int h, boolean down) {
        ctx.fill(x, y, x + w, y + h, down ? ACCENT : KEY_BG);
        int tx = x + (w - tr.getWidth(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        ctx.drawText(tr, label, tx, ty, down ? DARK : MUTED, false);
    }
}
