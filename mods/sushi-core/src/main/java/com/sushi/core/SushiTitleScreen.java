package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.sushi.core.SushiTheme.*;

/**
 * Sushi title screen. Original layout: a centered menu, an account bar at the top,
 * Sushi shortcuts, a tips card, and a footer. Shown in place of the vanilla title screen.
 */
public class SushiTitleScreen extends Screen {
    private static final int BTN_W = 220;
    private static final int BTN_H = 20;
    private static final int BTN_STEP = 24;
    private static final int PILL_H = 18;
    private static final int TOP_BG = 0xFF0C0E24;
    private static final int BOTTOM_BG = 0xFF05060D;
    private static final int PURPLE2 = 0xFFC084FC;

    private record Rect(int x, int y, int w, int h, String label, Runnable run) {
        boolean hit(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    public SushiTitleScreen() {
        super(Text.literal("Sushi Client"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private List<Rect> menuRects() {
        List<Rect> out = new ArrayList<>();
        int x = width / 2 - BTN_W / 2;
        int y = (int) (height * 0.44);
        out.add(new Rect(x, y, BTN_W, BTN_H, "Singleplayer", () -> client.setScreen(new SelectWorldScreen(this))));
        y += BTN_STEP;
        out.add(new Rect(x, y, BTN_W, BTN_H, "Multiplayer", () -> client.setScreen(new MultiplayerScreen(this))));
        y += BTN_STEP;
        out.add(new Rect(x, y, BTN_W, BTN_H, "Options", () -> client.setScreen(new OptionsScreen(this, client.options))));
        y += BTN_STEP;
        out.add(new Rect(x, y, BTN_W, BTN_H, "Keybinds", () -> client.setScreen(new KeybindsScreen(this, client.options))));
        y += BTN_STEP;
        out.add(new Rect(x, y, BTN_W, BTN_H, "Quit Game", () -> client.scheduleStop()));
        return out;
    }

    private List<Rect> topRects() {
        List<Rect> out = new ArrayList<>();
        String name = client.getSession().getUsername();
        String account = "Playing as " + name;
        out.add(new Rect(8, 8, textRenderer.getWidth(account) + 20, PILL_H, account, () -> { }));

        String menu = "Sushi menu";
        String hud = "HUD layout";
        int wMenu = textRenderer.getWidth(menu) + 20;
        int wHud = textRenderer.getWidth(hud) + 20;
        int xMenu = width - 8 - wMenu;
        out.add(new Rect(xMenu, 8, wMenu, PILL_H, menu, () -> client.setScreen(new SushiMenuScreen(this))));
        out.add(new Rect(xMenu - 6 - wHud, 8, wHud, PILL_H, hud, () -> client.setScreen(new HudEditorScreen(this))));
        return out;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        drawBackground(ctx);
        drawLogo(ctx);

        List<Rect> pills = topRects();
        List<Rect> menu = menuRects();
        for (Rect r : pills) drawButton(ctx, r, mouseX, mouseY);
        for (Rect r : menu) drawButton(ctx, r, mouseX, mouseY);
        drawTips(ctx);
        drawFooter(ctx);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Rect r : topRects()) {
                if (r.hit(mouseX, mouseY)) {
                    r.run().run();
                    return true;
                }
            }
            for (Rect r : menuRects()) {
                if (r.hit(mouseX, mouseY)) {
                    r.run().run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void drawBackground(DrawContext ctx) {
        for (int y = 0; y < height; y += 2) {
            ctx.fill(0, y, width, Math.min(height, y + 2), lerp(TOP_BG, BOTTOM_BG, y / (float) height));
        }
        // Soft purple glow top-right and cyan haze along the bottom.
        for (int y = 0; y < height / 2; y += 2) {
            float t = y / (height / 2f);
            ctx.fill(width / 2, y, width, Math.min(height, y + 2), lerp(0x26A855F7, 0x00A855F7, t));
        }
        for (int y = (int) (height * 0.6); y < height; y += 2) {
            float t = (y - height * 0.6f) / (height * 0.4f);
            ctx.fill(0, y, width, Math.min(height, y + 2), lerp(0x0022D3EE, 0x2222D3EE, t));
        }
    }

    private void drawLogo(DrawContext ctx) {
        int cx = width / 2;
        int cy = (int) (height * 0.22);
        int r = Math.max(16, Math.min(28, height / 10));
        disc(ctx, cx, cy, r, PURPLE);
        disc(ctx, cx, cy, r - 3, 0xFF0B0D1F);
        disc(ctx, cx, cy, (int) (r * 0.75f), CYAN);
        disc(ctx, cx, cy, (int) (r * 0.46f), 0xFF0B0D1F);
        disc(ctx, cx, cy, (int) (r * 0.22f), PURPLE2);

        int wordY = (int) (height * 0.33);
        ctx.getMatrices().push();
        ctx.getMatrices().translate(cx, wordY, 0);
        ctx.getMatrices().scale(3f, 3f, 1f);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("SUSHI"), 0, 0, TEXT);
        ctx.getMatrices().pop();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("C L I E N T"), cx, wordY + 28, PURPLE2);
    }

    private void drawButton(DrawContext ctx, Rect r, int mouseX, int mouseY) {
        boolean hot = r.hit(mouseX, mouseY);
        ctx.fill(r.x() - 1, r.y() - 1, r.x() + r.w() + 1, r.y() + r.h() + 1, hot ? CYAN : CARD_BORDER);
        ctx.fill(r.x(), r.y(), r.x() + r.w(), r.y() + r.h(), hot ? 0xF01A1D3A : CARD_BG);
        ctx.fill(r.x(), r.y(), r.x() + 2, r.y() + r.h(), hot ? PURPLE : PURPLE_DIM);
        int ty = r.y() + (r.h() - 8) / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(r.label()), r.x() + r.w() / 2, ty, hot ? TEXT : MUTED);
    }

    private void drawTips(DrawContext ctx) {
        int w = 220;
        int h = 80;
        int x = width - w - 12;
        int y = height - h - 30;
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, CARD_BORDER);
        ctx.fill(x, y, x + w, y + h, CARD_BG);
        ctx.fill(x, y, x + w, y + 2, CYAN);
        ctx.drawText(textRenderer, "TIPS", x + 12, y + 12, CYAN, false);
        ctx.drawText(textRenderer, "Right Shift opens the Sushi menu.", x + 12, y + 28, TEXT, false);
        ctx.drawText(textRenderer, "Edit HUD layout moves HUD boxes.", x + 12, y + 42, MUTED, false);
        ctx.drawText(textRenderer, "Each module has its own scale.", x + 12, y + 56, MUTED, false);
    }

    private void drawFooter(DrawContext ctx) {
        ctx.drawText(textRenderer, "Sushi Core 0.2.0 · Minecraft 1.21.1", 8, height - 22, MUTED, false);
        ctx.drawText(textRenderer, "Not affiliated with Mojang or Microsoft.", 8, height - 12, DISABLED, false);
    }

    private static void disc(DrawContext ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int half = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            ctx.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    private static int lerp(int a, int b, float t) {
        float tt = Math.max(0f, Math.min(1f, t));
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int oa = Math.round(aa + (ba - aa) * tt);
        int or = Math.round(ar + (br - ar) * tt);
        int og = Math.round(ag + (bg - ag) * tt);
        int ob = Math.round(ab + (bb - ab) * tt);
        return (oa << 24) | (or << 16) | (og << 8) | ob;
    }
}
