package me.noramibu.processor;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * 引力奇点附魔处理器
 * 
 * 效果：将周围的怪物和掉落物拉向中心
 * - I级：5格范围，0.15引力强度
 * - II级：8格范围，0.25引力强度
 * - III级：12格范围，0.40引力强度
 */
public class GravityProcessor implements EnchantmentProcessor {
    
    private int tickCounter = 0;  // 处理间隔计数器
    private int particleCounter = 0;  // 粒子生成计数器
    private double particleRotation = 0;  // 粒子旋转角度
    
    /**
     * 配置效果云：传送门粒子（黑洞效果）
     */
    @Override
    public void applyToCloud(AreaEffectCloudEntity cloud, int level) {
        // 设置传送门粒子（紫色旋转）
        cloud.setParticleType(ParticleTypes.PORTAL);
    }
    
    /**
     * 每2 ticks处理一次引力效果
     */
    @Override
    public void process(ServerWorld world, AreaEffectCloudEntity cloud, int level) {
        tickCounter++;
        if (tickCounter < 2) {
            return;  // 还没到处理时间
        }
        tickCounter = 0;
        
        // 获取引力参数
        double pullRadius;
        double pullStrength;
        
        switch (level) {
            case 1 -> { pullRadius = 5.0;  pullStrength = 0.15; }
            case 2 -> { pullRadius = 8.0;  pullStrength = 0.25; }
            case 3 -> { pullRadius = 12.0; pullStrength = 0.40; }
            default -> { pullRadius = 5.0; pullStrength = 0.15; }
        }
        
        Vec3d singularityPos = new Vec3d(cloud.getX(), cloud.getY(), cloud.getZ());
        
        // 应用引力到怪物
        applyGravityToMobs(world, singularityPos, pullRadius, pullStrength);
        
        // 应用引力到物品
        applyGravityToItems(world, singularityPos, pullRadius, pullStrength * 0.5);
        
        // 生成黑洞粒子效果
        particleCounter++;
        if (particleCounter >= 2) {
            spawnBlackHoleParticles(world, singularityPos);
            particleCounter = 0;
        }
    }
    
    /**
     * 对怪物应用引力
     */
    private void applyGravityToMobs(ServerWorld world, Vec3d singularityPos, double pullRadius, double pullStrength) {
        Box searchBox = new Box(
            singularityPos.x - pullRadius, singularityPos.y - pullRadius, singularityPos.z - pullRadius,
            singularityPos.x + pullRadius, singularityPos.y + pullRadius, singularityPos.z + pullRadius
        );
        
        List<HostileEntity> entities = world.getEntitiesByClass(
            HostileEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved()
        );
        
        // 限制处理数量
        int maxProcessed = Math.min(entities.size(), 30);
        
        for (int i = 0; i < maxProcessed; i++) {
            LivingEntity entity = entities.get(i);
            applyGravityForce(entity, singularityPos, pullRadius, pullStrength);
        }
    }
    
    /**
     * 对物品应用引力
     */
    private void applyGravityToItems(ServerWorld world, Vec3d singularityPos, double pullRadius, double pullStrength) {
        Box searchBox = new Box(
            singularityPos.x - pullRadius, singularityPos.y - pullRadius, singularityPos.z - pullRadius,
            singularityPos.x + pullRadius, singularityPos.y + pullRadius, singularityPos.z + pullRadius
        );
        
        List<ItemEntity> items = world.getEntitiesByClass(
            ItemEntity.class,
            searchBox,
            item -> !item.isRemoved()
        );
        
        // 限制处理数量
        int maxProcessed = Math.min(items.size(), 50);
        
        for (int i = 0; i < maxProcessed; i++) {
            ItemEntity item = items.get(i);
            applyGravityForce(item, singularityPos, pullRadius, pullStrength);
        }
    }
    
    /**
     * 对单个实体应用引力
     */
    private void applyGravityForce(net.minecraft.entity.Entity entity, Vec3d singularityPos, double pullRadius, double pullStrength) {
        Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        Vec3d toSingularity = singularityPos.subtract(entityPos);
        double distance = toSingularity.length();
        
        // 距离太近时不拉取（事件视界）
        if (distance < 0.5) {
            return;
        }
        
        // 计算引力强度（平方反比定律）
        double distanceFactor = 1.0 - (distance / pullRadius);
        if (distanceFactor <= 0) return;
        
        double actualPullStrength = pullStrength * distanceFactor * distanceFactor;
        
        // 应用速度
        Vec3d pullDirection = toSingularity.normalize();
        Vec3d currentVelocity = entity.getVelocity();
        Vec3d newVelocity = currentVelocity.add(pullDirection.multiply(actualPullStrength));
        
        // 限制最大速度
        double maxSpeed = 0.8;
        if (newVelocity.length() > maxSpeed) {
            newVelocity = newVelocity.normalize().multiply(maxSpeed);
        }
        
        entity.setVelocity(newVelocity);
        entity.velocityModified = true;
    }
    
    /**
     * 生成黑洞粒子效果
     */
    private void spawnBlackHoleParticles(ServerWorld world, Vec3d pos) {
        // 旋转角度递增
        particleRotation += 0.1;
        
        // 传送门粒子环（紫色旋转）
        double pulsatingRadius = 1.0 + Math.sin(particleRotation) * 0.3;
        for (int i = 0; i < 5; i++) {
            double angle = particleRotation + (i * Math.PI * 2 / 5);
            double x = pos.x + Math.cos(angle) * pulsatingRadius;
            double z = pos.z + Math.sin(angle) * pulsatingRadius;
            
            world.spawnParticles(
                ParticleTypes.PORTAL,
                x, pos.y, z,
                1, 0.0, 0.1, 0.0, 0.0
            );
        }
        
        // 中心黑色烟雾
        world.spawnParticles(
            ParticleTypes.LARGE_SMOKE,
            pos.x, pos.y, pos.z,
            2, 0.1, 0.1, 0.1, 0.0
        );
    }
}
