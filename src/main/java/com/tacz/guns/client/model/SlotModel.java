package com.tacz.guns.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockCubePerFace;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.pojo.model.FaceUVsItem;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * 物品栏里那张 16x16 的图标片。
 * <p>
 * 原来它继承 EntityModel，但 1.21.2 之后 EntityModel 带上了渲染状态泛型，而这里
 * 一个实体都没有 —— 它从来只是一个四边形。所以干脆不再是 EntityModel。
 */
public class SlotModel {
    private final BedrockPart bone;

    public SlotModel(boolean illuminated) {
        bone = new BedrockPart("slot");
        bone.setPos(8.0F, 24.0F, -10.0F);
        bone.cubes.add(new BedrockCubePerFace(-16.0F, -16.0F, 9.5F, 16.0F, 16.0F, 0, 0, 16, 16, FaceUVsItem.singleSouthFace()));
        bone.illuminated = illuminated;
    }

    public SlotModel() {
        this(false);
    }

    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        bone.render(poseStack, ItemDisplayContext.GUI, buffer, packedLight, packedOverlay);
    }

    /**
     * 走收集器的那条路。几何体是延后画的，所以这里只能用回调给的那份 pose ——
     * 提交之后调用方还会继续动它自己的 PoseStack。
     */
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, int packedLight, int packedOverlay) {
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);
            bone.render(local, ItemDisplayContext.GUI, buffer, packedLight, packedOverlay);
        });
    }
}
