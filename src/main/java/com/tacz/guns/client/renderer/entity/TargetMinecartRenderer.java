package com.tacz.guns.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.renderer.entity.state.TargetMinecartRenderState;
import com.tacz.guns.client.resource.InternalAssetLoader;
import com.tacz.guns.entity.TargetMinecart;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.AbstractMinecartRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

@Environment(EnvType.CLIENT)
public class TargetMinecartRenderer extends AbstractMinecartRenderer<TargetMinecart, TargetMinecartRenderState> {
    private static final String HEAD_NAME = "head";
    private static final String HEAD_2_NAME = "head2";

    public TargetMinecartRenderer(EntityRendererProvider.Context ctx) {
        /* MinecartRenderer 不再是泛型的，它自己就是 AbstractMinecart 那一份实现，
         * 所以这里直接继承抽象基类。模型层还是 TNT 矿车那个 —— 车厢是原版的，车上放的才是靶子。*/
        super(ctx, ModelLayers.TNT_MINECART);
        this.shadowRadius = 0.25F;
    }

    public static Optional<BedrockModel> getModel() {
        return InternalAssetLoader.getBedrockModel(InternalAssetLoader.TARGET_MINECART_MODEL_LOCATION);
    }

    @Override
    public TargetMinecartRenderState createRenderState() {
        return new TargetMinecartRenderState();
    }

    @Override
    public void extractRenderState(TargetMinecart minecart, TargetMinecartRenderState state, float partialTick) {
        super.extractRenderState(minecart, state, partialTick);
        // 和靶子方块一样：档案不再自己解析，客户端皮肤缓存给出可以直接用的渲染类型
        ResolvableProfile profile = minecart.getGameProfile();
        state.skullRenderType = profile == null ? null
                : Minecraft.getInstance().playerSkinRenderCache().getOrDefault(profile).renderType();
    }

    @Override
    protected void submitMinecartContents(TargetMinecartRenderState state, BlockState displayBlockState,
                                          PoseStack poseStack, SubmitNodeCollector collector, int light) {
        getModel().ifPresent(model -> {
            BedrockPart headModel = model.getNode(HEAD_NAME);
            BedrockPart head2Model = model.getNode(HEAD_2_NAME);
            RenderType skullRenderType = state.skullRenderType;

            poseStack.pushPose();
            poseStack.translate(0.5, 1.875, 0.5);
            poseStack.scale(1.5f, 1.5f, 1.5f);
            poseStack.mulPose(Axis.ZN.rotationDegrees(180));
            poseStack.mulPose(Axis.YN.rotationDegrees(90));

            RenderType renderType = RenderTypes.entityTranslucent(InternalAssetLoader.TARGET_MINECART_TEXTURE_LOCATION);
            /* 模型是所有靶车共用的，几何回调又是延后跑的，所以两颗头的可见性要在画之前设。
             * 详见 BedrockModel#render 的说明。*/
            model.render(poseStack, ItemDisplayContext.NONE, collector, renderType, light, OverlayTexture.NO_OVERLAY,
                    () -> {
                        headModel.visible = false;
                        head2Model.visible = false;
                    });

            if (skullRenderType != null) {
                poseStack.translate(0, 1, -4.5 / 16d);
                submitHead(headModel, poseStack, collector, skullRenderType, light);
                poseStack.translate(0, 0, 0.01);
                submitHead(head2Model, poseStack, collector, skullRenderType, light);
            }
            poseStack.popPose();
        });
    }

    private static void submitHead(BedrockPart head, PoseStack poseStack, SubmitNodeCollector collector,
                                   RenderType renderType, int light) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, builder) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);
            head.visible = true;
            head.render(local, ItemDisplayContext.NONE, builder, light, OverlayTexture.NO_OVERLAY);
        });
    }
}
