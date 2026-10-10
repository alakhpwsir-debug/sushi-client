package com.sushi.core;

import com.sushi.core.mixin.OverlayTextureAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Changes the colour of the flash an entity shows when it is hit. Vanilla paints that flash into the
 * top rows of the overlay texture, so the same rows are repainted here.
 */
final class SushiHitColor {
    /** Vanilla hurt colour, as NativeImage stores it (ABGR). Red at alpha 0xB2. */
    private static final int VANILLA = 0xB20000FF;
    private static int applied = VANILLA;

    private SushiHitColor() {}

    /** Called once per client tick. Repaints only when the chosen colour changes. */
    static void tick(MinecraftClient client) {
        int want = wanted();
        if (want == applied || client.gameRenderer == null) return;
        OverlayTexture overlay = client.gameRenderer.getOverlayTexture();
        NativeImageBackedTexture texture = ((OverlayTextureAccessor) (Object) overlay).sushi$texture();
        NativeImage image = texture.getImage();
        if (image == null) return;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 8; y++) {
                image.setColor(x, y, want);
            }
        }
        texture.upload();
        applied = want;
    }

    private static int wanted() {
        if (!SushiConfig.enabled[SushiConfig.HIT_COLOR]) return VANILLA;
        int argb = SushiConfig.COLORS[SushiConfig.hitColor];
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (0xB2 << 24) | (b << 16) | (g << 8) | r;
    }
}
