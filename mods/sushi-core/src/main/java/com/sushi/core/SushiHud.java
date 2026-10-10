package com.sushi.core;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.sushi.core.SushiTheme.*;

/** Draws each enabled HUD module at its saved position and scale. */
final class SushiHud {
    static final int COUNT = SushiConfig.KEYS.length;
    private static final int PAD = 6;
    private static final int KEY_W = 72;
    private static final int KEY_H = 83;
    private static final int MAX_LINES = 8;
    private static final int LINE_H = 11;

    /** One laid-out element. w/h are unscaled; bw/bh are the on-screen size. lines/colors hold the text, if any. */
    record Box(int x, int y, int bw, int bh, float s, int w, int h, String[] lines, int[] colors) {}

    /** Unscaled size of an element, plus its text. */
    private record Size(int w, int h, String[] lines, int[] colors) {}

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
            if (!SushiConfig.hasBox(i) || !SushiConfig.enabled[i]) continue;
            Size size = size(i, client, tr, sample);
            if (size == null) continue;
            float s = SushiConfig.scale[i] / 100f;
            int bw = Math.round(size.w() * s);
            int bh = Math.round(size.h() * s);
            int x;
            int y;
            if (SushiConfig.x[i] >= 0 && SushiConfig.y[i] >= 0) {
                x = SushiConfig.x[i];
                y = SushiConfig.y[i];
            } else {
                switch (i) {
                    case SushiConfig.KEYSTROKES -> {
                        x = PAD;
                        y = sh - bh - 40;
                    }
                    case SushiConfig.ARMOR -> {
                        x = sw / 2 + 96;
                        y = sh - bh - 4;
                    }
                    case SushiConfig.SHULKER -> {
                        x = sw / 2 - bw / 2;
                        y = sh - bh - 40;
                    }
                    case SushiConfig.MINIMAP -> {
                        x = sw - bw - PAD;
                        y = PAD;
                    }
                    case SushiConfig.POTIONS -> {
                        x = sw - bw - PAD;
                        y = PAD + 176;
                    }
                    default -> {
                        x = PAD;
                        y = stackY;
                        stackY += bh + 4;
                    }
                }
            }
            x = clamp(x, 0, Math.max(0, sw - bw));
            y = clamp(y, 0, Math.max(0, sh - bh));
            out[i] = new Box(x, y, bw, bh, s, size.w(), size.h(), size.lines(), size.colors());
        }
        return out;
    }

    /** Unscaled size and text of one module, or null when it has nothing to show right now. */
    private static Size size(int i, MinecraftClient client, TextRenderer tr, boolean sample) {
        switch (i) {
            case SushiConfig.KEYSTROKES:
                return new Size(KEY_W, KEY_H, new String[0], new int[0]);
            case SushiConfig.ARMOR: {
                if (!sample && !hasArmor(client)) return null;
                return new Size(4 + 4 * 20, 24, new String[0], new int[0]);
            }
            case SushiConfig.SHULKER: {
                if (!sample && !holdingShulker(client)) return null;
                return new Size(9 * 18 + 8, 3 * 18 + 8, new String[0], new int[0]);
            }
            case SushiConfig.MINIMAP: {
                int s = SushiConfig.minimapSize + 4;
                return new Size(s, s, new String[0], new int[0]);
            }
            case SushiConfig.POTIONS: {
                List<String> lines = potionLines(client, sample);
                if (lines.isEmpty()) return null;
                return listSize(tr, lines, potionColors(client, lines.size(), sample), 60);
            }
            case SushiConfig.WAYPOINTS: {
                List<String> lines = waypointLines(client, sample);
                if (lines.isEmpty()) return null;
                int[] colors = new int[lines.size()];
                java.util.Arrays.fill(colors, TEXT);
                return listSize(tr, lines, colors, 80);
            }
            default: {
                String text = text(i, client, sample);
                int color = color(i, client, sample);
                return new Size(tr.getWidth(text) + 14, 19, new String[] {text}, new int[] {color});
            }
        }
    }

    private static Size listSize(TextRenderer tr, List<String> lines, int[] colors, int minW) {
        int w = minW;
        for (String l : lines) w = Math.max(w, tr.getWidth(l) + 16);
        int h = 6 + lines.size() * LINE_H + 4;
        return new Size(w, h, lines.toArray(new String[0]), colors);
    }

    static void draw(DrawContext ctx, MinecraftClient client, Box[] boxes) {
        TextRenderer tr = client.textRenderer;
        for (int i = 0; i < boxes.length; i++) {
            Box b = boxes[i];
            if (b == null) continue;
            ctx.getMatrices().push();
            ctx.getMatrices().translate(b.x(), b.y(), 0);
            ctx.getMatrices().scale(b.s(), b.s(), 1f);
            switch (i) {
                case SushiConfig.KEYSTROKES -> drawKeys(ctx, tr, client);
                case SushiConfig.ARMOR -> drawArmor(ctx, tr, client, i);
                case SushiConfig.SHULKER -> drawShulker(ctx, tr, client, i);
                case SushiConfig.MINIMAP -> drawMinimap(ctx, tr, i);
                default -> drawLines(ctx, tr, i, b);
            }
            ctx.getMatrices().pop();
        }
    }

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static String text(int i, MinecraftClient client, boolean sample) {
        switch (i) {
            case SushiConfig.FPS:
                return "FPS " + (sample ? "184" : String.valueOf(client.getCurrentFps()));
            case SushiConfig.CPS: {
                String left = sample ? "6" : String.valueOf(ClickTracker.leftCps());
                if (!SushiConfig.cpsRight) return "CPS " + left;
                String right = sample ? "2" : String.valueOf(ClickTracker.rightCps());
                return "CPS " + left + " | " + right;
            }
            case SushiConfig.PING:
                return "Ping " + (sample ? "38 ms" : pingText(client));
            case SushiConfig.MEMORY:
                return memoryText(sample);
            default:
                return coordsText(client, sample);
        }
    }

    static int color(int i, MinecraftClient client, boolean sample) {
        if (i != SushiConfig.FPS) return TEXT;
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

    private static String memoryText(boolean sample) {
        if (sample) return "MEM 1.2 / 4.0 GB";
        Runtime rt = Runtime.getRuntime();
        double gb = 1024.0 * 1024.0 * 1024.0;
        double used = (rt.totalMemory() - rt.freeMemory()) / gb;
        double max = rt.maxMemory() / gb;
        return String.format(Locale.ROOT, "MEM %.1f / %.1f GB", used, max);
    }

    private static boolean hasArmor(MinecraftClient client) {
        if (client.player == null) return false;
        for (int k = 0; k < 4; k++) {
            if (!client.player.getInventory().getArmorStack(k).isEmpty()) return true;
        }
        return false;
    }

    static boolean isShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ShulkerBoxBlock;
    }

    private static boolean holdingShulker(MinecraftClient client) {
        if (client.player == null) return false;
        return isShulker(client.player.getMainHandStack()) || isShulker(client.player.getOffHandStack());
    }

    private static List<String> potionLines(MinecraftClient client, boolean sample) {
        List<String> out = new ArrayList<>();
        if (sample) {
            out.add("Speed II  03:00");
            return out;
        }
        if (client.player == null) return out;
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            if (out.size() >= MAX_LINES) break;
            String name = effect.getEffectType().value().getName().getString();
            String time = effect.isInfinite() ? "--:--" : formatTime(effect.getDuration());
            out.add(name + " " + roman(effect.getAmplifier() + 1) + "  " + time);
        }
        return out;
    }

    private static int[] potionColors(MinecraftClient client, int count, boolean sample) {
        int[] colors = new int[count];
        if (sample || client.player == null) {
            java.util.Arrays.fill(colors, CYAN);
            return colors;
        }
        int k = 0;
        for (StatusEffectInstance effect : client.player.getStatusEffects()) {
            if (k >= count) break;
            StatusEffectCategory category = effect.getEffectType().value().getCategory();
            colors[k++] = category == StatusEffectCategory.HARMFUL ? 0xFFF43F5E
                : category == StatusEffectCategory.BENEFICIAL ? CYAN : TEXT;
        }
        return colors;
    }

    private static List<String> waypointLines(MinecraftClient client, boolean sample) {
        List<String> out = new ArrayList<>();
        if (sample) {
            out.add("Waypoint 1  120 m");
            return out;
        }
        for (SushiWaypoints.Waypoint w : SushiWaypoints.here(client)) {
            if (out.size() >= 6) break;
            out.add(w.name() + "  " + SushiWaypoints.distance(client, w) + " m");
        }
        return out;
    }

    private static String formatTime(int ticks) {
        int total = Math.max(0, ticks / 20);
        return String.format(Locale.ROOT, "%02d:%02d", total / 60, total % 60);
    }

    private static String roman(int n) {
        String[] r = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return n >= 1 && n <= r.length ? r[n - 1] : String.valueOf(n);
    }

    /** Plain single-line text (FPS, CPS, Ping, Coordinates, Memory). */
    private static void drawLines(DrawContext ctx, TextRenderer tr, int i, Box b) {
        if (SushiConfig.bg[i]) ctx.fill(0, 0, b.w(), b.h(), HUD_BG);
        ctx.fill(0, 0, 2, b.h(), CYAN);
        if (b.lines().length == 1) {
            ctx.drawText(tr, b.lines()[0], 8, 5, b.colors()[0], true);
            return;
        }
        for (int k = 0; k < b.lines().length; k++) {
            ctx.drawText(tr, b.lines()[k], 8, 6 + k * LINE_H, b.colors()[k], true);
        }
    }

    private static void drawArmor(DrawContext ctx, TextRenderer tr, MinecraftClient client, int i) {
        if (SushiConfig.bg[i]) ctx.fill(0, 0, 84, 24, HUD_BG);
        if (client.player == null) return;
        // Helmet first, boots last.
        for (int k = 0; k < 4; k++) {
            ItemStack stack = client.player.getInventory().getArmorStack(3 - k);
            ctx.drawItemInSlot(tr, stack, 4 + k * 20, 4);
        }
    }

    private static void drawShulker(DrawContext ctx, TextRenderer tr, MinecraftClient client, int i) {
        if (SushiConfig.bg[i]) ctx.fill(0, 0, 9 * 18 + 8, 3 * 18 + 8, HUD_BG);
        List<ItemStack> items = new ArrayList<>();
        ItemStack held = client.player == null ? ItemStack.EMPTY : client.player.getMainHandStack();
        if (!isShulker(held) && client.player != null) held = client.player.getOffHandStack();
        if (isShulker(held)) {
            ContainerComponent comp = held.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
            comp.stream().forEach(items::add);
        }
        for (int slot = 0; slot < 27; slot++) {
            int col = slot % 9;
            int row = slot / 9;
            int sx = 4 + col * 18;
            int sy = 4 + row * 18;
            ctx.fill(sx, sy, sx + 16, sy + 16, 0x60000000);
            if (slot < items.size() && !items.get(slot).isEmpty()) {
                ctx.drawItemInSlot(tr, items.get(slot), sx, sy);
            }
        }
    }

    private static void drawMinimap(DrawContext ctx, TextRenderer tr, int i) {
        int s = SushiConfig.minimapSize;
        int outer = s + 4;
        ctx.fill(0, 0, outer, outer, HUD_BG);
        SushiMinimap.draw(ctx, 2, 2, s);
        ctx.fill(0, 0, outer, 1, CYAN_DIM);
        ctx.fill(0, outer - 1, outer, outer, CYAN_DIM);
        ctx.fill(0, 0, 1, outer, CYAN_DIM);
        ctx.fill(outer - 1, 0, outer, outer, CYAN_DIM);
        ctx.drawCenteredTextWithShadow(tr, net.minecraft.text.Text.literal("N"), outer / 2, 0, CYAN);
    }

    private static void drawKeys(DrawContext ctx, TextRenderer tr, MinecraftClient client) {
        var o = client.options;
        int s = 22;
        int g = 3;
        int bg = SushiConfig.bg[SushiConfig.KEYSTROKES] ? KEY_BG : 0x40141529;
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
