package com.tacz.guns.util;

import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.util.block.ProjectileExplosion;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ExplodeUtil {
    /**
     * 爆炸后落下的方块碎屑，1.21.11 新增的包字段。原版有一份同样组合的默认值，但它是私有的。
     */
    private static final WeightedList<ExplosionParticleInfo> BLOCK_PARTICLES = WeightedList.<ExplosionParticleInfo>builder()
            .add(new ExplosionParticleInfo(ParticleTypes.POOF, 0.5F, 1.0F))
            .add(new ExplosionParticleInfo(ParticleTypes.SMOKE, 1.0F, 1.0F))
            .build();

    public static void createExplosion(Entity owner, Entity exploder, float damage, float radius, boolean knockback, boolean destroy, Vec3 hitPos) {
        // 客户端不执行
        if (!(exploder.level() instanceof ServerLevel level)) {
            return;
        }
        // 依据配置文件读取方块破坏方式
        Explosion.BlockInteraction mode;
        if (destroy) {
            mode = Explosion.BlockInteraction.DESTROY;
        } else {
            mode = Explosion.BlockInteraction.KEEP;
        }
        // 创建爆炸
        ProjectileExplosion explosion = new ProjectileExplosion(level, owner, exploder, null, null, hitPos, damage, radius, knockback, mode);
        /* 1.21.2 起 explode() 就是全部 —— 算方块、伤害实体、破坏方块、点火，返回受影响的方块数。
         * finalizeExplosion 和 clearToBlow 都不在了，KEEP 模式下 explode 本来就不碰方块。*/
        int blockCount = explosion.explode();
        // 客户端发包，发送爆炸相关信息
        level.players().stream().filter(player -> Mth.sqrt((float) player.distanceToSqr(hitPos)) < AmmoConfig.EXPLOSIVE_AMMO_VISIBLE_DISTANCE.get()).forEach(player -> {
            ClientboundExplodePacket packet = new ClientboundExplodePacket(hitPos, radius, blockCount,
                    Optional.ofNullable(explosion.getHitPlayers().get(player)),
                    ParticleTypes.EXPLOSION, SoundEvents.GENERIC_EXPLODE, BLOCK_PARTICLES);
            player.connection.send(packet);
        });
    }
}
