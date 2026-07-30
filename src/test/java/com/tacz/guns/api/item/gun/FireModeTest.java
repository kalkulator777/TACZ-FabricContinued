package com.tacz.guns.api.item.gun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The fire mode is read straight out of item NBT, which can hold anything — an old save, a
 * {@code /data} command, another mod. It is read from the tooltip, the firing path and the tick
 * loop, so resolving it must not throw.
 */
class FireModeTest {
    @Test
    void everyModeResolvesFromItsOwnName() {
        for (FireMode mode : FireMode.values()) {
            assertEquals(mode, FireMode.fromName(mode.name()));
        }
    }

    @Test
    void anUnrecognisedNameReadsAsUnknown() {
        assertEquals(FireMode.UNKNOWN, FireMode.fromName("SAFETY"));
        assertEquals(FireMode.UNKNOWN, FireMode.fromName(""));
        assertEquals(FireMode.UNKNOWN, FireMode.fromName(null));
    }

    @Test
    void resolutionIsCaseSensitiveLikeTheTagItReads() {
        assertEquals(FireMode.UNKNOWN, FireMode.fromName("semi"));
    }
}
