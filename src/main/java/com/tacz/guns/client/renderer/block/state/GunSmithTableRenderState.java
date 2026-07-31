package com.tacz.guns.client.renderer.block.state;

import com.tacz.guns.client.model.bedrock.BedrockModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.Nullable;

public class GunSmithTableRenderState extends BlockEntityRenderState {
    /**
     * 模型来自枪包，可能还没加载好，也可能这个方块不是多方块的主方块 —— 两种情况都是 null，
     * 提交阶段直接跳过。
     */
    public @Nullable BedrockModel model;
    public @Nullable RenderType renderType;
    public float rotationDegrees;
}
