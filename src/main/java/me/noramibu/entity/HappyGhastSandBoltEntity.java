package me.noramibu.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.SmallFireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.AreaEffectCloudEntity;

import java.util.List;

/**
 * 炽沙属性：沙尘迷雾，扰乱怪物
 */
public class HappyGhastSandBoltEntity extends SmallFireballEntity {
    private final float impactDamage;
    private final float controlStrength;

    public HappyGhastSandBoltEntity(World world, LivingEntity owner, Vec3d direction, float impactDamage, float controlStrength) {
        super(world, owner, direction);
        this.impactDamage = impactDamage;
        this.controlStrength = controlStrength;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        if (this.getEntityWorld().isClient()) {
            return;
        }

        Entity target = entityHitResult.getEntity();
        LivingEntity shooter = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        if (target instanceof LivingEntity living && this.getEntityWorld() instanceof ServerWorld serverWorld) {
            var source = shooter != null ? this.getDamageSources().mobProjectile(this, shooter) : this.getDamageSources().magic();
            living.damage(serverWorld, source, this.impactDamage);
        }

        spawnSandCloud(entityHitResult.getPos());
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getEntityWorld().isClient()) {
            spawnSandCloud(Vec3d.ofCenter(blockHitResult.getBlockPos()));
            this.discard();
        }
        spawnSandParticles(blockHitResult.getPos());
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (!this.getEntityWorld().isClient()) {
            this.discard();
        }
    }

    private void spawnSandCloud(Vec3d center) {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(serverWorld, center.x, center.y, center.z);
        cloud.setOwner(this.getOwner() instanceof LivingEntity living ? living : null);
        cloud.setParticleType(ParticleTypes.ASH);
        float radius = 3.0f + (this.controlStrength * 0.5f);
        cloud.setRadius(radius);
        cloud.setDuration((int) (80 * this.controlStrength));
        cloud.setRadiusGrowth(-radius / cloud.getDuration());
        cloud.addEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, (int) (40 * this.controlStrength), 0));
        cloud.addEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, (int) (60 * this.controlStrength), 1));
        serverWorld.spawnEntity(cloud);

        List<MobEntity> mobs = serverWorld.getEntitiesByClass(
            MobEntity.class,
            new Box(
                center.x - radius, center.y - 0.5, center.z - radius,
                center.x + radius, center.y + 1.0, center.z + radius
            ),
            entity -> entity.isAlive()
        );

        LivingEntity shooter = this.getOwner() instanceof LivingEntity livingOwner ? livingOwner : null;
        for (MobEntity mob : mobs) {
            var source = shooter != null ? this.getDamageSources().mobProjectile(this, shooter) : this.getDamageSources().magic();
            mob.damage(serverWorld, source, this.impactDamage * 0.3f);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, (int) (40 * this.controlStrength), 1), shooter);
        }

        spawnSandParticles(center);
    }

    private void spawnSandParticles(Vec3d pos) {
        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y, pos.z, 24, 0.8, 0.3, 0.8, 0.01);
        }
    }
}
