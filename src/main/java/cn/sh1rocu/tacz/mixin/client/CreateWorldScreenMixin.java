package cn.sh1rocu.tacz.mixin.client;

import cn.sh1rocu.tacz.api.event.AddPackFindersEvent;
import cn.sh1rocu.tacz.api.mixin.PackRepositoryExtension;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldCallback;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContextMapper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.WorldDataConfiguration;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {
    @Shadow
    @Nullable
    private PackRepository tempDataPackRepository;

    /* openFresh 已经被拆开了：它只负责转发，真正建 PackRepository 的是私有的
     * openCreateWorldScreen —— testWorld 和「新建世界」都汇到这里，挂在这一处就够。*/
    @Inject(method = "openCreateWorldScreen",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createDefaultLoadConfig(Lnet/minecraft/server/packs/repository/PackRepository;Lnet/minecraft/world/level/WorldDataConfiguration;)Lnet/minecraft/server/WorldLoader$InitConfig;"))
    private static void tacz$addPacks(Minecraft minecraft, Runnable onClose, Function<?, ?> worldGenSettings,
                                      WorldCreationContextMapper contextMapper, ResourceKey<?> preset,
                                      CreateWorldCallback callback, CallbackInfo ci, @Local PackRepository repository) {
        AddPackFindersEvent event = new AddPackFindersEvent(PackType.SERVER_DATA, ((PackRepositoryExtension) repository)::tacz$addPackFinder, false);
        AddPackFindersEvent.CALLBACK.invoker().onAddPackFinders(event);
    }

    @Inject(method = "getDataPackSelectionSettings", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;reload()V"))
    private void tacz$addPacks(WorldDataConfiguration worldDataConfiguration, CallbackInfoReturnable<Pair<Path, PackRepository>> cir) {
        assert tempDataPackRepository != null;
        AddPackFindersEvent event = new AddPackFindersEvent(PackType.SERVER_DATA, ((PackRepositoryExtension) this.tempDataPackRepository)::tacz$addPackFinder, false);
        AddPackFindersEvent.CALLBACK.invoker().onAddPackFinders(event);
    }
}