package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.TextureStitchEvent;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureAtlas.class)
public class TextureAtlasMixin {
    @Inject(method = "upload", at = @At("TAIL"))
    private void tacz$uploadPost(SpriteLoader.Preparations preparations, CallbackInfo ci) {
        TextureStitchEvent.POST.invoker().post(new TextureStitchEvent.Post((TextureAtlas) (Object) this));
    }
}
