package com.sushi.core.mixin;

import com.sushi.core.SushiConfig;
import com.sushi.core.SushiTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps the vanilla title screen for the Sushi title screen when the option is on. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen sushiSwapTitleScreen(Screen screen) {
        if (SushiConfig.customTitle && screen != null && screen.getClass() == TitleScreen.class) {
            return new SushiTitleScreen();
        }
        return screen;
    }
}
