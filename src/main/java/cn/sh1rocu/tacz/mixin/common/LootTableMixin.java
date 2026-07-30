package cn.sh1rocu.tacz.mixin.common;

import com.tacz.guns.loot.LootTableInjectorModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(LootTable.class)
public class LootTableMixin {
    /* 每个取战利品的公开入口最后都汇到这一个方法上，所以注入挂在这里只会执行一次。
     *
     * 以前挂在私有的 getRandomItems(LootContext) 上。容器填充、方块掉落和钓鱼确实经过它，
     * 但生物掉落走的是 getRandomItems(LootParams, long, Consumer)，那条路直接调 raw，
     * 于是注入到生物战利品表里的东西一件也不会掉。 */
    @Inject(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V",
            at = @At("TAIL"))
    private void tacz$injectLoot(LootContext context, Consumer<ItemStack> output, CallbackInfo ci) {
        LootTableInjectorModifier.appendInjections((LootTable) (Object) this, context, output);
    }
}
