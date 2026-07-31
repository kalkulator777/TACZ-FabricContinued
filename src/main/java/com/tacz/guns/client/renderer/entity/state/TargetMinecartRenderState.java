package com.tacz.guns.client.renderer.entity.state;

import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.Nullable;

/**
 * 靶车比普通矿车多的那点东西：车上那颗头用谁的皮肤。没有主人时为 null。
 */
public class TargetMinecartRenderState extends MinecartRenderState {
    public @Nullable RenderType skullRenderType;
}
