package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.AddPackFindersEvent;
import cn.sh1rocu.tacz.api.event.InputEvent;
import cn.sh1rocu.tacz.api.event.RenderTickEvent;
import cn.sh1rocu.tacz.api.mixin.PackRepositoryExtension;
import cn.sh1rocu.tacz.api.event.ClientPlayerNetworkEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Shadow
    public abstract PackRepository getResourcePackRepository();

    @Shadow
    @Final
    public Options options;

    @Shadow
    @Final
    public ParticleEngine particleEngine;

    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Final
    private DeltaTracker.Timer deltaTracker;

    /* Frame-resolution ticks, which the animation interpolation and the crosshair need and
     * ClientTickEvents cannot give. Absorbed from SimpleBedrockModel, which patched the same
     * method from its own mixin. */
    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", ordinal = 0, shift = At.Shift.BEFORE))
    private void tacz$renderTickStart(boolean tick, CallbackInfo ci) {
        RenderTickEvent.EVENT.invoker().post(new RenderTickEvent((Minecraft) (Object) this, RenderTickEvent.Phase.START, this.deltaTracker));
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;pop()V", ordinal = 4, shift = At.Shift.AFTER))
    private void tacz$renderTickEnd(boolean tick, CallbackInfo ci) {
        RenderTickEvent.EVENT.invoker().post(new RenderTickEvent((Minecraft) (Object) this, RenderTickEvent.Phase.END, this.deltaTracker));
    }

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V"))
    private void tacz$addPacks(GameConfig gameConfig, CallbackInfo ci) {
        AddPackFindersEvent event = new AddPackFindersEvent(PackType.CLIENT_RESOURCES, ((PackRepositoryExtension) this.getResourcePackRepository())::tacz$addPackFinder, false);
        AddPackFindersEvent.CALLBACK.invoker().onAddPackFinders(event);
    }

    @Inject(method = "clearClientLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;resetData()V"))
    private void tacz$disconnect(Screen screen, CallbackInfo ci) {
        ClientPlayerNetworkEvent.LOGGING_OUT.invoker().post(new ClientPlayerNetworkEvent.LoggingOut(
                this.gameMode, this.player,
                this.player != null && this.player.connection != null ? this.player.connection.getConnection() : null));
    }

    @Unique
    private InputEvent.InteractionKeyMappingTriggered tacz$onClickInput(int button, KeyMapping keyMapping, InteractionHand hand) {
        InputEvent.InteractionKeyMappingTriggered event = new InputEvent.InteractionKeyMappingTriggered(button, keyMapping, hand);
        InputEvent.InteractionKeyMappingTriggered.EVENT.invoker().onInteractionKeyMappingTriggered(event);
        return event;
    }

    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getDirection()Lnet/minecraft/core/Direction;"), cancellable = true)
    private void tacz$onClickInputEvent(boolean leftClick, CallbackInfo ci, @Local BlockHitResult blockHitResult, @Local BlockPos blockPos, @Share("event") LocalRef<InputEvent.InteractionKeyMappingTriggered> eventRef) {
        InputEvent.InteractionKeyMappingTriggered inputEvent = tacz$onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND);
        eventRef.set(inputEvent);
        if (inputEvent.isCanceled()) {
            if (inputEvent.shouldSwingHand()) {
                // ParticleEngine.crack 没了，方块碎屑现在归客户端世界管
                if (this.level != null) {
                    this.level.addDestroyBlockEffect(blockPos, this.level.getBlockState(blockPos));
                }
                this.player.swing(InteractionHand.MAIN_HAND);
            }
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;continueDestroyBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean tacz$checkHandSwing(boolean original, @Share("event") LocalRef<InputEvent.InteractionKeyMappingTriggered> eventRef) {
        return original && eventRef.get().shouldSwingHand();
    }

    @Inject(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/HitResult;getType()Lnet/minecraft/world/phys/HitResult$Type;"), cancellable = true)
    private void tacz$onAttackClickInputEvent(CallbackInfoReturnable<Boolean> cir, @Share("inputEvent") LocalRef<InputEvent.InteractionKeyMappingTriggered> inputEvent, @Local boolean flag) {
        inputEvent.set(tacz$onClickInput(0, this.options.keyAttack, InteractionHand.MAIN_HAND));

        if (inputEvent.get().isCanceled()) {
            if (inputEvent.get().shouldSwingHand())
                this.player.swing(InteractionHand.MAIN_HAND);

            cir.setReturnValue(flag);
        }
    }

    @WrapWithCondition(method = "startAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private boolean tacz$swingHandIfEventPermits(LocalPlayer instance, InteractionHand interactionHand, @Share("inputEvent") LocalRef<InputEvent.InteractionKeyMappingTriggered> inputEvent) {
        return inputEvent.get() == null || inputEvent.get().shouldSwingHand();
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;", ordinal = 0), cancellable = true)
    private void tacz$callForgeUseInputEvent(CallbackInfo ci, @Share("inputEvent") LocalRef<InputEvent.InteractionKeyMappingTriggered> inputEvent, @Local InteractionHand hand) {
        inputEvent.set(tacz$onClickInput(1, this.options.keyUse, hand));

        if (inputEvent.get().isCanceled()) {
            if (inputEvent.get().shouldSwingHand())
                this.player.swing(hand);

            ci.cancel();
        }
    }

    /* InteractionResult.shouldSwing() 没了，判断改成 result instanceof Success 且
     * swingSource() == CLIENT。把 swingSource 换成 SERVER，整个 if 分支（挥手 + itemUsed）
     * 就会一起跳过，和旧版把 shouldSwing 改成 false 的效果完全一样。*/
    @ModifyExpressionValue(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/InteractionResult$Success;swingSource()Lnet/minecraft/world/InteractionResult$SwingSource;"))
    private InteractionResult.SwingSource tacz$onlySwingHandIfNeeded(InteractionResult.SwingSource original, @Share("inputEvent") LocalRef<InputEvent.InteractionKeyMappingTriggered> inputEvent) {
        if (inputEvent.get() != null && !inputEvent.get().shouldSwingHand()) {
            return InteractionResult.SwingSource.SERVER;
        }
        return original;
    }

    /* pickBlock 被整个重写了：取物品的逻辑挪进了 MultiPlayerGameMode.handlePickItemFrom*，
     * 方法体里再也没有 Abilities.instabuild 这个字段访问。改挂在 hasControlDown 上，
     * 位置就是原来那个 MISS 提前返回之后的第一行，语义与旧注入点一致。*/
    @Inject(method = "pickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;hasControlDown()Z"), cancellable = true)
    private void tacz$callInteractionPickInput(CallbackInfo ci) {
        if (tacz$onClickInput(2, this.options.keyPickItem, InteractionHand.MAIN_HAND).isCanceled())
            ci.cancel();
    }
}
