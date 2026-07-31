package com.tacz.guns.client.init;

import com.tacz.guns.GunMod;
import com.tacz.guns.api.client.other.ThirdPersonManager;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.gui.overlay.HeatBarOverlay;
import com.tacz.guns.client.gui.overlay.InteractKeyTextOverlay;
import com.tacz.guns.client.gui.overlay.KillAmountOverlay;
import com.tacz.guns.client.input.*;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.tooltip.ClientAmmoBoxTooltip;
import com.tacz.guns.client.tooltip.ClientAttachmentItemTooltip;
import com.tacz.guns.client.tooltip.ClientBlockItemTooltip;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.compat.controllable.ControllableCompat;
import com.tacz.guns.compat.immediatelyfast.ImmediatelyFastCompat;
import com.tacz.guns.compat.playeranimator.PlayerAnimatorCompat;
import com.tacz.guns.compat.shouldersurfing.ShoulderSurfingCompat;
import com.tacz.guns.compat.zoomify.ZoomifyCompat;
import com.tacz.guns.init.ModItems;
import com.tacz.guns.inventory.tooltip.AmmoBoxTooltip;
import com.tacz.guns.inventory.tooltip.AttachmentItemTooltip;
import com.tacz.guns.inventory.tooltip.BlockItemTooltip;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.item.AmmoBoxItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

@Environment(EnvType.CLIENT)
public class ClientSetupEvent {
    public static void init() {
        registerKeyMappings();
        registerClientTooltips();
        registerGuiOverlays();
        ClientLifecycleEvents.CLIENT_STARTED.register(ClientSetupEvent::onClientSetup);
        onClientResourceReload();
    }

    public static void registerKeyMappings() {
        // 注册键位
        // MKB 1.21存在不能单独设置alt, ctrl, shift的bug，暂时弃用
        registerKeyBinding(InspectKey.INSPECT_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(ReloadKey.RELOAD_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(ShootKey.SHOOT_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(InteractKey.INTERACT_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(FireSelectKey.FIRE_SELECT_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(AimKey.AIM_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(CrawlKey.CRAWL_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(RefitKey.REFIT_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(ZoomKey.ZOOM_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(MeleeKey.MELEE_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.NONE*/);
        registerKeyBinding(ConfigKey.OPEN_CONFIG_KEY/*, KeyConflictContext.IN_GAME, KeyModifier.ALT*/);
    }

    private static void registerKeyBinding(KeyMapping keyMapping/*, KeyConflictContext keyConflictContext, KeyModifier keyModifier*/) {
        KeyBindingHelper.registerKeyBinding(keyMapping);
//        var iKey = (IKeyBinding) keyMapping;
//        iKey.setKeyConflictContext(keyConflictContext);
//        iKey.setKeyModifierAndCode(keyModifier, iKey.getKey());
    }

    public static void registerClientTooltips() {
        // 注册文本提示
        TooltipComponentCallback.EVENT.register(tooltip -> {
            if (tooltip instanceof GunTooltip gunTooltip) {
                return new ClientGunTooltip(gunTooltip);
            }
            if (tooltip instanceof AmmoBoxTooltip ammoBoxTooltip) {
                return new ClientAmmoBoxTooltip(ammoBoxTooltip);
            }
            if (tooltip instanceof AttachmentItemTooltip attachmentItemTooltip) {
                return new ClientAttachmentItemTooltip(attachmentItemTooltip);
            }
            if (tooltip instanceof BlockItemTooltip blockItemTooltip) {
                return new ClientBlockItemTooltip(blockItemTooltip);
            }
            return null;
        });
    }

    public static void registerGuiOverlays() {
        // 注册 HUD。HudRenderCallback 换成了 HudElementRegistry，每一层有自己的 id，
        // 别的模组可以照着它插进来或者替换掉
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "gun_hud"), GunHudOverlay.INSTANCE);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "heat_bar"), HeatBarOverlay.INSTANCE);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "interact_key_text"), InteractKeyTextOverlay.INSTANCE);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(GunMod.MOD_ID, "kill_amount"), KillAmountOverlay.INSTANCE);
    }

    public static void onClientSetup(Minecraft minecraft) {
        // 注册自己的的硬编码第三人称动画
        ThirdPersonManager.registerDefault();

        // TODO: 弹药盒的染色。物品的 ColorProviderRegistry 没了 —— 1.21.4 之后物品染色是
        //  模型里的 tints，要注册一个 ItemTintSource 再在 items/ammo_box.json 里引用。
        //  和上面那条变种一起做。

        // TODO: 弹药盒的变种。ItemProperties 和模型里的 overrides 一起没了，1.21.4 之后
        //  要改成 items/ammo_box.json 里的 minecraft:select 加一个注册过的物品模型属性。
        //  和 ammo_box 的模型迁移一起做。

        // 初始化自己的枪包下载器
//       ClientGunPackDownloadManager.init();

//        // 与 player animator 的兼容
//       PlayerAnimatorCompat.init();

        // 与 Shoulder Surfing Reloaded 的兼容
        ShoulderSurfingCompat.init();

        // 与 Controllable 的兼容
        ControllableCompat.init();

        ZoomifyCompat.init();
        ImmediatelyFastCompat.init();

        // 模板缓冲不在这里开了：recordRenderCall 没了，而且 StencilSupport 用到的时候
        // 自己会开，第一支瞄具渲染时才付这个代价
    }

    public static void onClientResourceReload() {
        PlayerAnimatorCompat.init();

        ClientAssetsManager.INSTANCE.reloadAndRegister(ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)::registerReloadListener);
        if (PlayerAnimatorCompat.isInstalled()) {
            PlayerAnimatorCompat.registerReloadListener(ResourceManagerHelper.get(PackType.CLIENT_RESOURCES)::registerReloadListener);
        }
    }
}
