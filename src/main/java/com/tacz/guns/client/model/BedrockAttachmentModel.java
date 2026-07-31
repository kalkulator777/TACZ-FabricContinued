package com.tacz.guns.client.model;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.client.gl.ScopeRenderTypes;
import com.tacz.guns.client.gl.ScopeDebug;
import com.tacz.guns.client.gl.StencilSupport;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.model.functional.TextShowRender;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BedrockAttachmentModel extends BedrockAnimatedModel {
    /** 只在 -Dtacz.scopeDebug=true 时用来给诊断输出限流，见 {@link ScopeDebug}。 */
    private int tacz$scopeDebugTick = 0;

    private static final String SCOPE_VIEW_NODE = "scope_view";
    private static final String SCOPE_BODY_NODE = "scope_body";
    private static final String OCULAR_RING_NODE = "ocular_ring";
    private static final String DIVISION_NODE = "division";
    private static final String OCULAR_NODE = "ocular";
    private static final String OCULAR_SIGHT_NODE = "ocular_sight";
    private static final String OCULAR_SCOPE_NODE = "ocular_scope";
    private static final Pattern LASER_BEAM_PATTERN = Pattern.compile("^laser_beam(_(\\d+))?$");

    protected List<List<BedrockPart>> scopeViewPaths;
    protected @Nullable List<BedrockPart> scopeBodyPath;
    protected @Nullable List<BedrockPart> ocularRingPath;
    protected List<List<BedrockPart>> ocularNodePaths;
    protected List<Boolean> isScopeOcular;
    protected List<List<BedrockPart>> divisionNodePaths;
    protected @Nullable List<List<BedrockPart>> laserBeamPaths;

    private @Nullable ItemStack currentGunItem;
    private @Nullable ItemStack attachmentItem;

    private boolean isScope = false;
    private boolean isSight = false;
    private float scopeViewRadiusModifier = 1;

    public BedrockAttachmentModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
        scopeViewPaths = new ArrayList<>();
        ocularNodePaths = new ArrayList<>();
        isScopeOcular = new ArrayList<>();
        divisionNodePaths = new ArrayList<>();
        laserBeamPaths = new ArrayList<>();
        // 初始化 view 的 node path
        List<BedrockPart> path = getPath(modelMap.get(SCOPE_VIEW_NODE));
        int i = 2;
        while (path != null) {
            scopeViewPaths.add(path);
            path = getPath(modelMap.get(SCOPE_VIEW_NODE + '_' + i++));
        }
        // 初始化 ocular 的 node path
        String ocularRegex = "^(" + OCULAR_NODE + "|" + OCULAR_SIGHT_NODE + "|" + OCULAR_SCOPE_NODE + ")(_(\\d+))?$";
        Pattern ocularPattern = Pattern.compile(ocularRegex);
        TreeMap<Integer, OcularWrapper> map = new TreeMap<>();
        for (Map.Entry<String, ModelRendererWrapper> entry : modelMap.entrySet()) {
            Matcher matcher = ocularPattern.matcher(entry.getKey());
            if (matcher.matches()) {
                int num = 1;
                String numStr = matcher.group(3);
                if (numStr != null) {
                    num = Integer.parseInt(numStr);
                }
                String type = matcher.group(1);
                boolean isScope = OCULAR_SCOPE_NODE.equals(type);
                map.put(num, new OcularWrapper(entry.getValue(), isScope));
            }
            if (LASER_BEAM_PATTERN.matcher(entry.getKey()).find()) {
                laserBeamPaths.add(getPath(entry.getValue()));
            }
        }
        for (OcularWrapper wrapper : map.values()) {
            ocularNodePaths.add(getPath(wrapper.renderer));
            isScopeOcular.add(wrapper.isScope);
        }
        // 初始化 division 的 node path
        ModelRendererWrapper divisionModel = modelMap.get(DIVISION_NODE);
        path = getPath(modelMap.get(DIVISION_NODE));
        i = 2;
        while (path != null) {
            divisionNodePaths.add(path);
            divisionModel.setHidden(true);
            divisionModel = modelMap.get(DIVISION_NODE + '_' + i++);
            path = getPath(divisionModel);
        }

        scopeBodyPath = getPath(modelMap.get(SCOPE_BODY_NODE));
        ocularRingPath = getPath(modelMap.get(OCULAR_RING_NODE));
    }

    @Nullable
    public List<BedrockPart> getScopeViewPath(int viewSwitchCount) {
        if (scopeViewPaths.isEmpty()) {
            return null;
        }
        if (viewSwitchCount >= scopeViewPaths.size()) {
            return scopeViewPaths.get(0);
        }
        return scopeViewPaths.get(viewSwitchCount);
    }

    public void setIsScope(boolean isScope) {
        this.isScope = isScope;
    }

    public void setIsSight(boolean isSight) {
        this.isSight = isSight;
    }

    public boolean isScope() {
        return isScope;
    }

    public boolean isSight() {
        return isSight;
    }

    public void setScopeViewRadiusModifier(float scopeViewRadiusModifier) {
        this.scopeViewRadiusModifier = scopeViewRadiusModifier;
    }

    /**
     * 添加枪械自定义的文本显示
     */
    public void setTextShowList(Map<String, TextShow> textShowList) {
        textShowList.forEach((name, textShow) -> this.setFunctionalRenderer(name,
                bedrockPart -> new TextShowRender(this, textShow, currentGunItem)));
    }

    /**
     * @param texture 模型材质。模板遮罩要用到它派生出来的几个变体（只写模板的、不做深度测试的），
     *                而 1.21.5 之后没法从一个 RenderType 反查出材质，所以只能一路传进来。
     */
    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, Identifier texture, int light, int overlay) {
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (transformType.firstPerson()) {
            if (isScope && isSight) {
                renderBoth(matrixStack, transformType, renderType, texture, light, overlay);
            } else if (isScope) {
                renderScope(matrixStack, transformType, renderType, texture, light, overlay);
            } else if (isSight) {
                renderSight(matrixStack, transformType, renderType, texture, light, overlay);
            }
        } else {
            if (scopeBodyPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
            }
            if (ocularRingPath != null) {
                renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
            }
        }
        if (!isScope && !isSight && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
        super.render(matrixStack, transformType, renderType, light, overlay);
        if ((isScope || isSight) && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
    }

    /**
     * 走收集器的那条路，给物品渲染器用。
     * <p>
     * 只覆盖非第一人称的分支。第一人称那条是模板遮罩，它要求这些绘制按顺序立刻发生 ——
     * 写模板、按模板裁剪、再写下一层，而收集器是把几何体攒起来之后一起画的，顺序保证不了。
     * 所以第一人称仍然落回上面那个即时方法。
     */
    public void render(@Nullable ItemStack attachmentItem, ItemStack currentGunItem, PoseStack matrixStack,
                       ItemDisplayContext transformType, SubmitNodeCollector collector, RenderType renderType,
                       Identifier texture, int light, int overlay) {
        if (transformType.firstPerson()) {
            render(attachmentItem, currentGunItem, matrixStack, transformType, renderType, texture, light, overlay);
            return;
        }
        this.currentGunItem = currentGunItem;
        this.attachmentItem = attachmentItem;
        if (scopeBodyPath != null) {
            submitTempPart(matrixStack, transformType, collector, renderType, light, overlay, scopeBodyPath);
        }
        if (ocularRingPath != null) {
            submitTempPart(matrixStack, transformType, collector, renderType, light, overlay, ocularRingPath);
        }
        if (!isScope && !isSight && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
        super.render(matrixStack, transformType, collector, renderType, light, overlay);
        if ((isScope || isSight) && laserBeamPaths != null) {
            for (var entry : laserBeamPaths) {
                BeamRenderer.renderLaserBeam(attachmentItem, matrixStack, transformType, entry);
            }
        }
    }

    /**
     * {@link #renderTempPart} 的收集器版本。那些部件平时是隐藏的，只在这里临时打开 ——
     * 而几何体是延后画的，所以 visible 不能在提交之后再关掉，得在回调里开、在回调里关。
     */
    private void submitTempPart(PoseStack poseStack, ItemDisplayContext transformType, SubmitNodeCollector collector,
                                RenderType renderType, int light, int overlay, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (int i = 0; i < path.size() - 1; ++i) {
            path.get(i).translateAndRotateAndScale(poseStack);
        }
        BedrockPart part = path.get(path.size() - 1);
        collector.submitCustomGeometry(poseStack, renderType, (pose, builder) -> {
            PoseStack local = new PoseStack();
            local.last().set(pose);
            boolean wasVisible = part.visible;
            part.visible = true;
            part.render(local, transformType, builder, light, overlay);
            part.visible = wasVisible;
        });
        poseStack.popPose();
    }

    private Vector3f getBedrockPartCenter(PoseStack poseStack, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        poseStack.last().pose().mulLocal(RenderSystem.getModelViewMatrix());

        for (BedrockPart part : path) {
            part.translateAndRotateAndScale(poseStack);
        }
        Vector3f result = new Vector3f(poseStack.last().pose().m30(), poseStack.last().pose().m31(), poseStack.last().pose().m32());
        poseStack.popPose();
        return result;
    }

    private void renderTempPart(PoseStack poseStack, ItemDisplayContext transformType, RenderType renderType,
                                int light, int overlay, @Nonnull List<BedrockPart> path) {
        poseStack.pushPose();
        for (int i = 0; i < path.size() - 1; ++i) {
            path.get(i).translateAndRotateAndScale(poseStack);
        }
        BedrockPart part = path.get(path.size() - 1);
        part.visible = true;
        MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);
        part.render(poseStack, transformType, vertexConsumer, light, overlay);
        bufferSource.endBatch(renderType);
        part.visible = false;
        poseStack.popPose();
    }

    /**
     * 把目镜的轮廓盖进模板缓冲。
     * <p>
     * 旧代码在这里关掉颜色和深度写入，1.21.5 之后这两个开关是 RenderPipeline 的属性，
     * 不能再随手改，所以换成用一个写死了「不写颜色、不写深度」的 RenderType 去画。
     */
    private void renderOcularStencil(PoseStack matrixStack, ItemDisplayContext transformType, Identifier texture, int light, int overlay, boolean isScope) {
        if (!ocularNodePaths.isEmpty()) {
            RenderType stencilOnly = ScopeRenderTypes.stencilOnly(texture);
            StencilSupport.mask(0xFF);
            StencilSupport.op(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            // 绘制目镜
            for (int i = ocularNodePaths.size() - 1; i >= 0; i--) {
                if (isScope == isScopeOcular.get(i)) {
                    StencilSupport.func(GL11.GL_GREATER, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, stencilOnly, light, overlay, ocularNodePaths.get(i));
                }
            }
            // 恢复模板状态
            StencilSupport.op(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        }
    }

    private void renderDivisionOnly(PoseStack matrixStack, ItemDisplayContext transformType, Identifier texture, int light, int overlay) {
        if (!divisionNodePaths.isEmpty()) {
            // 旧代码在这里 disableDepthTest()，现在深度测试也进了管线，改用不做深度测试的 RenderType
            RenderType noDepthTest = ScopeRenderTypes.noDepthTest(texture);
            for (int i = 0; i < divisionNodePaths.size(); i++) {
                StencilSupport.func(GL11.GL_EQUAL, i + 1, 0xFF);
                renderTempPart(matrixStack, transformType, noDepthTest, light, overlay, divisionNodePaths.get(i));
            }
        }
    }

    private void renderOcularAndDivision(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, Identifier texture, int light, int overlay, boolean selective) {
        if (!ocularNodePaths.isEmpty()) {
            // 准备渲染圆形模板层。圆本身也是只写模板不写颜色的，那部分状态在 CIRCLE_PIPELINE 里。
            StencilSupport.op(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INVERT);
            // 80是一个随便找的大小合适的数值。
            float rad = 80 * scopeViewRadiusModifier;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                rad *= IClientPlayerGunOperator.fromLocalPlayer(player).getClientAimingProgress(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
            }
            for (int i = 0; i < ocularNodePaths.size(); i++) {
                if (selective && !isScopeOcular.get(i)) {
                    continue;
                }
                StencilSupport.func(GL11.GL_EQUAL, i + 1, 0xFF);
                Vector3f ocularCenter = getBedrockPartCenter(matrixStack, ocularNodePaths.get(i));
                float centerX = ocularCenter.x() * 16 * 90;
                float centerY = ocularCenter.y() * 16 * 90;
                if (ScopeDebug.ENABLED && tacz$scopeDebugTick++ % 60 == 0) {
                    ScopeDebug.logCircle(i,
                            player == null ? -1F : IClientPlayerGunOperator.fromLocalPlayer(player)
                                    .getClientAimingProgress(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)),
                            scopeViewRadiusModifier, rad, centerX, centerY,
                            ocularNodePaths.size(), divisionNodePaths.size());
                }
                ScopeRenderTypes.drawStencilCircle(matrixStack.last(), centerX, centerY, rad, 90);
            }
            StencilSupport.op(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            for (int i = 0; i < ocularNodePaths.size() && i < divisionNodePaths.size(); i++) {
                if (i > Byte.MAX_VALUE) {
                    throw new IllegalArgumentException("Index of oculus is out of range for 127");
                }
                if (selective && !isScopeOcular.get(i)) {
                    StencilSupport.func(GL11.GL_EQUAL, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
                } else {
                    // 渲染目镜黑色遮罩
                    StencilSupport.func(GL11.GL_EQUAL, i + 1, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularNodePaths.get(i));
                    // 渲染划分
                    int b = ~(i + 1) & 0xFF;
                    StencilSupport.func(GL11.GL_EQUAL, b, 0xFF);
                    renderTempPart(matrixStack, transformType, renderType, light, overlay, divisionNodePaths.get(i));
                }
            }
        }
    }

    private void renderBoth(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, Identifier texture, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        StencilSupport.clear();
        if (ocularRingPath != null) {
            StencilSupport.func(GL11.GL_ALWAYS, 0, 0xFF);
            StencilSupport.op(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            // 渲染目镜外环
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
        }
        // 渲染目镜以写入模板桓冲值 (暂时只渲染 ocular_scope)
        renderOcularStencil(matrixStack, transformType, texture, light, overlay, true);
        // 渲染镜身
        if (scopeBodyPath != null) {
            StencilSupport.func(GL11.GL_EQUAL, 0, 0xFF);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        // 渲染目镜以写入模板桓冲值 (渲染其他的目镜)
        renderOcularStencil(matrixStack, transformType, texture, light, overlay, false);
        // 渲染目镜遮罩和划分
        renderOcularAndDivision(matrixStack, transformType, renderType, texture, light, overlay, true);
        // 关闭模板缓冲
        StencilSupport.func(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay);
    }

    private void renderSight(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, Identifier texture, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        StencilSupport.clear();
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, texture, light, overlay, false);
        // 渲染划分
        renderDivisionOnly(matrixStack, transformType, texture, light, overlay);
        // 关闭模板缓冲
        StencilSupport.func(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        if (scopeBodyPath != null) {
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        super.render(matrixStack, transformType, renderType, light, overlay);
    }

    private void renderScope(PoseStack matrixStack, ItemDisplayContext transformType, RenderType renderType, Identifier texture, int light, int overlay) {
        RenderHelper.enableItemEntityStencilTest();
        // 清空模板缓冲区、准备绘制模板缓冲
        StencilSupport.clear();
        // 渲染目镜外环
        if (ocularRingPath != null) {
            StencilSupport.func(GL11.GL_ALWAYS, 0, 0xFF);
            StencilSupport.op(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, ocularRingPath);
        }
        // 渲染目镜以写入模板桓冲值
        renderOcularStencil(matrixStack, transformType, texture, light, overlay, false);
        // 渲染镜身
        if (scopeBodyPath != null) {
            StencilSupport.func(GL11.GL_EQUAL, 0, 0xFF);
            renderTempPart(matrixStack, transformType, renderType, light, overlay, scopeBodyPath);
        }
        // 渲染目镜遮罩和划分
        renderOcularAndDivision(matrixStack, transformType, renderType, texture, light, overlay, false);
        // 关闭模板缓冲
        StencilSupport.func(GL11.GL_ALWAYS, 0, 0xFF);
        RenderHelper.disableItemEntityStencilTest();
        // 渲染其他部分
        super.render(matrixStack, transformType, renderType, light, overlay);
    }

    private static class OcularWrapper {
        public ModelRendererWrapper renderer;
        public boolean isScope;

        public OcularWrapper(ModelRendererWrapper renderer, boolean isScope) {
            this.renderer = renderer;
            this.isScope = isScope;
        }
    }
}