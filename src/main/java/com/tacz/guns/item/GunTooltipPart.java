package com.tacz.guns.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Unit;
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
     * 1.21.5 把 HIDE_TOOLTIP 和 HIDE_ADDITIONAL_TOOLTIP 两个组件合成了一个 TOOLTIP_DISPLAY：
     * 整条提示的开关加上一份要隐藏的组件清单。第一位还是「整条不显示」，
     * 第二位原来是「不显示物品自己追加的那部分」，现在用隐藏 CUSTOM_DATA 表达。
     */
    public static void setHideFlags(ItemStack stack, int mask) {
        TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        if ((mask & 1) == 1) {
            display = new TooltipDisplay(true, display.hiddenComponents());
        }
        if ((mask & 2) == 2) {
            display = display.withHidden(DataComponents.CUSTOM_DATA, true);
        }
        stack.set(DataComponents.TOOLTIP_DISPLAY, display);
    }
}
