package com.tacz.guns.client.particle;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.init.ModBlocks;
import com.tacz.guns.particles.BulletHoleOption;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Author: Forked from MrCrayfish, continued by Timeless devs
 */
public class BulletHoleParticle extends SingleQuadParticle {
    private final Direction direction;
    private final BlockPos pos;
    private int uOffset;
    private int vOffset;
    private float textureDensity;

    public BulletHoleParticle(ClientLevel world, double x, double y, double z, Direction direction, BlockPos pos, String ammoId, String gunId, String gunDisplayId) {
        super(world, x, y, z, getSprite(pos));
        // uv 偏移要用 random，super 的构造里还拿不到，所以在这里再走一次 setSprite
        this.setSprite(this.sprite);
        this.direction = direction;
        this.pos = pos;
        this.lifetime = this.getLifetimeFromConfig(world);
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = 0.05F;

        BlockState state = world.getBlockState(pos);
        if (state.is(ModBlocks.TARGET) || shouldRemove()) {
            this.remove();
        }
        TimelessAPI.getGunDisplay(Identifier.parse(gunDisplayId), Identifier.parse(gunId)).ifPresent(gunIndex -> {
            float[] gunTracerColor = gunIndex.getTracerColor();
            if (gunTracerColor != null) {
                this.rCol = gunTracerColor[0];
                this.gCol = gunTracerColor[1];
                this.bCol = gunTracerColor[2];
            } else {
                TimelessAPI.getClientAmmoIndex(Identifier.parse(ammoId)).ifPresent(ammoIndex -> {
                    float[] ammoTracerColor = ammoIndex.getTracerColor();
                    this.rCol = ammoTracerColor[0];
                    this.gCol = ammoTracerColor[1];
                    this.bCol = ammoTracerColor[2];
                });
            }
        });
        this.alpha = 0.9F;
    }

    private int getLifetimeFromConfig(ClientLevel world) {
        int configLife = RenderConfig.BULLET_HOLE_PARTICLE_LIFE.get();
        if (configLife <= 1) {
            return configLife;
        }
        return configLife + world.random.nextInt(configLife / 2);
    }

    @Override
    protected void setSprite(TextureAtlasSprite sprite) {
        super.setSprite(sprite);
        this.uOffset = this.random.nextInt(16);
        this.vOffset = this.random.nextInt(16);
        // 材质应该都是方形
        this.textureDensity = (sprite.getU1() - sprite.getU0()) / 16.0F;
    }

    private static TextureAtlasSprite getSprite(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        BlockState state = minecraft.level != null ? minecraft.level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
        return minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(state);
    }

    @Override
    protected float getU0() {
        return this.sprite.getU0() + this.uOffset * this.textureDensity;
    }

    @Override
    protected float getV0() {
        return this.sprite.getV0() + this.vOffset * this.textureDensity;
    }

    @Override
    protected float getU1() {
        return this.getU0() + this.textureDensity;
    }

    @Override
    protected float getV1() {
        return this.getV0() + this.textureDensity;
    }

    @Override
    public void tick() {
        super.tick();
        if (shouldRemove()) {
            this.remove();
        }
    }

    /**
     * 1.21.9 起粒子不再自己往顶点缓冲里画，而是把一份「四边形状态」交出去，统一提交。
     * QuadParticleRenderState 只收「位置 + 四元数 + 边长 + uv + 颜色 + 亮度」，
     * 它拼出来的四边形躺在 XY 平面（角点 (±1, ±1, 0)），而旧代码那份躺在 XZ 平面
     * （角点 (±1, 0.01, ±1)），Direction.getRotation() 也是照 XZ 那份写的。
     * 所以这里先绕 X 轴转 -90°，把 XY 掰成 XZ，再套 direction 的旋转。
     * <p>
     * 防 z-fight 的那 0.01 没法塞进四元数，改成沿法线把整个粒子挪出去 —— 数学上和旧代码等价。
     * uv 也跟着 XY→XZ 的翻转对调过，保证贴图朝向不变。
     */
    @Override
    public void extract(QuadParticleRenderState renderState, Camera camera, float partialTicks) {
        Vec3 view = camera.position();
        float scale = this.getQuadSize(partialTicks);

        Quaternionf rotation = this.direction.getRotation();
        Vector3f offset = new Vector3f(0.0F, 0.01F, 0.0F).rotate(rotation).mul(scale);
        Quaternionf quadRotation = new Quaternionf(rotation).rotateX((float) (-Math.PI / 2));

        float particleX = (float) (Mth.lerp(partialTicks, this.xo, this.x) - view.x()) + offset.x();
        float particleY = (float) (Mth.lerp(partialTicks, this.yo, this.y) - view.y()) + offset.y();
        float particleZ = (float) (Mth.lerp(partialTicks, this.zo, this.z) - view.z()) + offset.z();

        // 0 - 30 tick 内，从 15 亮度到 0 亮度
        int light = Math.max(15 - this.age / 2, 0);
        int lightColor = LightTexture.pack(light, light);

        // 颜色，逐渐渐变到 0 0 0，也就是黑色
        float colorPercent = light / 15.0f;
        float red = this.rCol * colorPercent;
        float green = this.gCol * colorPercent;
        float blue = this.bCol * colorPercent;

        // 透明度，逐渐变成 0，也就是透明
        double threshold = RenderConfig.BULLET_HOLE_PARTICLE_FADE_THRESHOLD.get() * this.lifetime;
        float fade = 1.0f - (float) (Math.max(this.age - threshold, 0) / (this.lifetime - threshold));
        float alphaFade = this.alpha * fade;

        renderState.add(this.getLayer(),
                particleX, particleY, particleZ,
                quadRotation.x, quadRotation.y, quadRotation.z, quadRotation.w,
                scale,
                this.getU1(), this.getU0(), this.getV1(), this.getV0(),
                ARGB.colorFromFloat(alphaFade, red, green, blue), lightColor);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return this.sprite.atlasLocation().equals(TextureAtlas.LOCATION_BLOCKS)
                ? SingleQuadParticle.Layer.TERRAIN
                : SingleQuadParticle.Layer.ITEMS;
    }

    private boolean shouldRemove() {
        final BlockState blockState = this.level.getBlockState(this.pos);
        if (blockState.isAir()) {
            return true;
        } else {
            // 阻止弹孔在与方块不构成有效附着时继续渲染
            VoxelShape shape = blockState.getCollisionShape(this.level, this.pos);
            if (shape.isEmpty()) {
                return true;
            }
            AABB baseBlockBoundingBox = shape.bounds();
            AABB blockBoundingBox = baseBlockBoundingBox.move(this.pos);
            boolean intersects = blockBoundingBox.intersects(
                    this.x - 0.1, this.y - 0.1, this.z - 0.1,
                    this.x + 0.1, this.y + 0.1, this.z + 0.1);
            return !intersects;
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleProvider<BulletHoleOption> {
        public Provider() {
        }

        @Override
        public BulletHoleParticle createParticle(@NotNull BulletHoleOption option, @NotNull ClientLevel world, double x, double y, double z, double pXSpeed, double pYSpeed, double pZSpeed, @NotNull RandomSource random) {
            return new BulletHoleParticle(world, x, y, z, option.getDirection(), option.getPos(), option.getAmmoId(), option.getGunId(), option.getGunDisplayId());
        }
    }
}
