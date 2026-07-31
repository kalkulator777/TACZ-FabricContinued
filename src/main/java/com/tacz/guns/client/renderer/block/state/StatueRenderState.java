package com.tacz.guns.client.renderer.block.state;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;

/**
 * 雕像方块每帧渲染需要的东西。1.21.9 起方块实体渲染器不再直接画，而是先把要画的内容
 * 摘进这样一份状态里，再统一提交。
 */
public class StatueRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public final ItemStackRenderState gunItem = new ItemStackRenderState();
    /**
     * 展示的枪上下浮动的偏移。在摘取阶段算好，这样同一帧里它是一个固定值。
     */
    public double bobOffset;
}
