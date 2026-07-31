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
