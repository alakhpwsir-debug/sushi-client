package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import static com.sushi.core.SushiTheme.*;

/** Drag HUD elements to where you want them. Positions save when you let go. */
public class HudEditorScreen extends Screen {
    private final Screen parent;
    private SushiHud.Box[] boxes = new SushiHud.Box[SushiHud.COUNT];
    private int dragging = -1;
    private int grabX;
    private int grabY;

    HudEditorScreen(Screen parent) {
        super(Text.literal("HUD layout"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset positions"), b -> {
            for (int i = 0; i < SushiConfig.x.length; i++) {
                SushiConfig.x[i] = -1;
                SushiConfig.y[i] = -1;
            }
            SushiConfig.save();
        }).dimensions(cx - 160, height - 40, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
            .dimensions(cx + 10, height - 40, 150, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x90070910);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("HUD LAYOUT"), width / 2, 18, CYAN);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Drag each element. Positions save when you let go."), width / 2, 32, MUTED);
        boxes = SushiHud.boxes(client, textRenderer, width, height, true);
        SushiHud.draw(ctx, client, boxes);
        for (int i = 0; i < boxes.length; i++) {
            SushiHud.Box b = boxes[i];
            if (b == null) continue;
            outline(ctx, b.x() - 2, b.y() - 2, b.bw() + 4, b.bh() + 4, dragging == i ? CYAN : PURPLE);
        }
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < boxes.length; i++) {
                SushiHud.Box b = boxes[i];
                if (b != null && mouseX >= b.x() && mouseX <= b.x() + b.bw() && mouseY >= b.y() && mouseY <= b.y() + b.bh()) {
                    dragging = i;
                    grabX = (int) mouseX - b.x();
                    grabY = (int) mouseY - b.y();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging >= 0 && boxes[dragging] != null) {
            SushiConfig.x[dragging] = SushiHud.clamp((int) mouseX - grabX, 0, Math.max(0, width - boxes[dragging].bw()));
            SushiConfig.y[dragging] = SushiHud.clamp((int) mouseY - grabY, 0, Math.max(0, height - boxes[dragging].bh()));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging >= 0) {
            dragging = -1;
            SushiConfig.save();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
