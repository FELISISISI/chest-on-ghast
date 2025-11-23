package me.noramibu.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
 * 风属性攻击：超强击退
 */
public class HappyGhastWindProjectileEntity extends SmallFireballEntity {
    private final float impactDamage;
    private final float controlStrength;

    public HappyGhastWindProjectileEntity(World world, LivingEntity owner, Vec3d direction, float impactDamage, float controlStrength) {
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
        if (target instanceof LivingEntity living) {
            if (this.getEntityWorld() instanceof ServerWorld serverWorld) {
                var source = shooter != null ? this.getDamageSources().mobProjectile(this, shooter) : this.getDamageSources().magic();
                living.damage(serverWorld, source, this.impactDamage);
            }
            applyKnockback(living, this.controlStrength * 1.5);
        }

        spawnGustParticles(entityHitResult.getPos());
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getEntityWorld().isClient()) {
            applyAreaKnockback(Vec3d.ofCenter(blockHitResult.getBlockPos()));
            this.discard();
        }
        spawnGustParticles(blockHitResult.getPos());
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        if (!this.getEntityWorld().isClient()) {
            this.discard();
        }
    }

    private void applyKnockback(LivingEntity target, double force) {
        Vec3d direction = new Vec3d(
            target.getX() - this.getX(),
            target.getBodyY(0.5) - this.getY(),
            target.getZ() - this.getZ()
        );
        if (direction.lengthSquared() < 1.0E-4) {
            direction = new Vec3d(0, 1, 0);
        }
        direction = direction.normalize();
        target.addVelocity(direction.x * force, 0.35 * this.controlStrength, direction.z * force);
        target.velocityDirty = true;
    }

    private void applyAreaKnockback(Vec3d center) {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        double radius = 3.0 + this.controlStrength;
        List<MobEntity> mobs = serverWorld.getEntitiesByClass(
            MobEntity.class,
            new Box(
                center.x - radius, center.y - 0.5, center.z - radius,
                center.x + radius, center.y + 1.0, center.z + radius
            ),
            entity -> entity.isAlive()
        );

        for (MobEntity mob : mobs) {
            Vec3d dir = new Vec3d(mob.getX() - center.x, 0, mob.getZ() - center.z);
            if (dir.lengthSquared() < 1.0E-4) {
                dir = new Vec3d(0, 1, 0);
            }
            dir = dir.normalize();
            double force = 1.0 + (this.controlStrength * 0.6);
            mob.addVelocity(dir.x * force, 0.3 * this.controlStrength, dir.z * force);
            mob.velocityDirty = true;
        }
    }

    private void spawnGustParticles(Vec3d pos) {
        World world = this.getEntityWorld();
        if (world instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z, 16, 0.5, 0.2, 0.5, 0.01);
        }
    }
}
