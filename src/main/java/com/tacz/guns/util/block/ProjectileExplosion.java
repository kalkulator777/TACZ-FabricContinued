package com.tacz.guns.util.block;

import com.google.common.collect.Lists;
import com.tacz.guns.config.common.AmmoConfig;
import com.tacz.guns.util.HitboxHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import java.util.LinkedHashSet;

/**
 * 子弹的爆炸。和原版的区别只有一处，也是这个类存在的理由：伤害衰减不按
 * {@link ServerExplosion#getSeenPercent} 算，而是从爆心向实体判定箱上的十五个点各做一次射线，
 * 取没有被方块挡住的最近距离。判定箱取的还是延迟补偿过的那一份。
 * <p>
 * 1.21.2 把 {@code Explosion} 拆成了接口和 {@link ServerExplosion}，爆炸的各个阶段
 * （算方块、伤害实体、破坏方块、点火）都收进了私有方法，只有 {@code explode()} 是公开的。
 * 所以这里改成整个覆盖 {@code explode()}，方块那一段照抄原版的写法。
 * {@code finalizeExplosion} 和 {@code getToBlow} 都没有了，破坏方块是 explode 的一部分。
 */
public class ProjectileExplosion extends ServerExplosion {
    private static final ExplosionDamageCalculator DEFAULT_CONTEXT = new ExplosionDamageCalculator();

    private final ServerLevel level;
    private final Vec3 center;
    private final float power;
    private final float radius;
    private final boolean knockback;
    private final Entity owner;
    private final Entity exploder;
    private final Explosion.BlockInteraction blockInteraction;
    private final ExplosionDamageCalculator damageCalculator;

    public ProjectileExplosion(ServerLevel level, Entity owner, Entity exploder, @Nullable DamageSource source,
                               @Nullable ExplosionDamageCalculator damageCalculator, Vec3 center,
                               float power, float radius, boolean knockback, Explosion.BlockInteraction mode) {
        super(level, exploder, source, damageCalculator, center, radius, AmmoConfig.EXPLOSIVE_AMMO_FIRE.get(), mode);
        this.level = level;
        this.center = center;
        this.power = power;
        this.radius = radius;
        this.owner = owner;
        this.exploder = exploder;
        this.blockInteraction = mode;
        this.damageCalculator = damageCalculator == null ? DEFAULT_CONTEXT : damageCalculator;
        this.knockback = knockback;
    }

    @Override
    public int explode() {
        this.level.gameEvent(this.exploder, GameEvent.EXPLODE, this.center);
        List<BlockPos> toBlow = this.calculateBlocks();
        this.hurtEntities();
        if (this.blockInteraction != Explosion.BlockInteraction.KEEP) {
            this.breakBlocks(toBlow);
        }
        return toBlow.size();
    }

    /**
     * 和原版 {@code calculateExplodedPositions} 是同一套：从爆心沿 16×16×16 立方体表面的方向
     * 各射一条线，按方块的爆炸抗性削减强度。
     */
    private List<BlockPos> calculateBlocks() {
        Set<BlockPos> set = new LinkedHashSet<>();
        int i = 16;

        for (int x = 0; x < i; ++x) {
            for (int y = 0; y < i; ++y) {
                for (int z = 0; z < i; ++z) {
                    if (x == 0 || x == i - 1 || y == 0 || y == i - 1 || z == 0 || z == i - 1) {
                        double d0 = ((float) x / (i - 1) * 2.0F - 1.0F);
                        double d1 = ((float) y / (i - 1) * 2.0F - 1.0F);
                        double d2 = ((float) z / (i - 1) * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 /= d3;
                        d1 /= d3;
                        d2 /= d3;
                        float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double blockX = this.center.x;
                        double blockY = this.center.y;
                        double blockZ = this.center.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos pos = BlockPos.containing(blockX, blockY, blockZ);
                            BlockState blockState = this.level.getBlockState(pos);
                            FluidState fluidState = this.level.getFluidState(pos);
                            if (!this.level.isInWorldBounds(pos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(this, this.level, pos, blockState, fluidState);
                            if (optional.isPresent()) {
                                f -= (optional.get() + f1) * f1;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(this, this.level, pos, blockState, f)) {
                                set.add(pos);
                            }

                            blockX += d0 * (double) f1;
                            blockY += d1 * (double) f1;
                            blockZ += d2 * (double) f1;
                        }
                    }
                }
            }
        }

        return Lists.newArrayList(set);
    }

    private void breakBlocks(List<BlockPos> toBlow) {
        List<ItemStack> drops = Lists.newArrayList();
        List<BlockPos> dropPositions = Lists.newArrayList();
        for (BlockPos pos : toBlow) {
            this.level.getBlockState(pos).onExplosionHit(this.level, pos, this, (stack, dropPos) -> {
                drops.add(stack);
                dropPositions.add(dropPos);
            });
        }
        for (int i = 0; i < drops.size(); i++) {
            Block.popResource(this.level, dropPositions.get(i), drops.get(i));
        }
    }

    private void hurtEntities() {
        float radius = this.radius;
        int minX = Mth.floor(this.center.x - (double) radius - 1.0D);
        int maxX = Mth.floor(this.center.x + (double) radius + 1.0D);
        int minY = Mth.floor(this.center.y - (double) radius - 1.0D);
        int maxY = Mth.floor(this.center.y + (double) radius + 1.0D);
        int minZ = Mth.floor(this.center.z - (double) radius - 1.0D);
        int maxZ = Mth.floor(this.center.z + (double) radius + 1.0D);
        radius *= 2;
        List<Entity> entities = this.level.getEntities(this.exploder, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        Vec3 explosionPos = this.center;

        for (Entity entity : entities) {
            if (entity.ignoreExplosion(this)) {
                continue;
            }

            AABB boundingBox = HitboxHelper.getFixedBoundingBox(entity, this.owner);
            BlockHitResult result;
            double strength;
            double deltaX;
            double deltaY;
            double deltaZ;
            double minDistance = radius;

            Vec3[] d = new Vec3[15];

            if (!(entity instanceof LivingEntity)) {
                strength = Math.sqrt(entity.distanceToSqr(explosionPos)) * 2 / radius;
                deltaX = entity.getX() - this.center.x;
                deltaY = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - this.center.y;
                deltaZ = entity.getZ() - this.center.z;
            } else {
                deltaX = (boundingBox.maxX + boundingBox.minX) / 2;
                deltaY = (boundingBox.maxY + boundingBox.minY) / 2;
                deltaZ = (boundingBox.maxZ + boundingBox.minZ) / 2;
                d[0] = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
                d[1] = new Vec3(boundingBox.minX, boundingBox.minY, boundingBox.maxZ);
                d[2] = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.minZ);
                d[3] = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.minZ);
                d[4] = new Vec3(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
                d[5] = new Vec3(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ);
                d[6] = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ);
                d[7] = new Vec3(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
                d[8] = new Vec3(boundingBox.minX, deltaY, deltaZ);
                d[9] = new Vec3(boundingBox.maxX, deltaY, deltaZ);
                d[10] = new Vec3(deltaX, boundingBox.minY, deltaZ);
                d[11] = new Vec3(deltaX, boundingBox.maxY, deltaZ);
                d[12] = new Vec3(deltaX, deltaY, boundingBox.minZ);
                d[13] = new Vec3(deltaX, deltaY, boundingBox.maxZ);
                d[14] = new Vec3(deltaX, deltaY, deltaZ);
                for (int s = 0; s < 15; s++) {
                    result = BlockRayTrace.rayTraceBlocks(this.level, new ClipContext(explosionPos, d[s], ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
                    minDistance = (result.getType() != BlockHitResult.Type.BLOCK) ? Math.min(minDistance, explosionPos.distanceTo(d[s])) : minDistance;
                }
                strength = minDistance * 2 / radius;
                deltaX -= this.center.x;
                deltaY -= this.center.y;
                deltaZ -= this.center.z;
            }

            if (strength > 1.0D) {
                continue;
            }

            double distanceToExplosion = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

            if (distanceToExplosion != 0.0D) {
                deltaX /= distanceToExplosion;
                deltaY /= distanceToExplosion;
                deltaZ /= distanceToExplosion;
            }

            double damage = 1.0D - strength;
            entity.hurt(this.getDamageSource(), (float) damage * this.power);

            if (entity instanceof LivingEntity livingEntity) {
                damage *= (1.0F - livingEntity.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
            }

            float multiplier = this.power * radius / 500;
            // 启用击退效果
            if (AmmoConfig.EXPLOSIVE_AMMO_KNOCK_BACK.get() && this.knockback) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(deltaX * damage * multiplier, deltaY * damage * multiplier, deltaZ * damage * multiplier));
                if (entity instanceof Player player) {
                    if (!player.isSpectator() && (!player.isCreative() || !player.getAbilities().flying)) {
                        this.getHitPlayers().put(player, new Vec3(deltaX * damage * multiplier, deltaY * damage * multiplier, deltaZ * damage * multiplier));
                    }
                }
            }
        }
    }
}
