package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static com.sushi.core.SushiTheme.*;

/** Sushi module menu, opened with Right Shift. Cards toggle HUD elements on and off. */
public class SushiMenuScreen extends Screen {
    private static final String[] NAMES = {"FPS", "CPS", "Keystrokes", "Ping", "Coordinates"};
    private static final String[] DESCS = {"Frames per second", "Clicks per second", "Keys as you press them", "Connection latency", "Your block position"};

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

    @Override
    protected void init() {
        int total = COLS * CARD_W + (COLS - 1) * GAP;
        int startX = (width - total) / 2;
        int startY = top();
        for (int i = 0; i < NAMES.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (CARD_W + GAP);
            int y = startY + row * (CARD_H + GAP);
            int idx = i;
            addDrawableChild(ButtonWidget.builder(label(idx), b -> {
                toggle(idx);
                b.setMessage(label(idx));
            }).dimensions(x + 16, y + CARD_H - 34, CARD_W - 32, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
                .dimensions(width / 2 - 70, height - 44, 140, 20).build());
    }

    private int top() {
        return Math.max(60, height / 2 - 120);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8070910);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("SUSHI CLIENT"), width / 2, 24, CYAN);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Modules"), width / 2, 38, MUTED);

        int total = COLS * CARD_W + (COLS - 1) * GAP;
        int startX = (width - total) / 2;
        int startY = top();
        for (int i = 0; i < NAMES.length; i++) {
            int col = i % COLS;
            int row = i / COLS;
            int x = startX + col * (CARD_W + GAP);
            int y = startY + row * (CARD_H + GAP);
            boolean on = enabled(i);

            ctx.fill(x - 1, y - 1, x + CARD_W + 1, y + CARD_H + 1, on ? PURPLE_DIM : CARD_BORDER);
            ctx.fill(x, y, x + CARD_W, y + CARD_H, CARD_BG);
            ctx.fill(x, y, x + CARD_W, y + 2, on ? CYAN : DISABLED);

            ctx.drawText(textRenderer, NAMES[i], x + 16, y + 14, on ? TEXT : MUTED, true);
            ctx.drawText(textRenderer, DESCS[i], x + 16, y + 28, MUTED, false);
        }
    }

    private static Text label(int i) {
        return Text.literal(enabled(i) ? "ENABLED" : "DISABLED");
    }

    private static boolean enabled(int i) {
        return switch (i) {
            case 0 -> SushiConfig.fps;
            case 1 -> SushiConfig.cps;
            case 2 -> SushiConfig.keystrokes;
            case 3 -> SushiConfig.ping;
            default -> SushiConfig.coords;
        };
    }

    private static void toggle(int i) {
        switch (i) {
            case 0 -> SushiConfig.fps = !SushiConfig.fps;
            case 1 -> SushiConfig.cps = !SushiConfig.cps;
            case 2 -> SushiConfig.keystrokes = !SushiConfig.keystrokes;
            case 3 -> SushiConfig.ping = !SushiConfig.ping;
            default -> SushiConfig.coords = !SushiConfig.coords;
        }
        SushiConfig.save();
    }
}
