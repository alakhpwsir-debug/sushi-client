package com.sushi.core;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.MapColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Full minimap. Every chunk you load has its top-down colours stored, so the map keeps the land you
 * have already been to. The part around the player is drawn into a small texture every few ticks.
 */
public final class SushiMinimap {
    /** Largest minimap the texture can hold. Smaller sizes use the top-left corner of it. */
    private static final int TEX = 160;
    /** About 6000 chunks (96 by 96 blocks each, roughly 6 MB of colours). Older chunks are dropped first. */
    private static final int MAX_CHUNKS = 6000;

    private static final Map<String, int[]> CHUNKS = Collections.synchronizedMap(new LinkedHashMap<String, int[]>(512, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, int[]> eldest) {
            return size() > MAX_CHUNKS;
        }
    });

    private static NativeImageBackedTexture texture;
    private static Identifier textureId;

    private SushiMinimap() {}

    public static void init() {
        ClientChunkEvents.CHUNK_LOAD.register(SushiMinimap::onChunkLoad);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
    }

    public static void clear() {
        CHUNKS.clear();
    }

    private static String key(String dim, int cx, int cz) {
        return dim + "|" + cx + "|" + cz;
    }

    private static void onChunkLoad(ClientWorld world, WorldChunk chunk) {
        String dim = SushiWaypoints.dimension(world);
        int cx = chunk.getPos().x;
        int cz = chunk.getPos().z;
        int[] colors = new int[256];
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                int wx = cx * 16 + x;
                int wz = cz * 16 + z;
                int top = world.getTopY(Heightmap.Type.WORLD_SURFACE, wx, wz);
                pos.set(wx, top - 1, wz);
                BlockState state = world.getBlockState(pos);
                MapColor mc = state.getMapColor(world, pos);
                colors[z * 16 + x] = mc == MapColor.CLEAR ? 0 : 0xFF000000 | (mc.color & 0xFFFFFF);
            }
        }
        CHUNKS.put(key(dim, cx, cz), colors);
    }

    /** Called from the client tick. Refreshes the minimap texture while it is on. */
    public static void tick(MinecraftClient client) {
        if (!SushiConfig.enabled[SushiConfig.MINIMAP] || client.world == null || client.player == null) return;
        if (client.currentScreen != null) return;
        if (client.getTextureManager() == null) return;
        if (texture == null) {
            texture = new NativeImageBackedTexture(TEX, TEX, false);
            textureId = client.getTextureManager().registerDynamicTexture("sushi_minimap", texture);
        }
        paint(client);
    }

    /** Draws the minimap at the given size into the texture. Outside the saved map the pixels are clear. */
    private static void paint(MinecraftClient client) {
        NativeImage img = texture.getImage();
        if (img == null) return;
        int size = SushiConfig.minimapSize;
        int half = size / 2;
        String dim = SushiWaypoints.dimension(client.world);
        int px = (int) Math.floor(client.player.getX());
        int pz = (int) Math.floor(client.player.getZ());
        int lastCx = Integer.MIN_VALUE;
        int lastCz = Integer.MIN_VALUE;
        int[] chunk = null;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int wx = px - half + x;
                int wz = pz - half + y;
                int cx = Math.floorDiv(wx, 16);
                int cz = Math.floorDiv(wz, 16);
                if (cx != lastCx || cz != lastCz) {
                    lastCx = cx;
                    lastCz = cz;
                    chunk = CHUNKS.get(key(dim, cx, cz));
                }
                int colour = chunk == null ? 0 : chunk[Math.floorMod(wz, 16) * 16 + Math.floorMod(wx, 16)];
                img.setColor(x, y, toAbgr(colour));
            }
        }
        // The player is always in the middle.
        for (int y = half - 1; y <= half + 1; y++) {
            for (int x = half - 1; x <= half + 1; x++) {
                img.setColor(x, y, toAbgr(SushiTheme.CYAN));
            }
        }
        texture.upload();
    }

    /** Converts ARGB (the layout used by the rest of the mod) to ABGR (the layout NativeImage uses). */
    private static int toAbgr(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /** Draws the minimap texture into a box whose top-left corner is (x, y), in box coordinates. */
    static void draw(net.minecraft.client.gui.DrawContext ctx, int x, int y, int size) {
        if (textureId == null) return;
        ctx.drawTexture(textureId, x, y, size, size, 0f, 0f, size, size, TEX, TEX);
    }
}
