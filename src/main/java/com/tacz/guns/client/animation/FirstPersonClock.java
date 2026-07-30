package com.tacz.guns.client.animation;

import cn.sh1rocu.tacz.api.event.RenderTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

/**
 * The clock the first-person animation runs on. It is not the wall clock: it stops advancing
 * while the game is paused, so a gun does not finish reloading behind the pause menu.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu), where this was spread over
 * {@code AnimationClock}, {@code AnimationClocks}, {@code PausedClientAnimationClock} and
 * {@code ClientAnimationClockTicker}. Only the client half was ever reachable from here.
 */
@Environment(EnvType.CLIENT)
public final class FirstPersonClock {
    private static volatile long logicalNanos;
    private static long lastRealNanos;
    private static boolean initialized;

    private FirstPersonClock() {
    }

    public static void onRenderTick(RenderTickEvent event) {
        if (event.phase == RenderTickEvent.Phase.START) {
            update();
        }
    }

    public static void onLoggingOut(ClientPacketListener handler, Minecraft client) {
        reset();
    }

    private static void update() {
        long now = System.nanoTime();
        if (!initialized) {
            initialized = true;
            lastRealNanos = now;
        } else if (Minecraft.getInstance().isPaused()) {
            lastRealNanos = now;
        } else {
            logicalNanos += now - lastRealNanos;
            lastRealNanos = now;
        }
    }

    public static void reset() {
        logicalNanos = 0L;
        lastRealNanos = 0L;
        initialized = false;
    }

    public static long nowNanos() {
        return logicalNanos;
    }

    public static long nowMillis() {
        return logicalNanos / 1_000_000L;
    }

    public static boolean shouldTick() {
        return !Minecraft.getInstance().isPaused();
    }
}
