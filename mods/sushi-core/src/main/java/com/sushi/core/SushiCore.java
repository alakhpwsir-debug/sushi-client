package com.sushi.core;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SushiCore implements ClientModInitializer {
    public static final String MOD_ID = "sushi-core";

    private static KeyBinding toggleHud;
    private static boolean hudVisible = true;

    @Override
    public void onInitializeClient() {
        toggleHud = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sushi-core.toggle_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                "category.sushi-core"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleHud.wasPressed()) {
                hudVisible = !hudVisible;
            }
            ClickTracker.tick(client);
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            if (hudVisible) {
                SushiHud.render(context);
            }
        });
    }
}
