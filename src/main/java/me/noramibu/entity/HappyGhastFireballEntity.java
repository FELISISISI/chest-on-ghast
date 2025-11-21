package me.noramibu.entity;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 自定义火球实体
 * 在保留原版火球爆炸、粒子和同步逻辑的前提下，允许我们注入额外的等级系数伤害
 */
public class HappyGhastFireballEntity extends FireballEntity {
    private static final float BASE_VANILLA_DAMAGE = 6.0f;
    private final float scaledDamage;

    public HappyGhastFireballEntity(World world, LivingEntity owner, Vec3d direction, int explosionPower, float scaledDamage) {
        super(world, owner, direction, explosionPower);
        this.scaledDamage = scaledDamage;
    }

    @Override
    protected void onEntityHit(EntityHitResult entityHitResult) {
        float extraDamage = Math.max(0.0f, this.scaledDamage - BASE_VANILLA_DAMAGE);
        super.onEntityHit(entityHitResult);

        if (extraDamage <= 0) {
            return;
        }

        World world = this.getEntityWorld();
        if (!(world instanceof ServerWorld serverWorld)) {
            return;
        }

        Entity target = entityHitResult.getEntity();
        Entity owner = this.getOwner();
        DamageSource source = this.getDamageSources().fireball(this, owner);

        target.damage(serverWorld, source, extraDamage);
        EnchantmentHelper.onTargetDamaged(serverWorld, target, source);
    }
}
