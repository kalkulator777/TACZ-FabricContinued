package com.tacz.guns.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.renderer.IDynamicItemRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 把本模组的物品渲染器接到 1.21.4 之后的物品模型体系上。
 * <p>
 * 以前是 {@code BuiltinItemRendererRegistry.INSTANCE.register(item, renderer)}，一句话的事。
 * 现在物品长什么样由 {@code assets/<ns>/items/<id>.json} 决定，自定义渲染要声明成
 * {@code minecraft:special} 再指一个渲染器类型。所以这里注册一个类型 {@code tacz:dynamic}，
 * 它自己不画任何东西，只负责把活转交给物品的 {@link IDynamicItemRenderer}。
 * 谁有渲染器由物品自己说了算，和从前一样。
 */
@Environment(EnvType.CLIENT)
public class TaczSpecialItemRenderer implements SpecialModelRenderer<ItemStack> {
    public static final Identifier TYPE = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "dynamic");

    @Override
    public void submit(@Nullable ItemStack stack, ItemDisplayContext transformType, PoseStack poseStack,
                       SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (stack == null) {
            return;
        }
        IDynamicItemRenderer renderer = IDynamicItemRenderer.of(stack);
        if (renderer != null) {
            renderer.render(stack, transformType, poseStack, collector, light, overlay);
        }
    }

    /**
     * GUI 会拿这些点算包围盒，决定物品要缩到多大。这里的模型是运行时才知道的，
     * 给不出真实范围，所以报一个方块的八个角 —— 和物品模型的默认尺寸一致。
     */
    @Override
    public void getExtents(Consumer<Vector3fc> consumer) {
        for (int i = 0; i < 8; i++) {
            consumer.accept(new Vector3f((i & 1), (i >> 1 & 1), (i >> 2 & 1)));
        }
    }

    /**
     * 返回的东西会被存进渲染状态，下一帧再刷新。这里不复制：渲染器只读它，
     * 而每帧 updateForTopItem 都会重新取一次。
     */
    @Override
    public @Nullable ItemStack extractArgument(ItemStack stack) {
        return stack;
    }

    public static void register() {
        SpecialModelRenderers.ID_MAPPER.put(TYPE, Unbaked.MAP_CODEC);
    }

    @Environment(EnvType.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new TaczSpecialItemRenderer();
        }
    }
}
