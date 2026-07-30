package com.tacz.guns.api.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by an item renderer that draws itself in first person.
 * {@link com.tacz.guns.client.event.FirstPersonRenderHandler} looks the renderer up in
 * {@code BuiltinItemRendererRegistry} and, if it is one of these, hands first-person rendering
 * over to it instead of letting vanilla draw the item.
 * <p>
 * Absorbed from SimpleBedrockModel (LGPL-3.0, Sh1roCu) — see the roadmap for why. The
 * {@code updateParticleEmitterTransforms} hook went with the library's first-person particle
 * system, which nothing here fed.
 */
@Environment(EnvType.CLIENT)
public interface IFPGeoItemRenderer {
    /**
     * Whether a change from one stack to the other should count as the same item. Returning true
     * keeps the animation running instead of playing put-away and draw.
     */
    default boolean isSameItem(ItemStack oldStack, ItemStack newStack) {
        return ItemStack.isSameItem(oldStack, newStack);
    }

    @Nullable
    default IFPAnimationInstance createAnimationInstance(ItemStack stack, Entity entity) {
        return null;
    }

    /**
     * How long, in milliseconds, this item's put-away animation runs. The handler holds the switch
     * open for that long before the next item is drawn.
     */
    default long getPutAwayDuration(ItemStack stack) {
        return 0L;
    }

    default boolean blockOffhandRender() {
        return false;
    }

    void renderFirstPerson(LocalPlayer player, ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                           MultiBufferSource bufferSource, int packedLight, float partialTick);
}
