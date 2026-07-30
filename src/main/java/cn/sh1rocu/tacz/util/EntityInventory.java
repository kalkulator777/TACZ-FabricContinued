package cn.sh1rocu.tacz.util;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * The item storage a gun searches for ammunition, as a vanilla {@link Container}.
 * <p>
 * This replaces the Forge capability plumbing the fork inherited. Nothing ever asked for a
 * side-specific handler, so one view per entity is enough, and it is cheap enough to build on
 * demand rather than cache on the entity through a mixin.
 */
public final class EntityInventory {
    private static final Container EMPTY = new SimpleContainer(0);

    /**
     * Hands first, then armour — the order the Forge wrapper exposed, which decides
     * which ammunition gets consumed first.
     */
    private static final List<EquipmentSlot> EQUIPMENT_SLOTS = List.of(
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);

    private EntityInventory() {
    }

    public static Container of(LivingEntity entity) {
        if (!entity.isAlive()) {
            return EMPTY;
        }
        if (entity instanceof Player player) {
            return player.getInventory();
        }
        return new EquipmentContainer(entity);
    }

    /**
     * The hands and armour of a non-player entity — what a mob carrying a gun can reach.
     */
    private record EquipmentContainer(LivingEntity entity) implements Container {
        @Override
        public int getContainerSize() {
            return EQUIPMENT_SLOTS.size();
        }

        @Override
        public boolean isEmpty() {
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                if (!entity.getItemBySlot(slot).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return isValidSlot(slot) ? entity.getItemBySlot(EQUIPMENT_SLOTS.get(slot)) : ItemStack.EMPTY;
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            if (!isValidSlot(slot) || amount <= 0) {
                return ItemStack.EMPTY;
            }
            EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS.get(slot);
            ItemStack existing = entity.getItemBySlot(equipmentSlot);
            if (existing.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = existing.split(amount);
            if (existing.isEmpty()) {
                entity.setItemSlot(equipmentSlot, ItemStack.EMPTY);
            }
            return removed;
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            if (!isValidSlot(slot)) {
                return ItemStack.EMPTY;
            }
            EquipmentSlot equipmentSlot = EQUIPMENT_SLOTS.get(slot);
            ItemStack existing = entity.getItemBySlot(equipmentSlot);
            entity.setItemSlot(equipmentSlot, ItemStack.EMPTY);
            return existing;
        }

        @Override
        public void setItem(int slot, @NotNull ItemStack stack) {
            if (isValidSlot(slot)) {
                entity.setItemSlot(EQUIPMENT_SLOTS.get(slot), stack);
            }
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return entity.isAlive();
        }

        @Override
        public void clearContent() {
            for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
                entity.setItemSlot(slot, ItemStack.EMPTY);
            }
        }

        private boolean isValidSlot(int slot) {
            return slot >= 0 && slot < EQUIPMENT_SLOTS.size();
        }
    }
}
