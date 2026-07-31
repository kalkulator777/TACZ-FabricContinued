package cn.sh1rocu.tacz.mixin.client;

import com.mojang.blaze3d.opengl.DirectStateAccess;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.textures.GpuTexture;
import com.tacz.guns.client.gl.StencilSupport;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版建 FBO 时只挂 GL_COLOR_ATTACHMENT0 和 GL_DEPTH_ATTACHMENT。
 * 深度纹理被换成 DEPTH32F_STENCIL8 之后，模板面得再单独挂一次才能用。
 * <p>
 * 这里挂在 getFbo 而不是 createFbo 上，是有原因的。FBO 缓存在颜色纹理对象上，键是深度纹理的
 * <em>GL id</em>；而 GL 会回收被删掉的 id。开启模板时深度纹理被重建，新纹理往往拿到和旧的
 * 一样的 id，于是 getFbo 直接命中缓存、返回那个早就建好的 FBO，createFbo 一次也不会再被调用。
 * 原版不受影响 —— FBO 的附件记的就是纹理 id，重建之后自动指向新纹理 —— 但我们的模板附件是在
 * createFbo 里补的，那一趟没跑，附件也就没有。诊断里看到的就是这个：模板挂在了 25 号 FBO 上，
 * 而实际在画的是缓存下来的 3 号。
 * <p>
 * 改成每次取 FBO 时检查，第一次见到的 FBO 补挂一次，之后靠 {@link StencilSupport} 里的集合跳过。
 */
@Mixin(GlTexture.class)
public class GlTextureMixin {
    @Inject(method = "getFbo", at = @At("RETURN"))
    private void tacz$attachStencil(DirectStateAccess directStateAccess, @Nullable GpuTexture depthTexture,
                                    CallbackInfoReturnable<Integer> cir) {
        if (depthTexture instanceof GlTexture depth && StencilSupport.isStencilTexture(depth.glId())) {
            StencilSupport.ensureStencilAttached(cir.getReturnValueI(), depth.glId());
        }
    }
}
