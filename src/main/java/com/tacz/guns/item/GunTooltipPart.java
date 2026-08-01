package com.tacz.guns.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public enum GunTooltipPart {
    DESCRIPTION,
    AMMO_INFO,
    BASE_INFO,
    EXTRA_DAMAGE_INFO,
    UPGRADES_TIP,
    PACK_INFO;

    private final int mask = 1 << this.ordinal();

    public int getMask() {
        return this.mask;
    }

    public static int getHideFlags(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.getIntOr("HideFlags", /*stack.getItem().getDefaultTooltipHideFlags(stack)*/ 0);
    }

    /**
     * 这里的 mask 是本枚举的位掩码 —— 一位对应一段 TACZ 自己的提示（描述、弹药、基础属性……），
     * 和原版 HIDE_TOOLTIP / HIDE_ADDITIONAL_TOOLTIP 的位不是一回事。
     * <p>
     * 之前这里把它当成原版的位来解释，映射到 1.21.5 新的 TOOLTIP_DISPLAY 上：第 0 位当成
     * 「整条提示不显示」，第 1 位当成「隐藏 CUSTOM_DATA」。于是写进去的和 {@link #getHideFlags}
     * 读出来的根本不是一个地方 —— 读的是 CUSTOM_DATA 里的 HideFlags，而这个方法从不写它。
     * 结果是 {@code /tacz hide_tooltip_part} 对所有分段都无效，唯独 mask 1 会把整条提示抹掉，
     * mask 2 会隐藏一个毫不相干的组件，而命令还照样报告改了多少把枪。
     * 写回读的那个地方就对了。
     */
    public static void setHideFlags(ItemStack stack, int mask) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("HideFlags", mask));
    }
}
