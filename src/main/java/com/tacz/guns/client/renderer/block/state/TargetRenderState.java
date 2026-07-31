package com.tacz.guns.client.renderer.block.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

public class TargetRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    /**
     * 靶子被打倒的角度，已经按帧内进度插值过。
     */
    public float tiltDegrees;
    /**
     * 靶子头顶那颗玩家头的渲染类型，来自客户端皮肤缓存；没有主人时为 null。
     */
    public @Nullable RenderType skullRenderType;
}
