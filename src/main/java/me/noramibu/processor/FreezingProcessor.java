package me.noramibu.processor;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * 冰冻附魔处理器
 * 
 * 效果：冻住效果云范围内的怪物
 * - I级：冻结3秒，缓慢V
 * - II级：冻结5秒，缓慢VII
 * - III级：冻结8秒，缓慢X
 */
public class FreezingProcessor implements EnchantmentProcessor {
    
    /**
     * 配置效果云：雪花粒子 + 缓慢效果
     */
    @Override
    public void applyToCloud(AreaEffectCloudEntity cloud, int level) {
        // 设置雪花粒子
        cloud.setParticleType(ParticleTypes.SNOWFLAKE);
        
        // 计算参数
        int duration;
        int amplifier;
        
        switch (level) {
            case 1 -> { duration = 60;  amplifier = 4; }  // 3秒，缓慢V
            case 2 -> { duration = 100; amplifier = 6; }  // 5秒，缓慢VII
            case 3 -> { duration = 160; amplifier = 9; }  // 8秒，缓慢X
            default -> { duration = 60; amplifier = 4; }
        }
        
        // 添加缓慢效果
        cloud.addEffect(new StatusEffectInstance(
            StatusEffects.SLOWNESS,
            duration,
            amplifier,
            false, true
        ));
        
        // 添加挖掘疲劳效果（防止快速攻击）
        cloud.addEffect(new StatusEffectInstance(
            StatusEffects.MINING_FATIGUE,
            duration,
            amplifier,
            false, true
        ));
        
        // 添加速度提升给玩家（部分补偿）
        cloud.addEffect(new StatusEffectInstance(
            StatusEffects.SPEED,
            duration,
            amplifier / 2,
            false, true
        ));
    }
    
    /**
     * 冰冻不需要持续处理（状态效果自动生效）
     */
    @Override
    public void process(ServerWorld world, AreaEffectCloudEntity cloud, int level) {
        // 冰冻效果由状态效果系统自动处理
    }
}
