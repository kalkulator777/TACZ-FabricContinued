package com.tacz.guns.client.model.functional;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.GunMod;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.LaserConfig;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.util.LaserColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;

public class BeamRenderer {
    public static final Identifier LASER_BEAM_TEXTURE = Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "textures/entity/beam.png");
    private static final LaserConfig DEFAULT_LASER_CONFIG = new LaserConfig();

    public static void renderLaserBeam(ItemStack stack, PoseStack poseStack, ItemDisplayContext transformType, @Nonnull List<BedrockPart> path) {
        if (stack == null || !transformType.firstPerson() && !(transformType == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer builder = bufferSource.getBuffer(LaserBeamRenderState.getLaserBeam());
        poseStack.pushPose();
        {
            for (int i = 0; i < path.size(); ++i) {
                path.get(i).translateAndRotateAndScale(poseStack);
            }

            LaserConfig laserConfig = getLaserConfig(stack);

            int color = LaserColorUtil.getLaserColor(stack, laserConfig);
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;

            stringVertex(transformType.firstPerson() ? -laserConfig.getLength() : -laserConfig.getLengthThird(),
                    transformType.firstPerson() ? laserConfig.getWidth() : laserConfig.getWidthThird(),
                    builder, poseStack.last(), r, g, b, RenderConfig.ENABLE_LASER_FADE_OUT.get());
        }
        poseStack.popPose();
    }

    private static LaserConfig getLaserConfig(ItemStack stack) {
        if (stack == null) {
            return DEFAULT_LASER_CONFIG;
        }

        if (stack.getItem() instanceof IAttachment iAttachment) {
            return TimelessAPI.getClientAttachmentIndex(iAttachment.getAttachmentId(stack))
                    .map(ClientAttachmentIndex::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        if (stack.getItem() instanceof IGun) {
            return TimelessAPI.getGunDisplay(stack)
                    .map(GunDisplayInstance::getLaserConfig)
                    .orElse(DEFAULT_LASER_CONFIG);
        }

        return DEFAULT_LASER_CONFIG;
    }

    private static void stringVertex(float z, float width, VertexConsumer pConsumer, PoseStack.Pose pPose, int r, int g, int b, boolean fadeOut) {
        float halfWidth = width / 2;
        int endAlpha = fadeOut ? 0 : 255;
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 0);
        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1);
        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1);
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0);

        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 0);
        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1);
        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1);
        pConsumer.addVertex(pPose.pose(), -halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0);

        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, 0).setColor(r, g, b, 255).setUv(0, 0);
        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1);
        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1);
        pConsumer.addVertex(pPose.pose(), halfWidth, halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0);

        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1);
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, 0).setColor(r, g, b, 255).setUv(0, 1);
        pConsumer.addVertex(pPose.pose(), -halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 1);
        pConsumer.addVertex(pPose.pose(), halfWidth, -halfWidth, z).setColor(r, g, b, endAlpha).setUv(1, 0);
    }

    public static class LaserBeamRenderState {
        /**
         * 旧代码是一串 RenderStateShard 拼出来的 CompositeState，1.21.5 之后这些状态搬进了
         * RenderPipeline，RenderType 只剩管线 + 材质 + 几个附加项。逐条对应：
         * <ul>
         *     <li>LIGHTNING_ADDITIVE_TRANSPARENCY (SRC_ALPHA, ONE / ONE, ZERO) 就是 BlendFunction.OVERLAY</li>
         *     <li>NO_CULL、COLOR_DEPTH_WRITE 是管线属性</li>
         *     <li>VIEW_OFFSET_Z_LAYERING、ITEM_ENTITY_TARGET 留在 RenderSetup 上</li>
         * </ul>
         * 着色器换成了 position_tex_color：原来那支 position_color_tex_lightmap 已经没有了，
         * 而光照贴图这里本来就写死满亮度（pack(15, 15)），去掉它对画面没有影响。
         */
        private static final RenderPipeline LASER_BEAM_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/laser_beam"))
                .withVertexShader("core/position_tex_color")
                .withFragmentShader("core/position_tex_color")
                .withSampler("Sampler0")
                .withBlend(BlendFunction.OVERLAY)
                .withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                .build();

        private static final RenderType LASER_BEAM = RenderType.create("laser_beam", RenderSetup.builder(LASER_BEAM_PIPELINE)
                .withTexture("Sampler0", LASER_BEAM_TEXTURE)
                .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                .bufferSize(256)
                .sortOnUpload()
                .createRenderSetup());

        public static RenderType getLaserBeam() {
            return LASER_BEAM;
        }
    }
}
