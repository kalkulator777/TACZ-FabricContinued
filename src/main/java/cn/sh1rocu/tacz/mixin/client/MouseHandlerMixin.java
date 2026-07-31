package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.9 把 {@code onPress(long, int, int, int)} 换成了
 * {@code onButton(long, MouseButtonInfo, int)} —— 按钮和修饰键收进了一个记录，动作还是单独的 int。
 * 事件带的坐标取自鼠标的当前位置，和原版在这个方法里自己算 MouseButtonEvent 用的是同一份。
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private double xpos;
    @Shadow
    private double ypos;

    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getOverlay()Lnet/minecraft/client/gui/screens/Overlay;"), cancellable = true)
    private void tacz$onMouseButtonPre(long windowPointer, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        InputEvent.MouseButton.Pre event = new InputEvent.MouseButton.Pre(tacz$toEvent(buttonInfo), action);
        InputEvent.MouseButton.Pre.EVENT.invoker().onMousePre(event);

        if (event.isCanceled()) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("TAIL"))
    private void tacz$onMouseButtonPost(long windowPointer, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (windowPointer == this.minecraft.getWindow().handle()) {
            InputEvent.MouseButton.Post event = new InputEvent.MouseButton.Post(tacz$toEvent(buttonInfo), action);
            InputEvent.MouseButton.Post.EVENT.invoker().onMousePost(event);
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private MouseButtonEvent tacz$toEvent(MouseButtonInfo buttonInfo) {
        return new MouseButtonEvent(this.xpos, this.ypos, buttonInfo);
    }
}
