package com.sushi.core;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class SushiCore implements ClientModInitializer {
    public static final String MOD_ID = "sushi-core";

    private static KeyBinding menuKey;
    private static KeyBinding zoomKey;
    private static KeyBinding waypointKey;
    /** Not bound to a key by default. Lets the player flip the scoreboard from Controls. */
    private static KeyBinding scoreboardKey;
    private static boolean hitboxesSetByUs;

    @Override
    public void onInitializeClient() {
        SushiConfig.load();

        String category = "category.sushi-core";
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sushi-core.menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));
        zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sushi-core.zoom", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, category));
        waypointKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sushi-core.waypoint", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_N, category));
        scoreboardKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.sushi-core.scoreboard", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));

        SushiMinimap.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (menuKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new SushiMenuScreen());
                }
            }
            while (scoreboardKey.wasPressed()) {
                SushiConfig.enabled[SushiConfig.SCOREBOARD] = !SushiConfig.enabled[SushiConfig.SCOREBOARD];
                SushiConfig.save();
            }
            while (waypointKey.wasPressed()) {
                if (client.currentScreen == null && client.player != null) {
                    SushiWaypoints.addHere(client);
                }
            }
            SushiZoom.tick(client.currentScreen == null && zoomKey.isPressed());
            ClickTracker.tick(client);
            FocusFpsLimiter.tick(client);
            SushiMinimap.tick(client);
            SushiHitColor.tick(client);
            tickHitboxes(client);
            if (!SushiConfig.enabled[SushiConfig.MOTION_BLUR]) SushiMotionBlur.release();
        });

        HudRenderCallback.EVENT.register((context, tickCounter) -> {
            SushiCrosshair.render(context);
            SushiHud.render(context);
        });

        WorldRenderEvents.END.register(context -> SushiMotionBlur.onWorldEnd(MinecraftClient.getInstance()));
    }

    /**
     * Turns entity hitboxes on while the module is enabled. When it is switched off, the change is undone
     * once, so the F3+B toggle keeps working afterwards.
     */
    private static void tickHitboxes(MinecraftClient client) {
        if (client.getEntityRenderDispatcher() == null) return;
        boolean want = SushiConfig.enabled[SushiConfig.HITBOX];
        if (want) {
            if (!client.getEntityRenderDispatcher().shouldRenderHitboxes()) {
                client.getEntityRenderDispatcher().setRenderHitboxes(true);
                hitboxesSetByUs = true;
            }
        } else if (hitboxesSetByUs) {
            client.getEntityRenderDispatcher().setRenderHitboxes(false);
            hitboxesSetByUs = false;
        }
    }
}
