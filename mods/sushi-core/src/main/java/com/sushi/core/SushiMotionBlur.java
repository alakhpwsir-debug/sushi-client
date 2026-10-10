package com.sushi.core;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

/**
 * Frame-blend motion blur. After the world is drawn, the last blended frame is laid over the new one
 * with a little opacity, then the result is saved as the next "last frame". When the camera moves,
 * the picture smears; when it stands still the image stays sharp.
 */
public final class SushiMotionBlur {
    private static int texture = -1;
    private static int texW;
    private static int texH;
    private static boolean hasHistory;

    private SushiMotionBlur() {}

    /** Runs after the world is drawn and before the HUD. */
    public static void onWorldEnd(MinecraftClient client) {
        if (!SushiConfig.enabled[SushiConfig.MOTION_BLUR] || client.currentScreen != null) {
            hasHistory = false;
            return;
        }
        Framebuffer fb = client.getFramebuffer();
        int w = fb.textureWidth;
        int h = fb.textureHeight;
        if (w <= 0 || h <= 0) return;
        ensureTexture(w, h);
        if (hasHistory) {
            blendHistory(w, h, SushiConfig.motionBlur / 100f);
        }
        GlStateManager._bindTexture(texture);
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
        hasHistory = true;
    }

    /** Frees the GL texture when the game closes or the setting is turned off. */
    public static void release() {
        if (texture != -1) {
            GL11.glDeleteTextures(texture);
            texture = -1;
        }
        hasHistory = false;
    }

    private static void ensureTexture(int w, int h) {
        if (texture != -1 && w == texW && h == texH) return;
        if (texture != -1) GL11.glDeleteTextures(texture);
        texture = GL11.glGenTextures();
        texW = w;
        texH = h;
        GlStateManager._bindTexture(texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
        hasHistory = false;
    }

    /** Draws the saved frame over the screen at the given opacity (0 to 1). */
    private static void blendHistory(int w, int h, float alpha) {
        Matrix4f oldProjection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorter oldSorting = RenderSystem.getVertexSorting();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0f, w, h, 0f, -1000f, 1000f), VertexSorter.BY_DISTANCE);

        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(1f, 1f, 1f, alpha);

        // The saved frame is stored bottom-up, so the top of the screen uses the top of the texture (v = 1).
        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buf.vertex(0f, 0f, 0f).texture(0f, 1f).color(255, 255, 255, 255);
        buf.vertex(0f, h, 0f).texture(0f, 0f).color(255, 255, 255, 255);
        buf.vertex(w, h, 0f).texture(1f, 0f).color(255, 255, 255, 255);
        buf.vertex(w, 0f, 0f).texture(1f, 1f).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setProjectionMatrix(oldProjection, oldSorting);
        modelView.popMatrix();
    }
}
