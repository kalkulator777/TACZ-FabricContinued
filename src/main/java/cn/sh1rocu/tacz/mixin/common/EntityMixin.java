package cn.sh1rocu.tacz.mixin.common;

import cn.sh1rocu.tacz.api.extension.IEntityPersistentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class EntityMixin implements IEntityPersistentData {
    @Shadow
    private Level level;

    @Unique
    private CompoundTag tacz$persistentData;

    @Unique
    @Override
    public CompoundTag tacz$getPersistentData() {
        if (this.tacz$persistentData == null) {
            this.tacz$persistentData = new CompoundTag();
        }
        return tacz$persistentData;
    }

    /**
     * 1.21.6 起实体存读走的是 ValueOutput / ValueInput，只认编解码器，也不再直接给出 CompoundTag。
     * 这一坨本来就是一整块原样保存的 NBT，用 CompoundTag.CODEC 存回去即可。
     */
    @Inject(method = "saveWithoutId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V"))
    private void tacz$savePersistentData(ValueOutput output, CallbackInfo ci) {
        if (this.tacz$persistentData != null) {
            output.store("NeoForgeData", CompoundTag.CODEC, this.tacz$persistentData.copy());
        }
    }

    @Inject(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V"))
    private void tacz$loadPersistentData(ValueInput input, CallbackInfo ci) {
        input.read("NeoForgeData", CompoundTag.CODEC).ifPresent(data -> tacz$persistentData = data);
    }
}