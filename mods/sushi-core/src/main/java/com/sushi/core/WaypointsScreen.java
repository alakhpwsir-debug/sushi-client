package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

import static com.sushi.core.SushiTheme.*;

/** Lists every saved waypoint, with add-here, delete and clear-all. */
public class WaypointsScreen extends Screen {
    private static final int ROW_H = 24;
    private static final int MAX_ROWS = 8;

    private final Screen parent;

    WaypointsScreen(Screen parent) {
        super(Text.literal("Waypoints"));
        this.parent = parent;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        clearChildren();
        int cx = width / 2;
        int top = 62;
        List<SushiWaypoints.Waypoint> all = new ArrayList<>(SushiConfig.waypoints);
        int rows = Math.min(MAX_ROWS, all.size());
        for (int r = 0; r < rows; r++) {
            SushiWaypoints.Waypoint w = all.get(r);
            addDrawableChild(ButtonWidget.builder(Text.literal("Delete"), b -> {
                    SushiWaypoints.remove(w);
                    clearAndInit();
                })
                .dimensions(cx + 120, top + r * ROW_H, 60, 18).build());
        }
        int by = height - 56;
        addDrawableChild(ButtonWidget.builder(Text.literal("Add at my position"), b -> {
                if (client != null) SushiWaypoints.addHere(client);
                clearAndInit();
            }).dimensions(cx - 160, by, 150, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Clear all"), b -> {
                SushiWaypoints.clearAll();
                clearAndInit();
            }).dimensions(cx - 5, by, 75, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(cx + 80, by, 80, 20).build());
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0xC8070910);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Waypoints"), width / 2, 20, CYAN);
        List<SushiWaypoints.Waypoint> all = SushiConfig.waypoints;
        if (all.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("No waypoints yet. Press N in game or use the button below."), width / 2, 90, MUTED);
            return;
        }
        int cx = width / 2;
        int top = 62;
        int rows = Math.min(MAX_ROWS, all.size());
        for (int r = 0; r < rows; r++) {
            SushiWaypoints.Waypoint w = all.get(r);
            int y = top + r * ROW_H;
            ctx.fill(cx - 160, y, cx + 110, y + 20, CARD_BG);
            ctx.fill(cx - 160, y, cx - 158, y + 20, CYAN);
            String dim = w.dimension().replace("minecraft:", "");
            ctx.drawText(textRenderer, w.name(), cx - 150, y + 6, TEXT, true);
            ctx.drawText(textRenderer, w.x() + ", " + w.y() + ", " + w.z() + "  " + dim, cx - 60, y + 6, MUTED, false);
        }
        if (all.size() > MAX_ROWS) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("+" + (all.size() - MAX_ROWS) + " more not shown"), width / 2, top + MAX_ROWS * ROW_H + 4, MUTED);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
