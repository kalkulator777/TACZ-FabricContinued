package cn.sh1rocu.tacz.api.mixin;

public interface RenderTargetStencil {
    void tacz$enableStencil();

    /**
     * 把深度纹理换回原版格式。光影包接管管线之后必须做这一步，理由见
     * {@code StencilSupport#syncWithShaderPack()}。
     */
    void tacz$disableStencil();

    boolean tacz$isStencilEnabled();
}
