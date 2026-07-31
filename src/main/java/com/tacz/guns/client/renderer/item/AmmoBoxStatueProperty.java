package com.tacz.guns.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.tacz.guns.item.AmmoBoxItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 弹药盒的外观编号，注册名 {@code tacz:ammo_statue}。
 * <p>
 * 1.21.4 之前这是 {@code ItemProperties.register} 加模型里的 overrides；两样东西都没了，
 * 现在归 items/ammo_box.json 里的 {@code minecraft:range_dispatch} 管，而 range_dispatch
 * 认的属性来自这个注册表。返回值和旧的 predicate 值一一对应，见
 * {@link AmmoBoxItem#getStatue}，所以模型文件里的编号一个都不用动。
 */
@Environment(EnvType.CLIENT)
public record AmmoBoxStatueProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<AmmoBoxStatueProperty> MAP_CODEC = MapCodec.unit(new AmmoBoxStatueProperty());

    public static void register() {
        RangeSelectItemModelProperties.ID_MAPPER.put(AmmoBoxItem.PROPERTY_NAME, MAP_CODEC);
    }

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return AmmoBoxItem.getStatue(stack);
    }

    @Override
    public MapCodec<AmmoBoxStatueProperty> type() {
        return MAP_CODEC;
    }
}
