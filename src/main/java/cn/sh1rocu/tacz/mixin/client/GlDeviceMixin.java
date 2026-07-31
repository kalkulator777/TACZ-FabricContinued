package cn.sh1rocu.tacz.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlDevice;
import com.tacz.guns.client.gl.StencilSupport;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.ByteBuffer;

/**
 * 把 RenderTargetMixin 标记出来的那张深度纹理，从 GL_DEPTH_COMPONENT32 换成 GL_DEPTH32F_STENCIL8。
 * 深度精度不变，多出来的 8 位就是瞄具遮罩要用的模板缓冲。
 */
@Mixin(GlDevice.class)
public class GlDeviceMixin {
    /** GL_DEPTH_COMPONENT32，即 GlConst 给 TextureFormat.DEPTH32 的内部格式。 */
    @org.spongepowered.asm.mixin.Unique
    private static final int TACZ$GL_DEPTH_COMPONENT32 = 0x81A7;

    @WrapOperation(method = "createTexture(Ljava/lang/String;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;",
            at = @At(remap = false, value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/opengl/GlStateManager;_texImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"))
    private void tacz$packDepthStencil(int target, int level, int internalFormat, int width, int height, int border,
                                       int format, int type, @Nullable ByteBuffer pixels, Operation<Void> original) {
        if (StencilSupport.isAllocatingPackedDepthStencil() && internalFormat == TACZ$GL_DEPTH_COMPONENT32) {
            original.call(target, level, StencilSupport.GL_DEPTH32F_STENCIL8, width, height, border,
                    StencilSupport.GL_DEPTH_STENCIL, StencilSupport.GL_FLOAT_32_UNSIGNED_INT_24_8_REV, pixels);
        } else {
            original.call(target, level, internalFormat, width, height, border, format, type, pixels);
        }
    }
}
