package com.tacz.guns.client.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.ARBDirectStateAccess;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * 1.21.9 起 RenderSystem 里已经没有任何模板缓冲的接口，RenderPipeline 也不描述模板状态。
 * 好在原版从头到尾不碰模板，所以裸 GL 调用不会和管线状态互相覆盖，直接用即可。
 * <p>
 * 但有两点和 1.21.1 不同，必须注意：
 * <ul>
 *     <li>主渲染目标的深度纹理现在是 DEPTH32，没有模板位。把它换成 DEPTH32F_STENCIL8
 *         并给 FBO 补上模板附件的活，在 RenderTargetMixin / GlDeviceMixin / GlTextureMixin 里，
 *         由本类的 {@link #beginPackedDepthStencil()} 一族方法串起来。</li>
 *     <li>渲染通道结束时 GlCommandEncoder 会把 FBO 绑回 0（窗口默认帧缓冲），
 *         所以 glClear 必须在绑定了主渲染目标的通道内执行，见 {@link #clear()}。
 *         模板的 func / op / mask 是全局上下文状态，什么时候设都行。</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public final class StencilSupport {
    /** GL_DEPTH32F_STENCIL8：和原版的 DEPTH32 一样是浮点深度，额外多 8 位模板。 */
    public static final int GL_DEPTH32F_STENCIL8 = 0x8CAD;
    /** GL_DEPTH_STENCIL */
    public static final int GL_DEPTH_STENCIL = 0x84F9;
    /** GL_FLOAT_32_UNSIGNED_INT_24_8_REV */
    public static final int GL_FLOAT_32_UNSIGNED_INT_24_8_REV = 0x8DAD;
    /** GL_STENCIL_ATTACHMENT */
    public static final int GL_STENCIL_ATTACHMENT = 0x8D20;
    /** GL_DRAW_FRAMEBUFFER */
    public static final int GL_DRAW_FRAMEBUFFER = 0x8CA9;

    /** 带模板位的深度纹理的 GL id。GlTextureMixin 靠它决定要不要额外挂模板附件。 */
    private static final IntSet STENCIL_TEXTURES = new IntOpenHashSet();

    private static boolean allocatingPackedDepthStencil = false;

    private StencilSupport() {
    }

    // ---------------------------------------------------------------- 深度 + 模板纹理的分配

    /**
     * 在这之后创建的深度纹理会被 GlDeviceMixin 换成 DEPTH32F_STENCIL8。
     * 只在渲染线程上、且紧贴着一次 createTexture 调用使用，所以一个静态标志就够了。
     */
    public static void beginPackedDepthStencil() {
        allocatingPackedDepthStencil = true;
    }

    public static void endPackedDepthStencil() {
        allocatingPackedDepthStencil = false;
    }

    public static boolean isAllocatingPackedDepthStencil() {
        return allocatingPackedDepthStencil;
    }

    /** 已经补挂过模板附件的 FBO。FBO 的 id 同样会被回收，所以纹理集合一变就得清空。 */
    private static final IntSet PATCHED_FBOS = new IntOpenHashSet();

    public static void registerStencilTexture(int glId) {
        STENCIL_TEXTURES.add(glId);
        PATCHED_FBOS.clear();
    }

    public static void forgetStencilTexture(int glId) {
        STENCIL_TEXTURES.remove(glId);
        PATCHED_FBOS.clear();
    }

    /** 第一次见到这个 FBO 时把模板面补挂上去，之后直接跳过。 */
    public static void ensureStencilAttached(int fbo, int depthTextureId) {
        if (PATCHED_FBOS.add(fbo)) {
            attachStencilTo(fbo, depthTextureId);
        }
    }

    public static boolean isStencilTexture(int glId) {
        return !STENCIL_TEXTURES.isEmpty() && STENCIL_TEXTURES.contains(glId);
    }

    /**
     * 把深度纹理同时挂到 GL_STENCIL_ATTACHMENT 上。原版只挂了 GL_DEPTH_ATTACHMENT，
     * 而 DEPTH32F_STENCIL8 的模板面需要单独挂一次才能用。
     * <p>
     * 走 glNamedFramebufferTexture，和原版 DirectStateAccess.Core.bindFrameBufferTextures
     * 挂颜色和深度用的是同一个函数。先前这里是「绑 FBO — 挂纹理 — 绑回去」，而
     * {@link GlStateManager#_glBindFramebuffer} 会跳过它认为多余的绑定：只要它记的当前值
     * 和要绑的值一样，就一次 GL 调用都不发。于是模板面被挂到了当时真正绑着的那个 FBO 上，
     * 而不是参数里的那个 —— 诊断打出来的就是这个结果：附件挂到了 25 号，而实际在画的那个
     * 帧缓冲查出来 stencilAttachment=0。DSA 不碰绑定状态，也就没有这个问题。
     */
    public static void attachStencilTo(int fbo, int depthTextureId) {
        ScopeDebug.note("attachStencilTo fbo=" + fbo + " depthTexture=" + depthTextureId);
        if (GL.getCapabilities().GL_ARB_direct_state_access) {
            ARBDirectStateAccess.glNamedFramebufferTexture(fbo, GL_STENCIL_ATTACHMENT, depthTextureId, 0);
            return;
        }
        // 没有 DSA 的驱动上只能绑定后再挂。先绑一个别的，逼 GlStateManager 把绑定真的发出去。
        int previous = GlStateManager.getFrameBuffer(GL_DRAW_FRAMEBUFFER);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, fbo);
        GlStateManager._glFramebufferTexture2D(GL_DRAW_FRAMEBUFFER, GL_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D, depthTextureId, 0);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, previous);
    }

    // ---------------------------------------------------------------- 模板状态

    public static void enableTest() {
        RenderSystem.assertOnRenderThread();
        ScopeDebug.note("enableTest");
        Minecraft.getInstance().getMainRenderTarget().tacz$enableStencil();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    public static void disableTest() {
        RenderSystem.assertOnRenderThread();
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    public static void func(int func, int ref, int mask) {
        GL11.glStencilFunc(func, ref, mask);
    }

    public static void op(int stencilFail, int depthFail, int pass) {
        GL11.glStencilOp(stencilFail, depthFail, pass);
    }

    public static void mask(int mask) {
        GL11.glStencilMask(mask);
    }

    /**
     * 清空模板缓冲。
     * <p>
     * 必须在一个绑定了主渲染目标的渲染通道里做：通道之间 GL 的绘制帧缓冲是窗口默认帧缓冲，
     * 在那上面 glClear 清的不是我们要的东西。这里开一个不做任何清除、也不画任何东西的空通道，
     * 只为了让 GlCommandEncoder 帮我们把 FBO 绑对。
     */
    public static void clear() {
        RenderSystem.assertOnRenderThread();
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView color = target.getColorTextureView();
        GpuTextureView depth = target.getDepthTextureView();
        if (color == null || depth == null) {
            return;
        }
        try (RenderPass ignored = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "TACZ stencil clear", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
            GlStateManager._disableScissorTest();
            GL11.glStencilMask(0xFF);
            GL11.glClearStencil(0);
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            ScopeDebug.dumpAttachmentOnChange();
            ScopeDebug.noteTargetTextures(target);
        }
    }
}
