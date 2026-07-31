package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.InputEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    /**
     * keyPress 现在是 {@code (long, int action, KeyEvent)} —— 键码、扫描码和修饰键
     * 收进了 KeyEvent，动作单独留在外面，而且参数顺序变了。
     */
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void tacz$onKey(long window, int action, KeyEvent keyEvent, CallbackInfo ci) {
        if (window == this.minecraft.getWindow().handle()) {
            InputEvent.Key.EVENT.invoker().onKey(new InputEvent.Key(keyEvent, action));
        }
    }
}
