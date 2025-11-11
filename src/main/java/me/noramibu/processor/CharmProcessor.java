package me.noramibu.processor;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * 魅惑附魔处理器
 * 
 * 效果：让效果云范围内的怪物互相攻击
 * - I级：每次造成2.0伤害（1颗心）
 * - II级：每次造成4.0伤害（2颗心）
 * - III级：每次造成6.0伤害（3颗心）
 */
public class CharmProcessor implements EnchantmentProcessor {
    
    private int tickCounter = 0;  // 处理间隔计数器
    
    /**
     * 配置效果云：紫色女巫粒子
     */
    @Override
    public void applyToCloud(AreaEffectCloudEntity cloud, int level) {
        // 设置紫色魔法粒子
        cloud.setParticleType(ParticleTypes.WITCH);
    }
    
    /**
     * 每10 ticks处理一次魅惑效果
     */
    @Override
    public void process(ServerWorld world, AreaEffectCloudEntity cloud, int level) {
        tickCounter++;
        if (tickCounter < 10) {
            return;  // 还没到处理时间
        }
        tickCounter = 0;
        
        // 获取效果云范围内的所有敌对生物
        Vec3d cloudPos = new Vec3d(cloud.getX(), cloud.getY(), cloud.getZ());
        float radius = cloud.getRadius();
        
        Box searchBox = new Box(
            cloudPos.x - radius, cloudPos.y - radius, cloudPos.z - radius,
            cloudPos.x + radius, cloudPos.y + radius, cloudPos.z + radius
        );
        
        List<HostileEntity> hostiles = world.getEntitiesByClass(
            HostileEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved()
        );
        
        // 至少需要2只怪物才能互相攻击
        if (hostiles.size() < 2) {
            return;
        }
        
        // 计算伤害量
        float damageAmount = switch (level) {
            case 1 -> 2.0f;
            case 2 -> 4.0f;
            case 3 -> 6.0f;
            default -> 2.0f;
        };
        
        // 限制处理数量（性能优化）
        int maxProcessed = Math.min(hostiles.size(), 10);
        
        // 让每个怪物攻击另一个随机怪物
        for (int i = 0; i < maxProcessed; i++) {
            HostileEntity attacker = hostiles.get(i);
            
            // 随机选择一个目标（不是自己）
            List<HostileEntity> potentialTargets = new ArrayList<>(hostiles);
            potentialTargets.remove(attacker);
            
            if (!potentialTargets.isEmpty()) {
                HostileEntity target = potentialTargets.get(world.getRandom().nextInt(potentialTargets.size()));
                
                // 造成伤害（使用mobAttack伤害源）
                target.damage(world, world.getDamageSources().mobAttack(attacker), damageAmount);
                
                // 显示愤怒粒子
                world.spawnParticles(
                    ParticleTypes.ANGRY_VILLAGER,
                    attacker.getX(),
                    attacker.getY() + attacker.getHeight() / 2,
                    attacker.getZ(),
                    3, 0.3, 0.3, 0.3, 0.0
                );
            }
        }
    }
}
