package com.tacz.guns.client.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

/**
 * 瞄具模板遮罩的诊断输出，靠 {@code -Dtacz.scopeDebug=true} 打开，默认整个类什么都不做。
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
public final class ScopeDebug {
    public static final boolean ENABLED = Boolean.getBoolean("tacz.scopeDebug");

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean attachmentDumped = false;

    private ScopeDebug() {
    }

    /**
     * 必须在一个已经绑定了主渲染目标的渲染通道内部调用 —— 通道之间绑的是窗口默认帧缓冲，
     * 在那儿问出来的附件信息是别人的。
     */
    public static void dumpAttachmentOnce() {
        if (!ENABLED || attachmentDumped) {
            return;
        }
        attachmentDumped = true;
        int fbo = GlStateManager.getFrameBuffer(StencilSupport.GL_DRAW_FRAMEBUFFER);
        int status = GL30.glCheckFramebufferStatus(StencilSupport.GL_DRAW_FRAMEBUFFER);
        int attachment = GL30.glGetFramebufferAttachmentParameteri(StencilSupport.GL_DRAW_FRAMEBUFFER,
                StencilSupport.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        int bits = GL30.glGetFramebufferAttachmentParameteri(StencilSupport.GL_DRAW_FRAMEBUFFER,
                StencilSupport.GL_STENCIL_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE);
        LOGGER.info("[tacz/scope] fbo={} complete={} stencilAttachment={} stencilBits={} stencilTest={}",
                fbo, status == GL30.GL_FRAMEBUFFER_COMPLETE, attachment, bits, GL11.glIsEnabled(GL11.GL_STENCIL_TEST));
    }

    public static void logCircle(int ocular, float aimingProgress, float radiusModifier, float radius,
                                 float centerX, float centerY, int ocularCount, int divisionCount) {
        if (!ENABLED) {
            return;
        }
        LOGGER.info("[tacz/scope] ocular {}/{} aiming={} radiusModifier={} radius={} center=({}, {}) divisions={}",
                ocular, ocularCount, aimingProgress, radiusModifier, radius, centerX, centerY, divisionCount);
    }
}
