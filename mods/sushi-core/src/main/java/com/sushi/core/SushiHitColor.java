package com.sushi.core;

import com.sushi.core.mixin.OverlayTextureAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Changes the colour of the flash an entity shows when it is hit. Vanilla paints that flash into the
 * top half of the overlay texture (rows 0 to 7), so those rows are repainted here.
 *
 * The check runs every tick rather than only when a setting changes, so the colour is put back if
 * the overlay texture is ever recreated (for example after a resource reload).
 */
final class SushiHitColor {
    /** Vanilla hurt colour, as NativeImage stores it (ABGR): red at alpha 0xB2. */
    private static final int VANILLA = 0xB20000FF;

    /** The texture and colour we last painted. Used to skip the upload when nothing changed. */
    private static NativeImageBackedTexture paintedTexture;
    private static int paintedColor = VANILLA;

    private SushiHitColor() {}

    /** Called once per client tick. */
    static void tick(MinecraftClient client) {
        if (client.gameRenderer == null) return;
        OverlayTexture overlay = client.gameRenderer.getOverlayTexture();
        NativeImageBackedTexture texture = ((OverlayTextureAccessor) (Object) overlay).sushi$texture();
        int want = wanted();
        if (texture == paintedTexture && want == paintedColor) return;

        NativeImage image = texture.getImage();
        if (image == null) return;
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 8; y++) {
                image.setColor(x, y, want);
            }
        }
        texture.upload();
        paintedTexture = texture;
        paintedColor = want;
    }

    /** The colour the hurt flash should use right now, in NativeImage (ABGR) layout. */
    private static int wanted() {
        if (!SushiConfig.enabled[SushiConfig.HIT_COLOR]) return VANILLA;
        int argb = SushiConfig.COLORS[SushiConfig.hitColor];
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (0xB2 << 24) | (b << 16) | (g << 8) | r;
    }
}
