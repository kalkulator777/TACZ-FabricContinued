package com.tacz.guns.client.gui.toast;

import net.minecraft.client.renderer.RenderPipelines;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@Environment(EnvType.CLIENT)
public class GunLevelUpToast implements Toast {
    private final Component title;
    private final Component subTitle;
    private final ItemStack icon;
    private Visibility visibility = Visibility.SHOW;

    public GunLevelUpToast(ItemStack icon, Component titleComponent, @Nullable Component subtitle) {
        this.icon = icon;
        this.title = titleComponent;
        this.subTitle = subtitle;
    }

    /**
     * Toast 在 1.21.2 拆成了三块：可见性自己报，状态在 update 里推进，render 只负责画。
     * 这个类本来就没被用上（见下面那条 todo），所以只补出接口要求的形状。
     */
    @Override
    public @NotNull Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(@NotNull ToastManager toastManager, long timeSinceLastVisible) {
        this.visibility = timeSinceLastVisible >= 5000L ? Visibility.HIDE : Visibility.SHOW;
    }

    @Override
    public void render(@NotNull GuiGraphics gui, @NotNull Font font, long timeSinceLastVisible) {
        // todo 这个类没有实际使用，先不管了
    }
}
