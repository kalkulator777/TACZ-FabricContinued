package cn.sh1rocu.tacz.mixin.client;

import com.tacz.guns.client.event.PreventsHotbarEvent;
import com.tacz.guns.client.event.RenderCrosshairEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(Gui.class)
public class GuiMixin {
    @Inject(method = "renderHotbarAndDecorations", at = @At("HEAD"), cancellable = true)
    private void tacz$onRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        PreventsHotbarEvent.onRenderHotbarEvent(cancelled);
        if (cancelled.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void tacz$renderCrosshairPre(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        RenderCrosshairEvent.onRenderOverlay(guiGraphics, Minecraft.getInstance().getWindow(), deltaTracker, ci);
    }
}
