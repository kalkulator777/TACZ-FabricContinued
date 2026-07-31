package com.tacz.guns.client.gui.pip;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 枪械工作台左边那个转着的成品预览。
 * <p>
 * 以前是直接改 RenderSystem 的 modelview 矩阵，把物品当场画进 GUI。1.21.5 之后 GUI 的
 * 变换是 Matrix3x2f —— 只有二维，转不了三维的东西。原版给这类东西留的口子是
 * picture-in-picture：先把内容画进一张离屏纹理，再把纹理贴到界面上。
 */
@Environment(EnvType.CLIENT)
public record GunSmithTableModelRenderState(
        ItemStack stack,
        float rotation,
        float pitch,
        int x0,
        int y0,
        int x1,
        int y1,
        float scale,
        @Nullable ScreenRectangle scissorArea,
        @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GunSmithTableModelRenderState(ItemStack stack, float rotation, float pitch,
                                         int x0, int y0, int x1, int y1, float scale,
                                         @Nullable ScreenRectangle scissorArea) {
        this(stack, rotation, pitch, x0, y0, x1, y1, scale, scissorArea,
                PictureInPictureRenderState.getBounds(x0, y0, x1, y1, scissorArea));
    }
}
