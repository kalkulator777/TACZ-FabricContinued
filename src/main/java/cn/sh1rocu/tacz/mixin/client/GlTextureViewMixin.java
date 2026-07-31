package cn.sh1rocu.tacz.mixin.client;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.textures.GpuTexture;
import com.tacz.guns.client.gl.StencilSupport;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 渲染通道要的 FBO 是从这里拿的，不是 {@link GlTexture}。两个类各有一份一模一样的
 * getFbo / createFbo / fboCache —— {@code GlCommandEncoder.createRenderPass} 调的是
 * GlTextureView 那份，GlTexture 那份只服务于 blit 和拷贝。
 * <p>
 * 模板附件一直挂在 GlTexture 建出来的那个 FBO 上，而画面走的是 GlTextureView 建出来的另一个，
 * 于是诊断里永远是「模板挂到了 25 号，实际在画 3 号，3 号上没有模板附件」。
 */
@Mixin(GlTextureView.class)
public class GlTextureViewMixin {
    @Inject(method = "getFbo", at = @At("RETURN"))
    private void tacz$attachStencil(DirectStateAccess directStateAccess, @Nullable GpuTexture depthTexture,
                                    CallbackInfoReturnable<Integer> cir) {
        if (depthTexture instanceof GlTexture depth && StencilSupport.isStencilTexture(depth.glId())) {
            StencilSupport.ensureStencilAttached(cir.getReturnValueI(), depth.glId());
        }
    }
}
