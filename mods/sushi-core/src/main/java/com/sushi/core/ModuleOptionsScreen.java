package com.sushi.core;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.Supplier;

import static com.sushi.core.SushiTheme.*;

/** Options for one module: scale and background for HUD modules, plus the module's own settings. */
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
        int y = Math.max(50, height / 2 - 80);
        if (SushiConfig.hasBox(idx)) {
            row(cx, y, this::scaleLabel, this::cycleScale);
            row(cx, y + 26, this::bgLabel, () -> {
                SushiConfig.bg[idx] = !SushiConfig.bg[idx];
                SushiConfig.save();
            });
            y += 52;
        }
        switch (idx) {
            case SushiConfig.CPS -> {
                row(cx, y, () -> Text.literal("Show right clicks: " + onOff(SushiConfig.cpsRight)), () -> {
                    SushiConfig.cpsRight = !SushiConfig.cpsRight;
                    SushiConfig.save();
                });
            }
            case SushiConfig.COORDS -> {
                row(cx, y, () -> Text.literal("Show Y coordinate: " + onOff(SushiConfig.coordsY)), () -> {
                    SushiConfig.coordsY = !SushiConfig.coordsY;
                    SushiConfig.save();
                });
            }
            case SushiConfig.ZOOM -> {
                row(cx, y, () -> Text.literal("Zoom: " + SushiConfig.zoomFactor + "x"), this::cycleZoom);
            }
            case SushiConfig.CROSSHAIR -> {
                row(cx, y, () -> Text.literal("Style: " + SushiConfig.CROSSHAIR_STYLES[SushiConfig.crosshairStyle]), () -> {
                    SushiConfig.crosshairStyle = (SushiConfig.crosshairStyle + 1) % SushiConfig.CROSSHAIR_STYLES.length;
                    SushiConfig.save();
                });
                row(cx, y + 26, () -> Text.literal("Colour: " + SushiConfig.COLOR_NAMES[SushiConfig.crosshairColor]), () -> {
                    SushiConfig.crosshairColor = (SushiConfig.crosshairColor + 1) % SushiConfig.COLORS.length;
                    SushiConfig.save();
                });
            }
            case SushiConfig.HIT_COLOR -> {
                row(cx, y, () -> Text.literal("Colour: " + SushiConfig.COLOR_NAMES[SushiConfig.hitColor]), () -> {
                    SushiConfig.hitColor = (SushiConfig.hitColor + 1) % SushiConfig.COLORS.length;
                    SushiConfig.save();
                });
            }
            case SushiConfig.MOTION_BLUR -> {
                row(cx, y, () -> Text.literal("Strength: " + SushiConfig.motionBlur + "%"), () -> {
                    int i = 0;
                    for (int k = 0; k < SushiConfig.MOTION_STEPS.length; k++) {
                        if (SushiConfig.MOTION_STEPS[k] == SushiConfig.motionBlur) i = k;
                    }
                    SushiConfig.motionBlur = SushiConfig.MOTION_STEPS[(i + 1) % SushiConfig.MOTION_STEPS.length];
                    SushiConfig.save();
                });
            }
            case SushiConfig.MINIMAP -> {
                row(cx, y, () -> Text.literal("Size: " + SushiConfig.minimapSize + " px"), () -> {
                    int i = 0;
                    for (int k = 0; k < SushiConfig.MINIMAP_SIZES.length; k++) {
                        if (SushiConfig.MINIMAP_SIZES[k] == SushiConfig.minimapSize) i = k;
                    }
                    SushiConfig.minimapSize = SushiConfig.MINIMAP_SIZES[(i + 1) % SushiConfig.MINIMAP_SIZES.length];
                    SushiConfig.save();
                });
            }
            case SushiConfig.WAYPOINTS -> {
                addDrawableChild(ButtonWidget.builder(Text.literal("Manage waypoints"),
                        b -> client.setScreen(new WaypointsScreen(this)))
                    .dimensions(cx - 110, y, 220, 20).build());
            }
            default -> {
            }
        }
        addDrawableChild(ButtonWidget.builder(Text.literal("Back"), b -> close())
            .dimensions(cx - 60, height - 44, 120, 20).build());
    }

    /** Adds a button that shows a label and runs an action when clicked. The label is refreshed after each click. */
    private void row(int cx, int y, Supplier<Text> label, Runnable action) {
        addDrawableChild(ButtonWidget.builder(label.get(), b -> {
            action.run();
            b.setMessage(label.get());
        }).dimensions(cx - 110, y, 220, 20).build());
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

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
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
        return Text.literal("Background: " + onOff(SushiConfig.bg[idx]));
    }

    private void cycleZoom() {
        int[] steps = SushiConfig.ZOOM_FACTORS;
        int i = 0;
        for (int k = 0; k < steps.length; k++) {
            if (steps[k] == SushiConfig.zoomFactor) i = k;
        }
        SushiConfig.zoomFactor = steps[(i + 1) % steps.length];
        SushiConfig.save();
    }
}
