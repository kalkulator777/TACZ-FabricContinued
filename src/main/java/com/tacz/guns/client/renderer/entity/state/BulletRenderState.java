package com.tacz.guns.client.renderer.entity.state;

import com.tacz.guns.client.model.BedrockAmmoModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * 一发子弹这一帧要画的东西。
 * <p>
 * 以前这些全是渲染时现从实体上读的 —— 位置、速度、射击者、摄像机。1.21.9 起渲染分成摘取和
 * 提交两步，实体只在摘取阶段能碰，所以都先算好放在这里。
 */
public class BulletRenderState extends EntityRenderState {
    /**
     * 子弹自己的朝向，已按帧内进度插值。
     */
    public float yRot;
    public float xRot;

    /**
     * 弹头模型。枪包里没有配就是 null，那样只画曳光。
     */
    public @Nullable BedrockAmmoModel ammoModel;
    public @Nullable RenderType ammoRenderType;

    /**
     * 曳光。{@link #tracer} 为 false 时下面的都不用看。
     */
    public boolean tracer;
    public float[] tracerColor = {1, 1, 1};
    public @Nullable RenderType tracerRenderType;
    public boolean firstPerson;
    /**
     * 第一人称下要不要做那两次摄像机旋转 —— 只有开着光影时才需要。
     */
    public boolean compensateShaderPack;
    public float cameraXRot;
    public float cameraYRot;
    public @Nullable Vector3f muzzleOffset;
    public double offsetReducer;
    public float tracerWidth;
    public double trailLength;
}
