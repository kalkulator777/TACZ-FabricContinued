package cn.sh1rocu.tacz.mixin.client;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.tacz.guns.client.gl.StencilSupport;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版建 FBO 时只挂 GL_COLOR_ATTACHMENT0 和 GL_DEPTH_ATTACHMENT。
 * 深度纹理被换成 DEPTH32F_STENCIL8 之后，模板面得再单独挂一次才能用。
 */
@Mixin(GlTexture.class)
public class GlTextureMixin {
    @Inject(method = "createFbo", at = @At("RETURN"))
    private void tacz$attachStencil(DirectStateAccess directStateAccess, int depthTextureId,
                                    CallbackInfoReturnable<Integer> cir) {
        if (StencilSupport.isStencilTexture(depthTextureId)) {
            StencilSupport.attachStencilTo(cir.getReturnValueI(), depthTextureId);
        }
    }
}
