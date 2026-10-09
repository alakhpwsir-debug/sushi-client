package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static com.sushi.core.SushiTheme.*;

/** Sushi module menu, opened with Right Shift. Cards turn modules on and off and open their options. */
public class SushiMenuScreen extends Screen {
    private static final int COLS = 3;
    private static final int CARD_W = 180;
    private static final int CARD_H = 92;
    private static final int GAP = 14;

    public SushiMenuScreen() {
        super(Text.literal("Sushi Client"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int startX() {
        int total = COLS * CARD_W + (COLS - 1) * GAP;
        return (width - total) / 2;
    }

    private int startY() {
        return Math.max(60, height / 2 - 120);
    }

    private int cardX(int i) {
        return startX() + (i % COLS) * (CARD_W + GAP);
    }

    private int cardY(int i) {
        return startY() + (i / COLS) * (CARD_H + GAP);
    }

    @Override
    protected void init() {
        for (int i = 0; i < SushiConfig.NAMES.length; i++) {
            int idx = i;
            int x = cardX(i);
            int y = cardY(i);
            addDrawableChild(ButtonWidget.builder(Text.literal("Options"),
                    b -> client.setScreen(new ModuleOptionsScreen(this, idx)))
                .dimensions(x + CARD_W - 66, y + 8, 56, 16).build());
            addDrawableChild(ButtonWidget.builder(label(idx), b -> {
                toggle(idx);
                b.setMessage(label(idx));
            }).dimensions(x + 16, y + CARD_H - 34, CARD_W - 32, 20).build());
        }
        int by = height - 44;
        addDrawableChild(ButtonWidget.builder(Text.literal("Edit HUD layout"),
                b -> client.setScreen(new HudEditorScreen(this)))
            .dimensions(width / 2 - 160, by, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(width / 2 + 10, by, 150, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8070910);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("SUSHI CLIENT"), width / 2, 24, CYAN);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Modules"), width / 2, 38, MUTED);
        for (int i = 0; i < SushiConfig.NAMES.length; i++) {
            int x = cardX(i);
            int y = cardY(i);
            boolean on = SushiConfig.enabled[i];
            ctx.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, on ? PURPLE_DIM : CARD_BORDER);
            ctx.fill(x, y, x + CARD_W, y + CARD_H, CARD_BG);
            ctx.fill(x, y, x + CARD_W, y + 2, on ? CYAN : DISABLED);
            ctx.drawText(textRenderer, SushiConfig.NAMES[i], x + 16, y + 14, on ? TEXT : MUTED, true);
            ctx.drawText(textRenderer, SushiConfig.DESCS[i], x + 16, y + 28, MUTED, false);
        }
    }

    private static Text label(int i) {
        return Text.literal(SushiConfig.enabled[i] ? "ENABLED" : "DISABLED");
    }

    private static void toggle(int i) {
        SushiConfig.enabled[i] = !SushiConfig.enabled[i];
        SushiConfig.save();
    }
}
