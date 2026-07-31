package com.tacz.guns.api.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import cn.sh1rocu.tacz.api.extension.IItem;

/**
 * 本模组自己的「物品有个自定义渲染器」接口，替代 Fabric 的
 * {@code BuiltinItemRendererRegistry.DynamicItemRenderer}。
 * <p>
 * 1.21.4 之后物品模型改由 {@code assets/<ns>/items/*.json} 描述，自定义渲染走
 * {@code minecraft:special} + {@link net.minecraft.client.renderer.special.SpecialModelRenderer}，
 * Fabric 那个注册表随之没了。
 * <p>
 * 但那个注册表在这里还兼着第二个身份：从物品反查渲染器，给动画 tick、镜头 FOV、
 * 第一人称渲染用。那部分本来就不需要注册表 —— {@link IItem#getCustomRenderer()}
 * 直接就有，所以查询改成走 {@link #of(Item)}。
 */
@Environment(EnvType.CLIENT)
public interface IDynamicItemRenderer {
    void render(ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                SubmitNodeCollector collector, int light, int overlay);

    /**
     * 这个物品的自定义渲染器，没有就返回 null。
     */
    @Nullable
    static IDynamicItemRenderer of(Item item) {
        return item instanceof IItem itemEx ? itemEx.getCustomRenderer() : null;
    }

    @Nullable
    static IDynamicItemRenderer of(ItemStack stack) {
        return of(stack.getItem());
    }
}
