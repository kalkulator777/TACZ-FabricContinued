package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.mixin.RenderTargetStencil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.tacz.guns.client.gl.StencilSupport;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * 1.21.5 之后 RenderTarget 不再自己拼 FBO，深度缓冲变成了一张 GpuTexture，格式写死是 DEPTH32，
 * 没有模板位。瞄具的模板遮罩需要模板缓冲，所以这里在开启模板时把深度纹理换成 DEPTH32F_STENCIL8。
 * 真正改格式的地方在 GlDeviceMixin，把纹理挂到 GL_STENCIL_ATTACHMENT 上的地方在 GlTextureMixin。
 */
@Mixin(value = RenderTarget.class, priority = 2000)
public abstract class RenderTargetMixin implements RenderTargetStencil {
    @Shadow
    public int width;
    @Shadow
    public int height;
    @Shadow
    protected @Nullable GpuTexture depthTexture;

    @Shadow
    public abstract void resize(int width, int height);

    @Unique
    private boolean tacz$stencilEnabled = false;

    @WrapOperation(method = "createBuffers", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/util/function/Supplier;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;"))
    private GpuTexture tacz$depthWithStencil(GpuDevice device, Supplier<String> label, int usage, TextureFormat format,
                                             int width, int height, int depthOrLayers, int mipLevels,
                                             Operation<GpuTexture> original) {
        // 只碰深度纹理，颜色纹理照常。不用 ordinal 判断，是因为 useDepth 为假时深度那次调用根本不存在。
        if (!this.tacz$stencilEnabled || !format.hasDepthAspect()) {
            return original.call(device, label, usage, format, width, height, depthOrLayers, mipLevels);
        }
        StencilSupport.beginPackedDepthStencil();
        try {
            GpuTexture texture = original.call(device, label, usage, format, width, height, depthOrLayers, mipLevels);
            if (texture instanceof GlTexture glTexture) {
                StencilSupport.registerStencilTexture(glTexture.glId());
                com.tacz.guns.client.gl.RenderDebug.note("packed depth+stencil texture " + glTexture.glId());
            }
            return texture;
        } finally {
            StencilSupport.endPackedDepthStencil();
        }
    }

    @Inject(method = "destroyBuffers", at = @At("HEAD"))
    private void tacz$forgetStencilTexture(CallbackInfo ci) {
        if (this.depthTexture instanceof GlTexture glTexture) {
            StencilSupport.forgetStencilTexture(glTexture.glId());
        }
    }

    @Override
    public void tacz$enableStencil() {
        if (!this.tacz$stencilEnabled) {
            this.tacz$stencilEnabled = true;
            com.tacz.guns.client.gl.RenderDebug.note("tacz$enableStencil -> resize " + this.width + "x" + this.height);
            this.resize(this.width, this.height);
        }
    }

    @Override
    public void tacz$disableStencil() {
        if (this.tacz$stencilEnabled) {
            this.tacz$stencilEnabled = false;
            com.tacz.guns.client.gl.RenderDebug.note("tacz$disableStencil -> resize " + this.width + "x" + this.height);
            this.resize(this.width, this.height);
        }
    }

    @Override
    public boolean tacz$isStencilEnabled() {
        return this.tacz$stencilEnabled;
    }
}
