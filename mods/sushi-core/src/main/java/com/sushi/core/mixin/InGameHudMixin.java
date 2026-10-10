package com.sushi.core.mixin;

import com.sushi.core.SushiConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla crosshair and sidebar when the Sushi versions replace them. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void sushi$replaceCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (SushiConfig.enabled[SushiConfig.CROSSHAIR]) {
            ci.cancel();
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
            at = @At("HEAD"), cancellable = true)
    private void sushi$scoreboardToggle(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!SushiConfig.enabled[SushiConfig.SCOREBOARD]) {
            ci.cancel();
        }
    }
}
