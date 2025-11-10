package me.noramibu.processor;

import me.noramibu.enchantment.FireballEnchantment;
import me.noramibu.system.EnchantmentSystem;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 持久附魔处理器
 * 
 * 效果：延长效果云的持续时间
 * - I级：x1.5倍
 * - II级：x2倍
 * - III级：x3倍
 */
public class DurationProcessor implements EnchantmentProcessor {
    
    /**
     * 持久不需要效果云处理（在创建时已应用）
     */
    @Override
    public void applyToCloud(AreaEffectCloudEntity cloud, int level) {
        // 持久效果在EffectCloudSystem.onFireballHit中已处理
    }
    
    /**
     * 持久不需要持续处理
     */
    @Override
    public void process(ServerWorld world, AreaEffectCloudEntity cloud, int level) {
        // 持久不需要持续处理
    }
    
    /**
     * 计算增强后的持续时间
     * 
     * @param ghast 快乐恶魂
     * @param baseDuration 基础持续时间
     * @return 增强后的持续时间
     */
    public static int calculateDuration(HappyGhastEntity ghast, int baseDuration) {
        int level = EnchantmentSystem.getEnchantmentLevel(ghast, FireballEnchantment.DURATION);
        
        float multiplier = switch (level) {
            case 1 -> 1.5f;
            case 2 -> 2.0f;
            case 3 -> 3.0f;
            default -> 1.0f;
        };
        
        return Math.round(baseDuration * multiplier);
    }
}
