package com.sushi.core;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayDeque;
import java.util.Deque;

/** Counts left/right clicks per second by watching the attack / use key state each tick. */
final class ClickTracker {
    private static final Deque<Long> LEFT = new ArrayDeque<>();
    private static final Deque<Long> RIGHT = new ArrayDeque<>();
    private static boolean prevLeft;
    private static boolean prevRight;

    private ClickTracker() {}

    static void tick(MinecraftClient client) {
        if (client.currentScreen != null) {
            prevLeft = false;
            prevRight = false;
            return;
        }
        long now = System.currentTimeMillis();
        boolean left = client.options.attackKey.isPressed();
        boolean right = client.options.useKey.isPressed();
        if (left && !prevLeft) LEFT.addLast(now);
        if (right && !prevRight) RIGHT.addLast(now);
        prevLeft = left;
        prevRight = right;
        trim(LEFT, now);
        trim(RIGHT, now);
    }

    private static void trim(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) {
            clicks.pollFirst();
        }
    }

    static int leftCps() {
        return LEFT.size();
    }

    static int rightCps() {
        return RIGHT.size();
    }
}
