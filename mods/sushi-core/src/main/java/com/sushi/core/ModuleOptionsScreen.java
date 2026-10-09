package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static com.sushi.core.SushiTheme.*;

/** Options for one module: scale, background, and the module's own toggles. */
public class ModuleOptionsScreen extends Screen {
    private final Screen parent;
    private final int idx;

    ModuleOptionsScreen(Screen parent, int idx) {
        super(Text.literal(SushiConfig.NAMES[idx]));
        this.parent = parent;
        this.idx = idx;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 40;
        addDrawableChild(ButtonWidget.builder(scaleLabel(), b -> {
            cycleScale();
            b.setMessage(scaleLabel());
        }).dimensions(cx - 110, y, 220, 20).build());
        addDrawableChild(ButtonWidget.builder(bgLabel(), b -> {
            SushiConfig.bg[idx] = !SushiConfig.bg[idx];
            SushiConfig.save();
            b.setMessage(bgLabel());
        }).dimensions(cx - 110, y + 26, 220, 20).build());
        if (idx == 1) {
            addDrawableChild(ButtonWidget.builder(rightLabel(), b -> {
                SushiConfig.cpsRight = !SushiConfig.cpsRight;
                SushiConfig.save();
                b.setMessage(rightLabel());
            }).dimensions(cx - 110, y + 52, 220, 20).build());
        }
        if (idx == 4) {
            addDrawableChild(ButtonWidget.builder(yLabel(), b -> {
                SushiConfig.coordsY = !SushiConfig.coordsY;
                SushiConfig.save();
                b.setMessage(yLabel());
            }).dimensions(cx - 110, y + 52, 220, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(cx - 60, height - 44, 120, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8070910);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(SushiConfig.NAMES[idx] + " options"), width / 2, 24, CYAN);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(SushiConfig.DESCS[idx]), width / 2, 38, MUTED);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    private Text scaleLabel() {
        return Text.literal("Scale: " + SushiConfig.scale[idx] + "%");
    }

    private void cycleScale() {
        int[] steps = SushiConfig.SCALES;
        int next = 0;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == SushiConfig.scale[idx]) {
                next = (i + 1) % steps.length;
                break;
            }
        }
        SushiConfig.scale[idx] = steps[next];
        SushiConfig.save();
    }

    private Text bgLabel() {
        return Text.literal("Background: " + (SushiConfig.bg[idx] ? "ON" : "OFF"));
    }

    private Text rightLabel() {
        return Text.literal("Show right clicks: " + (SushiConfig.cpsRight ? "ON" : "OFF"));
    }

    private Text yLabel() {
        return Text.literal("Show Y coordinate: " + (SushiConfig.coordsY ? "ON" : "OFF"));
    }
}
