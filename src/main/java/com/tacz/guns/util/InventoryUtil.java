package com.tacz.guns.util;

import net.minecraft.world.entity.player.Inventory;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    /**
     * Slot indices that arrive over the network are attacker-controlled. {@link Inventory#getItem(int)}
     * does not range-check them: a negative index throws out of the packet handler and takes the
     * dedicated server down with it. Always gate on this before dereferencing such an index.
     */
    public static boolean isValidSlot(Inventory inventory, int slotIndex) {
        return slotIndex >= 0 && slotIndex < inventory.getContainerSize();
    }
}
