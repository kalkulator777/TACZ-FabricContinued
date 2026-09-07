package com.tacz.guns.client.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/**
 * 瞄具模板遮罩的诊断输出，靠 {@code -Dtacz.renderDebug=true} 打开，默认整个类什么都不做。
 * <p>
 * 存在的理由很具体：模板遮罩是这次移植里唯一一处既没法靠编译期发现问题、又没法靠看一眼
 * 截图断定原因的地方 —— 「圆没落在该落的位置」和「模板缓冲根本没挂上」在画面上长得很像。
 * 这两个方法各回答其中一个问题：
 * <ul>
 *     <li>{@link #dumpAttachmentOnce()} 在绑好主渲染目标的通道里问一次 GL，
 *         模板附件到底在不在、有几位、帧缓冲完不完整；</li>
 *     <li>{@link #logCircle} 打出圆的半径和圆心是怎么算出来的。半径是
 *         {@code 80 * radiusModifier * aimingProgress}，所以不瞄准时它必须是 0。</li>
 * </ul>
 * 等这个遮罩定下来之后，这个类可以直接删掉。
 */
@Environment(EnvType.CLIENT)
public final class RenderDebug {
    public static final boolean ENABLED = Boolean.getBoolean("tacz.renderDebug");

    /**
     * {@code -Dtacz.forceStencil=true}：进世界后第一帧就把模板缓冲挂上，不用真的拿着一把
     * 装了瞄具的枪。
     * <p>
     * 存在的理由：模板那条路平时只有「第一人称 + 枪 + 装了瞄具」才会走到，而自动化环境里
     * 凑齐这三样要点半天鼠标。要单独回答「把主渲染目标的深度纹理换成 packed 格式，光影下
     * 会发生什么」，这一个开关就够了 —— 它把格式切换和瞄具遮罩本身分开。
     */
    public static final boolean FORCE_STENCIL = ENABLED && Boolean.getBoolean("tacz.forceStencil");

    private static boolean stencilForced = false;
    private static boolean recoveryProbed = false;
    private static int framesSinceForced = 0;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static String lastAttachmentState = null;
    private static boolean drawPassPending = true;
    private static final java.util.Set<String> NOTED = new java.util.HashSet<>();

    private RenderDebug() {
    }

    /**
     * 必须在一个已经绑定了主渲染目标的渲染通道内部调用 —— 通道之间绑的是窗口默认帧缓冲，
     * 在那儿问出来的附件信息是别人的。
     */
    public static void dumpAttachmentOnChange() {
        if (!ENABLED) {
            return;
        }
        int fbo = GlStateManager.getFrameBuffer(StencilSupport.GL_DRAW_FRAMEBUFFER);
        int status = GL30.glCheckFramebufferStatus(StencilSupport.GL_DRAW_FRAMEBUFFER);
        int attachment = GL30.glGetFramebufferAttachmentParameteri(StencilSupport.GL_DRAW_FRAMEBUFFER,
                StencilSupport.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        /* 附件为 0 时问 STENCIL_SIZE 本身就是 GL_INVALID_OPERATION —— 每帧问一次，
         * 日志里就是每秒几百行驱动报错。只有真挂上了才值得问位数。*/
        int bits = attachment == 0 ? 0 : GL30.glGetFramebufferAttachmentParameteri(StencilSupport.GL_DRAW_FRAMEBUFFER,
                StencilSupport.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE);
        String state = String.format("fbo=%d complete=%s stencilAttachment=%d stencilBits=%d stencilTest=%s",
                fbo, status == GL30.GL_FRAMEBUFFER_COMPLETE, attachment, bits, GL11.glIsEnabled(GL11.GL_STENCIL_TEST));
        if (!state.equals(lastAttachmentState)) {
            lastAttachmentState = state;
            LOGGER.info("[tacz/render] {}", state);
        }
    }

    /** 和 {@link #dumpAttachmentOnChange()} 一样，但只打一次，且注明是在绘制通道里问的。 */
    public static void dumpAttachmentInDrawPass() {
        if (!ENABLED || !drawPassPending) {
            return;
        }
        drawPassPending = false;
        int fbo = GlStateManager.getFrameBuffer(StencilSupport.GL_DRAW_FRAMEBUFFER);
        int attachment = GL30.glGetFramebufferAttachmentParameteri(StencilSupport.GL_DRAW_FRAMEBUFFER,
                StencilSupport.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        int bits = attachment == 0 ? 0 : GL30.glGetFramebufferAttachmentParameteri(StencilSupport.GL_DRAW_FRAMEBUFFER,
                StencilSupport.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE);
        LOGGER.info("[tacz/render] in draw pass: fbo={} stencilAttachment={} stencilBits={} stencilTest={}",
                fbo, attachment, bits, GL11.glIsEnabled(GL11.GL_STENCIL_TEST));
    }

    /** 见 {@link #FORCE_STENCIL}。每帧调用，只有第一次真的做事。 */
    public static void forceStencilOnce() {
        if (!FORCE_STENCIL || stencilForced) {
            return;
        }
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        stencilForced = true;
        probeDepthCopy("before");
        log("forceStencil: attaching the stencil buffer without a scope");
        /* 故意绕过 StencilSupport#isUsable 直接换格式。要测的正是最坏的那一路：玩家先举了一次
         * 带瞄具的枪、格式已经换过，之后才开光影包。那时候只有帧边界上的 syncWithShaderPack
         * 能把它换回来，下面那次 "recovered" 探测就是在看它换没换回来。*/
        minecraft.getMainRenderTarget().tacz$enableStencil();
        probeDepthCopy("after");
    }

    /** 换回原版格式之后再探一次。{@link #forceStencilOnce()} 之后隔几帧调用。 */
    public static void probeRecovery() {
        if (!FORCE_STENCIL || !stencilForced || recoveryProbed || framesSinceForced++ < 20) {
            return;
        }
        recoveryProbed = true;
        probeDepthCopy("recovered");
    }

    /**
     * 用 Iris 每帧复制深度用的那个调用，试着把主渲染目标的深度拷进一张 DEPTH_COMPONENT32 纹理，
     * 然后问 GL 出没出错。
     * <p>
     * 这是整件事的关键一步，值得单独测：{@code glCopyImageSubData} 要求源和目标的内部格式
     * 匹配，而 Iris 是照着 {@code GpuTexture.getFormat()} 分配 depthtex1/depthtex2 的 ——
     * 那个值我们没改，还是 DEPTH32。所以「换了格式之后这个拷贝还成不成立」不能靠推理，
     * 得让驱动自己回答。前后各测一次，同一台机器同一个驱动，只有格式变了。
     * <p>
     * 全程走 DSA，不绑定任何纹理，免得把 GlStateManager 记的绑定状态搞乱。
     */
    private static void probeDepthCopy(String when) {
        com.mojang.blaze3d.pipeline.RenderTarget target = net.minecraft.client.Minecraft.getInstance().getMainRenderTarget();
        if (!(target.getDepthTexture() instanceof com.mojang.blaze3d.opengl.GlTexture depth)) {
            return;
        }
        if (!org.lwjgl.opengl.GL.getCapabilities().GL_ARB_direct_state_access) {
            log("depth copy probe {}: skipped, no DSA", when);
            return;
        }
        int width = target.width;
        int height = target.height;
        int destination = org.lwjgl.opengl.ARBDirectStateAccess.glCreateTextures(GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.ARBDirectStateAccess.glTextureStorage2D(destination, 1, 0x81A7 /* GL_DEPTH_COMPONENT32 */, width, height);
        while (GL11.glGetError() != GL11.GL_NO_ERROR) {
            // 把之前积下的错误清干净，否则读到的不是这次的
        }
        org.lwjgl.opengl.GL43.glCopyImageSubData(depth.glId(), GL11.GL_TEXTURE_2D, 0, 0, 0, 0,
                destination, GL11.GL_TEXTURE_2D, 0, 0, 0, 0, width, height, 1);
        int error = GL11.glGetError();
        LOGGER.info("[tacz/render] depth copy probe {}: source={} {}x{} glCopyImageSubData -> 0x{}",
                when, depth.glId(), width, height, Integer.toHexString(error));
        GL11.glDeleteTextures(destination);
    }

    /** 同一条消息只打一次，用来记录「这条路走过了」这类一次性事实。 */
    public static void note(String message) {
        if (ENABLED && NOTED.add(message)) {
            LOGGER.info("[tacz/render] {}", message);
        }
    }

    /**
     * 主渲染目标的颜色和深度纹理的 GL id。诊断到现在卡在一个问题上：模板挂上去的那个 FBO
     * 和渲染通道真正绑的那个不是同一个。要往下走，先得知道通道用的是哪一对纹理。
     */
    public static void noteTargetTextures(com.mojang.blaze3d.pipeline.RenderTarget target) {
        if (!ENABLED) {
            return;
        }
        note("main target colour=" + glId(target.getColorTexture()) + " depth=" + glId(target.getDepthTexture()));
    }

    private static String glId(@org.jetbrains.annotations.Nullable com.mojang.blaze3d.textures.GpuTexture texture) {
        return texture instanceof com.mojang.blaze3d.opengl.GlTexture gl ? Integer.toString(gl.glId()) : String.valueOf(texture);
    }

    /**
     * 枪口火焰的显示窗口，毫秒。只有诊断开着时 {@code -Dtacz.muzzleFlashMs=…} 才生效。
     * <p>
     * 存在的理由：窗口本身是 50 毫秒，而软件渲染下一帧就要 80 毫秒 —— 开火之后的第一帧
     * 已经在 86 毫秒，永远落不进窗口。看不到火焰不等于火焰画错了，把窗口临时拉长才能
     * 把这两件事分开。
     */
    public static long muzzleFlashWindow(long defaultMs) {
        if (!ENABLED) {
            return defaultMs;
        }
        return Long.getLong("tacz.muzzleFlashMs", defaultMs);
    }

    /** 每次都打。给那些「发生了多少次、间隔多久」才有意义的观察用。 */
    public static void log(String format, Object... args) {
        if (ENABLED) {
            LOGGER.info("[tacz/render] " + format, args);
        }
    }

    public static void logCircle(int ocular, float aimingProgress, float radiusModifier, float radius,
                                 float centerX, float centerY, int ocularCount, int divisionCount) {
        if (!ENABLED) {
            return;
        }
        LOGGER.info("[tacz/render] ocular {}/{} aiming={} radiusModifier={} radius={} center=({}, {}) divisions={}",
                ocular, ocularCount, aimingProgress, radiusModifier, radius, centerX, centerY, divisionCount);
    }
}
