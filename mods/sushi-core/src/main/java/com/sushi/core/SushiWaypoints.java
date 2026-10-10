package com.sushi.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Saved points you can go back to. They are kept per dimension and saved in the config. */
final class SushiWaypoints {
    record Waypoint(String name, int x, int y, int z, String dimension) {}

    private SushiWaypoints() {}

    static String dimension(ClientWorld world) {
        return world.getRegistryKey().getValue().toString();
    }

    /** Adds a waypoint at the player's position. */
    static void addHere(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;
        String dim = dimension(client.world);
        int n = 1;
        for (Waypoint w : SushiConfig.waypoints) {
            if (w.dimension().equals(dim)) n++;
        }
        SushiConfig.waypoints.add(new Waypoint("Waypoint " + n, player.getBlockX(), player.getBlockY(), player.getBlockZ(), dim));
        SushiConfig.save();
        player.sendMessage(Text.literal("Added " + "Waypoint " + n + " at " + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ()), true);
    }

    /** Waypoints in the dimension the player is in now. */
    static List<Waypoint> here(MinecraftClient client) {
        List<Waypoint> out = new ArrayList<>();
        if (client.world == null) return out;
        String dim = dimension(client.world);
        for (Waypoint w : SushiConfig.waypoints) {
            if (w.dimension().equals(dim)) out.add(w);
        }
        return out;
    }

    static void remove(Waypoint w) {
        SushiConfig.waypoints.remove(w);
        SushiConfig.save();
    }

    static void clearAll() {
        SushiConfig.waypoints.clear();
        SushiConfig.save();
    }

    /** Distance in blocks from the player to the waypoint (horizontal and vertical). */
    static int distance(MinecraftClient client, Waypoint w) {
        PlayerEntity p = client.player;
        if (p == null) return 0;
        double dx = p.getX() - (w.x() + 0.5);
        double dy = p.getY() - w.y();
        double dz = p.getZ() - (w.z() + 0.5);
        return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
    }
}
