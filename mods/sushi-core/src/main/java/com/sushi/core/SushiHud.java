package com.sushi.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

import static com.sushi.core.SushiTheme.*;

/** Draws the Sushi HUD: stats panel (top-left) and keystroke board (bottom-left). */
final class SushiHud {
    private SushiHud() {}

    static void render(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden || client.currentScreen instanceof SushiMenuScreen) {
            return;
        }
        TextRenderer tr = client.textRenderer;
        renderStats(ctx, tr, client);
        if (SushiConfig.keystrokes) {
            renderKeystrokes(ctx, tr, client);
        }
    }

    private static void renderStats(DrawContext ctx, TextRenderer tr, MinecraftClient client) {
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (SushiConfig.fps) {
            int fps = client.getCurrentFps();
            lines.add("FPS " + fps);
            colors.add(fps >= 120 ? CYAN : fps >= 60 ? TEXT : PURPLE);
        }
        if (SushiConfig.cps) {
            lines.add("CPS " + ClickTracker.leftCps() + " | " + ClickTracker.rightCps());
            colors.add(TEXT);
        }
        if (SushiConfig.ping) {
            int ping = -1;
            if (client.getNetworkHandler() != null) {
                PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
                if (entry != null) ping = entry.getLatency();
            }
            lines.add("Ping " + (ping < 0 ? "-" : ping + " ms"));
            colors.add(TEXT);
        }
        if (SushiConfig.coords) {
            lines.add("XYZ " + client.player.getBlockX() + " " + client.player.getBlockY() + " " + client.player.getBlockZ());
            colors.add(TEXT);
        }
        if (lines.isEmpty()) {
            return;
        }

        int width = 0;
        for (String s : lines) width = Math.max(width, tr.getWidth(s));
        int x = 6, y = 6;
        int h = lines.size() * 11 + 8;
        ctx.fill(x, y, x + width + 14, y + h, HUD_BG);
        ctx.fill(x, y, x + 2, y + h, CYAN);
        ctx.fill(x + 2, y, x + 3, y + h, PURPLE_DIM);

        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(tr, lines.get(i), x + 8, y + 5 + i * 11, colors.get(i), true);
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
        ctx.fill(x, y, x + w, y + h, down ? CYAN : KEY_BG);
        int tx = x + (w - tr.getWidth(label)) / 2;
        int ty = y + (h - 8) / 2 + 1;
        ctx.drawText(tr, label, tx, ty, down ? DARK : MUTED, false);
    }
}
