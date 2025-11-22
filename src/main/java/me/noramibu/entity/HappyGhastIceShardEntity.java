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

import java.util.List;

/**
 * 冰属性攻击：命中后造成伤害并附加大幅减速
 */
public class HappyGhastIceShardEntity extends SmallFireballEntity {
    private final float impactDamage;
    private final float controlStrength;

    public HappyGhastIceShardEntity(World world, LivingEntity owner, Vec3d direction, float impactDamage, float controlStrength) {
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
        Entity owner = this.getOwner();
        if (target instanceof LivingEntity living) {
            float damage = this.impactDamage;
            living.damage(((ServerWorld) this.getEntityWorld()), this.getDamageSources().freeze(), damage);

            int duration = (int) (60 * this.controlStrength);
            int amplifier = Math.max(0, Math.round(this.controlStrength) - 1);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amplifier), owner instanceof LivingEntity livingOwner ? livingOwner : null);
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, duration, Math.max(0, amplifier - 1)), owner instanceof LivingEntity livingOwner ? livingOwner : null);
        }

        spawnImpactParticles(entityHitResult.getPos());
        applyFrostArea(entityHitResult.getPos());
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getEntityWorld().isClient()) {
            applyFrostArea(Vec3d.ofCenter(blockHitResult.getBlockPos()));
            this.discard();
        }
        spawnImpactParticles(blockHitResult.getPos());
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (!this.getEntityWorld().isClient()) {
            this.discard();
        }
    }

    private void spawnImpactParticles(Vec3d pos) {
        if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y, pos.z, 12, 0.4, 0.3, 0.4, 0.01);
        }
    }

    private void applyFrostArea(Vec3d center) {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        double radius = 2.5 + (this.controlStrength * 0.5);
        List<MobEntity> mobs = serverWorld.getEntitiesByClass(
            MobEntity.class,
            new Box(
                center.x - radius, center.y - 0.5, center.z - radius,
                center.x + radius, center.y + 1.0, center.z + radius
            ),
            entity -> entity.isAlive()
        );

        int duration = (int) (40 * this.controlStrength);
        int amplifier = Math.max(0, Math.round(this.controlStrength));
        for (MobEntity mob : mobs) {
            mob.damage(serverWorld, this.getDamageSources().freeze(), this.impactDamage * 0.4f);
            mob.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration, amplifier), this.getOwner() instanceof LivingEntity living ? living : null);
        }

        serverWorld.spawnParticles(ParticleTypes.SNOWFLAKE, center.x, center.y, center.z, 20, 0.7, 0.1, 0.7, 0.01);
    }
}
