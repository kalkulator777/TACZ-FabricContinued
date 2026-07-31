package com.tacz.guns.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.SlotModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.renderer.block.GunSmithTableRenderer;
import com.tacz.guns.api.client.renderer.IDynamicItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

public class GunSmithTableItemRenderer implements IDynamicItemRenderer {
    private static final SlotModel SLOT_BLOCK_MODEL = new SlotModel();

    public static final Supplier<GunSmithTableItemRenderer> INSTANCE = Suppliers.memoize(GunSmithTableItemRenderer::new);

    @Override
    public void render(@Nonnull ItemStack stack, @Nonnull ItemDisplayContext transformType, @Nonnull PoseStack poseStack, @Nonnull SubmitNodeCollector collector, int pPackedLight, int pPackedOverlay) {
        GunSmithTableRenderer.getIndex(stack).ifPresentOrElse(index -> {
            BedrockModel model = index.getModel();
            Identifier texture = index.getTexture();
            if (model == null) {
                return;
            }
            poseStack.pushPose();

            ItemTransforms transforms = index.getTransforms();
            if (transforms != null) {
                ItemTransform transform = transforms.getTransform(transformType);
                // apply 现在收的是 Pose 而不是整个 PoseStack，而且 NO_TRANSFORM 那支会自己
                // 平移 -0.5 —— 那是从旧调用方折进来的，这里外面已经做过了，所以要跳开
                if (transform != ItemTransform.NO_TRANSFORM) {
                    poseStack.translate(0.5F, 0.5F, 0.5F);
                    transform.apply(false, poseStack.last());
                    poseStack.translate(-0.5F, -0.5F, -0.5F);
                }
            }

            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            RenderType renderType = RenderTypes.entityTranslucent(texture);
            model.render(poseStack, transformType, collector, renderType, pPackedLight, pPackedOverlay);
            poseStack.popPose();
        }, () -> {
            poseStack.translate(0.5, 1.5, 0.5);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            SLOT_BLOCK_MODEL.submit(poseStack, collector, RenderTypes.entityTranslucent(MissingTextureAtlasSprite.getLocation()), pPackedLight, pPackedOverlay);
        });
    }
}
