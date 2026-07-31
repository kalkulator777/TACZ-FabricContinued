package com.tacz.guns.client.input;

import cn.sh1rocu.tacz.api.event.InputEvent;
import com.mojang.blaze3d.platform.InputConstants;
import com.tacz.guns.client.gui.compat.ClothConfigScreen;
import com.tacz.guns.compat.cloth.MenuIntegration;
import com.tacz.guns.init.CompatRegistry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;

import java.net.URI;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

import static com.tacz.guns.util.InputExtraCheck.isInGame;

@Environment(EnvType.CLIENT)
public class ConfigKey {
    /* Forge 版这个键是 Shift+T，靠 Forge 的 KeyModifier 和 KeyConflictContext 才不会和聊天打架。
     * 原版 KeyMapping 没有修饰键，所以直接绑 T 的结果是按 T 弹出配置界面而不是聊天框，
     * 也就是装上这个模组之后聊天键就没了。原版没有哪个键是空的，随便挑一个又会撞上别的模组，
     * 所以默认不绑定：在"选项-控制"里能看到它，配置本身也能从 Mod Menu 打开。 */
    public static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping("key.tacz.open_config.desc",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            ModKeyCategory.TACZ);

    public static void onOpenConfig(InputEvent.Key event) {
        if (OPEN_CONFIG_KEY.isUnbound()) {
            return;
        }
        if (isInGame() && event.getAction() == GLFW.GLFW_PRESS
                && OPEN_CONFIG_KEY.matches(event.getKeyEvent())) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null || player.isSpectator()) {
                return;
            }
            if (!FabricLoader.getInstance().isModLoaded(CompatRegistry.CLOTH_CONFIG)) {
                // ClickEvent 和 HoverEvent 变成了密封接口加一组记录，动作不再是枚举参数
                ClickEvent clickEvent = new ClickEvent.OpenUrl(URI.create(ClothConfigScreen.CLOTH_CONFIG_URL));
                HoverEvent hoverEvent = new HoverEvent.ShowText(Component.translatable("gui.tacz.cloth_config_warning.download"));
                MutableComponent component = Component.translatable("gui.tacz.cloth_config_warning.tips").withStyle(style ->
                        style.applyFormat(ChatFormatting.BLUE).applyFormat(ChatFormatting.UNDERLINE).withClickEvent(clickEvent).withHoverEvent(hoverEvent));
                player.displayClientMessage(component, false);
            } else {
                CompatRegistry.checkModLoad(CompatRegistry.CLOTH_CONFIG, () -> Minecraft.getInstance().setScreen(MenuIntegration.getConfigScreen(null)));
            }
        }
    }
}
