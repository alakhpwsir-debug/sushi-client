package com.sushi.core;

import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import static com.sushi.core.SushiTheme.*;

/**
 * Sushi module menu, opened with Right Shift. Cards turn modules on and off and open their options.
 * The cards are split into pages so the whole list fits small windows.
 */
public class SushiMenuScreen extends Screen {
    private static final int COLS = 3;
    private static final int ROWS = 2;
    private static final int PER_PAGE = COLS * ROWS;
    private static final int CARD_H = 72;
    private static final int GAP = 10;
    private static final int TOP = 56;

    /** How long the open animation lasts. Short, so it feels snappy. */
    private static final long OPEN_NANOS = 240_000_000L;

    private final Screen parent;
    /** Set on the game clock each opening, so every open plays the animation. */
    private final long openedAt = System.nanoTime();
    /** 0 = invisible, 1 = fully shown. Read by the drawing code while the animation runs. */
    private float fade = 1f;
    private int page = 0;

    public SushiMenuScreen() {
        this(null);
    }

    public SushiMenuScreen(Screen parent) {
        super(Text.literal("Sushi Client"));
        this.parent = parent;
    }

    private int pages() {
        return (SushiConfig.NAMES.length + PER_PAGE - 1) / PER_PAGE;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Eased 0 to 1 over OPEN_NANOS (ease-out cubic, so it lands softly). */
    private float openProgress() {
        float t = (System.nanoTime() - openedAt) / (float) OPEN_NANOS;
        if (t >= 1f) return 1f;
        if (t <= 0f) return 0f;
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    /** Fades an ARGB colour by the given factor (0 to 1). */
    private static int fadeColor(int argb, float f) {
        int a = Math.round(((argb >>> 24) & 0xFF) * f);
        return a <= 0 ? 0 : (a << 24) | (argb & 0xFFFFFF);
    }

    /** Plays the open animation: the menu rises, scales up from 94% and fades in. */
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float p = openProgress();
        if (p >= 1f) {
            if (fade != 1f) setButtonsAlpha(1f);
            fade = 1f;
            super.render(ctx, mouseX, mouseY, delta);
            return;
        }
        fade = p;
        setButtonsAlpha(p);
        ctx.getMatrices().push();
        float s = 0.94f + 0.06f * p;
        ctx.getMatrices().translate(width / 2f, height / 2f + (1f - p) * 12f, 0f);
        ctx.getMatrices().scale(s, s, 1f);
        ctx.getMatrices().translate(-width / 2f, -height / 2f, 0f);
        super.render(ctx, mouseX, mouseY, delta);
        ctx.getMatrices().pop();
    }

    private void setButtonsAlpha(float a) {
        for (Element e : children()) {
            if (e instanceof ClickableWidget w) w.setAlpha(a);
        }
    }

    private int cardW() {
        return Math.max(120, Math.min(180, (width - 40 - (COLS - 1) * GAP) / COLS));
    }

    private int startX() {
        int total = COLS * cardW() + (COLS - 1) * GAP;
        return (width - total) / 2;
    }

    private int cardX(int slot) {
        return startX() + (slot % COLS) * (cardW() + GAP);
    }

    private int cardY(int slot) {
        return TOP + (slot / COLS) * (CARD_H + GAP);
    }

    private int navY() {
        return TOP + ROWS * CARD_H + GAP + 10;
    }

    @Override
    protected void init() {
        clearChildren();
        page = Math.max(0, Math.min(page, pages() - 1));
        int first = page * PER_PAGE;
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int i = first + slot;
            if (i >= SushiConfig.NAMES.length) break;
            int idx = i;
            int x = cardX(slot);
            int y = cardY(slot);
            int cw = cardW();
            if (SushiConfig.hasOptions(idx)) {
                addDrawableChild(ButtonWidget.builder(Text.literal("Options"),
                        b -> client.setScreen(new ModuleOptionsScreen(this, idx)))
                    .dimensions(x + cw - 66, y + 6, 56, 16).build());
            }
            addDrawableChild(ButtonWidget.builder(label(idx), b -> {
                toggle(idx);
                b.setMessage(label(idx));
            }).dimensions(x + 12, y + CARD_H - 26, cw - 24, 18).build());
        }
        if (pages() > 1) {
            int cx = width / 2;
            ButtonWidget prev = ButtonWidget.builder(Text.literal("< Prev"), b -> {
                page--;
                clearAndInit();
            }).dimensions(cx - 120, navY(), 70, 18).build();
            prev.active = page > 0;
            addDrawableChild(prev);
            ButtonWidget next = ButtonWidget.builder(Text.literal("Next >"), b -> {
                page++;
                clearAndInit();
            }).dimensions(cx + 50, navY(), 70, 18).build();
            next.active = page < pages() - 1;
            addDrawableChild(next);
        }
        int by = height - 30;
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD layout"),
                b -> client.setScreen(new HudEditorScreen(this)))
            .dimensions(width / 2 - 230, by, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(titleLabel(), b -> {
                SushiConfig.customTitle = !SushiConfig.customTitle;
                SushiConfig.save();
                b.setMessage(titleLabel());
            }).dimensions(width / 2 - 75, by, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(width / 2 + 80, by, 150, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        float f = fade;
        // The dim is drawn a little past the screen edges so the scale-up leaves no gap.
        ctx.fill(-48, -48, width + 48, height + 48, fadeColor(0xC8070910, f));
        drawCenteredFaded(ctx, "SUSHI CLIENT", width / 2, 14, fadeColor(CYAN, f));
        drawCenteredFaded(ctx, "Modules", width / 2, 28, fadeColor(MUTED, f));
        int first = page * PER_PAGE;
        int cw = cardW();
        for (int slot = 0; slot < PER_PAGE; slot++) {
            int i = first + slot;
            if (i >= SushiConfig.NAMES.length) break;
            int x = cardX(slot);
            int y = cardY(slot);
            boolean on = SushiConfig.enabled[i];
            ctx.fill(x - 1, y - 1, x + cw + 1, y + CARD_H + 1, fadeColor(on ? PURPLE_DIM : CARD_BORDER, f));
            ctx.fill(x, y, x + cw, y + CARD_H, fadeColor(CARD_BG, f));
            ctx.fill(x, y, x + cw, y + 2, fadeColor(on ? CYAN : DISABLED, f));
            drawTextFaded(ctx, SushiConfig.NAMES[i], x + 12, y + 12, fadeColor(on ? TEXT : MUTED, f), true);
            drawTextFaded(ctx, SushiConfig.DESCS[i], x + 12, y + 26, fadeColor(MUTED, f), false);
        }
        if (pages() > 1) {
            drawCenteredFaded(ctx, "Page " + (page + 1) + " / " + pages(), width / 2, navY() + 5, fadeColor(MUTED, f));
        }
    }

    /** Skips text whose colour has faded to nothing (a zero alpha would draw it fully opaque). */
    private void drawTextFaded(DrawContext ctx, String text, int x, int y, int color, boolean shadow) {
        if ((color >>> 24) == 0) return;
        ctx.drawText(textRenderer, text, x, y, color, shadow);
    }

    private void drawCenteredFaded(DrawContext ctx, String text, int cx, int y, int color) {
        if ((color >>> 24) == 0) return;
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(text), cx, y, color);
    }

    private static Text label(int i) {
        return Text.literal(SushiConfig.enabled[i] ? "ENABLED" : "DISABLED");
    }

    private static Text titleLabel() {
        return Text.literal("Title: " + (SushiConfig.customTitle ? "SUSHI" : "VANILLA"));
    }

    private static void toggle(int i) {
        SushiConfig.enabled[i] = !SushiConfig.enabled[i];
        SushiConfig.save();
    }
}
