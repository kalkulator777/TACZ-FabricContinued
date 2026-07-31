package com.tacz.guns.client.gl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.tacz.guns.GunMod;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Function;

/**
 * 瞄具模板遮罩用的管线和 RenderType。
 * <p>
 * 1.21.5 之后 colorMask / depthMask / depthFunc 不再是可以随手改的全局状态，
 * 而是 {@link RenderPipeline} 的属性，也就是说它们被烘进了 RenderType。
 * 原来 {@code RenderSystem.colorMask(false, ...)} 那样的写法，现在只能换成
 * 「用一个写死了这些状态的 RenderType 去画」。这里就是那几个 RenderType。
 */
@Environment(EnvType.CLIENT)
public final class ScopeRenderTypes {
    /**
     * 只写模板、不写颜色也不写深度的管线。
     * <p>
     * 不带 ALPHA_CUTOUT：这一趟只是把目镜的轮廓盖进模板缓冲，本来就不该有片元被材质的
     * alpha 丢掉。也正因为不写颜色，材质具体是什么无所谓，用调用方给的那张就行。
     */
    private static final RenderPipeline STENCIL_ONLY_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_stencil_only"))
            .withSampler("Sampler1")
            .withColorWrite(false)
            .withDepthWrite(false)
            .build();

    /** 关掉深度测试的实体管线，用来把分划画到镜身之上。对应旧代码里的 RenderSystem.disableDepthTest()。 */
    private static final RenderPipeline NO_DEPTH_TEST_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_no_depth_test"))
            .withShaderDefine("ALPHA_CUTOUT", 0.1F)
            .withSampler("Sampler1")
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .build();

    /**
     * 画模板圆形用的管线。顶点只有位置，颜色被 mask 掉了，所以不需要。
     * 三角扇按 SkyRenderer 的路子直接 draw，不走索引缓冲。
     */
    static final RenderPipeline CIRCLE_PIPELINE = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "pipeline/scope_stencil_circle"))
            .withVertexShader("core/position")
            .withFragmentShader("core/position")
            .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.TRIANGLE_FAN)
            .withColorWrite(false)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private static final Function<Identifier, RenderType> STENCIL_ONLY = Util.memoize(
            texture -> RenderType.create("tacz_scope_stencil_only", RenderSetup.builder(STENCIL_ONLY_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()));

    private static final Function<Identifier, RenderType> NO_DEPTH_TEST = Util.memoize(
            texture -> RenderType.create("tacz_scope_no_depth_test", RenderSetup.builder(NO_DEPTH_TEST_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()));

    private ScopeRenderTypes() {
    }

    public static RenderType stencilOnly(Identifier texture) {
        return STENCIL_ONLY.apply(texture);
    }

    public static RenderType noDepthTest(Identifier texture) {
        return NO_DEPTH_TEST.apply(texture);
    }

    /**
     * 画一个只写模板缓冲的圆。旧代码用的是 Tesselator + BufferUploader.drawWithShader，
     * 1.21.5 之后那套即时绘制没有了，改成 SkyRenderer 里的写法：自己攒一份网格，
     * 上传到即时顶点缓冲，再开一个渲染通道画掉。三角扇不走索引，直接 draw。
     *
     * @param pose    圆心和圆周顶点会先在 CPU 上被这个矩阵变换一遍，和旧代码一致
     * @param segments 圆周细分段数
     */
    public static void drawStencilCircle(PoseStack.Pose pose, float centerX, float centerY, float radius, int segments) {
        int vertexSize = DefaultVertexFormat.POSITION.getVertexSize();
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized((segments + 2) * vertexSize)) {
            BufferBuilder builder = new BufferBuilder(byteBufferBuilder, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            builder.addVertex(pose, centerX, centerY, -90.0F);
            for (int i = 0; i <= segments; i++) {
                float angle = i * ((float) Math.PI * 2F) / segments;
                builder.addVertex(pose, centerX + Mth.cos(angle) * radius, centerY + Mth.sin(angle) * radius, -90.0F);
            }
            try (MeshData mesh = builder.buildOrThrow()) {
                RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
                GpuTextureView color = target.getColorTextureView();
                GpuTextureView depth = target.getDepthTextureView();
                if (color == null || depth == null) {
                    return;
                }
                GpuBuffer vertexBuffer = CIRCLE_PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(mesh.vertexBuffer());
                GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                        .writeTransform(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), new Matrix4f());
                try (RenderPass pass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "TACZ scope stencil circle", color, OptionalInt.empty(), depth, OptionalDouble.empty())) {
                    pass.setPipeline(CIRCLE_PIPELINE);
                    RenderSystem.bindDefaultUniforms(pass);
                    pass.setUniform("DynamicTransforms", transforms);
                    pass.setVertexBuffer(0, vertexBuffer);
                    pass.draw(0, mesh.drawState().vertexCount());
                    /* 问帧缓冲要放在 draw 之后。放在 setPipeline 之后问过一次，答案是 fbo=3 且
                     * 没有模板附件 —— 但那一次的 createFbo 记录是在这之后才打出来的，也就是说
                     * 当时编码器还没绑我们这对纹理，问到的是上一个通道留下的帧缓冲。绘制命令
                     * 发出去之后，绑定一定已经生效。*/
                    RenderDebug.dumpAttachmentInDrawPass();
                }
            }
        }
    }
}
