package me.noramibu.system;

import me.noramibu.data.HappyGhastData;
import me.noramibu.level.LevelConfig;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HappyGhastEntity;

/**
 * 等级系统 - 负责经验、升级和饱食度管理
 * 
 * 职责：
 * 1. 更新饱食度
 * 2. 处理升级逻辑
 * 3. 更新实体属性（血量）
 * 
 * 设计原则：
 * - 静态方法：无状态，纯逻辑处理
 * - 简单清晰：只处理数值计算
 */
public class LevelingSystem {
    
    /**
     * 每tick调用（只在服务端）
     * 
     * @param ghast 快乐恶魂实体
     * @param data 快乐恶魂数据
     */
    public static void tick(HappyGhastEntity ghast, HappyGhastData data) {
        // 更新饱食度
        data.updateHunger();
        
        // 确保血量上限正确
        updateMaxHealth(ghast, data);
    }
    
    /**
     * 添加经验值
     * 
     * @param ghast 快乐恶魂实体
     * @param data 数据
     * @param amount 经验值数量
     * @return 是否升级了
     */
    public static boolean addExperience(HappyGhastEntity ghast, HappyGhastData data, int amount) {
        boolean leveledUp = data.addExperience(amount);
        
        if (leveledUp) {
            // 更新血量上限
            updateMaxHealth(ghast, data);
            
            // 恢复血量到满
            ghast.setHealth(data.getMaxHealth());
        }
        
        return leveledUp;
    }
    
    /**
     * 更新实体的最大血量属性
     * 
     * @param ghast 快乐恶魂实体
     * @param data 数据
     */
    private static void updateMaxHealth(HappyGhastEntity ghast, HappyGhastData data) {
        EntityAttributeInstance healthAttribute = ghast.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (healthAttribute != null) {
            float expectedMaxHealth = data.getMaxHealth();
            if (healthAttribute.getBaseValue() != expectedMaxHealth) {
                healthAttribute.setBaseValue(expectedMaxHealth);
            }
        }
    }
}
