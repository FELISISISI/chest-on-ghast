package me.noramibu.level;

import me.noramibu.config.GhastConfig;

/**
 * 快乐恶魂等级配置类
 * 从JSON配置文件读取等级数据
 */
public class LevelConfig {
    public static final int MAX_LEVEL = 6;
    
    /**
     * 等级属性数据类
     */
    public static class LevelData {
        private final int level;
        private final float maxHealth;
        private final float maxHunger;
        private final int expToNextLevel;
        private final float hungerDecayRate;
        private final GhastConfig.LevelConfig config;  // 保存原始配置引用
        
        public LevelData(int level, float maxHealth, float maxHunger, int expToNextLevel, float hungerDecayRate, GhastConfig.LevelConfig config) {
            this.level = level;
            this.maxHealth = maxHealth;
            this.maxHunger = maxHunger;
            this.expToNextLevel = expToNextLevel;
            this.hungerDecayRate = hungerDecayRate;
            this.config = config;
        }
        
        public int getLevel() { return level; }
        public float getMaxHealth() { return maxHealth; }
        public float getMaxHunger() { return maxHunger; }
        public int getExpToNextLevel() { return expToNextLevel; }
        public float getHungerDecayRate() { return hungerDecayRate; }
        
        // 新增方法：获取效果云配置
        public int getCloudDuration() { return config != null ? config.cloudDuration : 100; }
        public float getCloudRadius() { return config != null ? config.cloudRadius : 3.0f; }
        public boolean isEffectCloudEnabled() { return config != null && config.enableEffectCloud; }
        public int getDamageAmplifier() { return config != null ? config.damageAmplifier : 0; }
        public int getRegenAmplifier() { return config != null ? config.regenAmplifier : 0; }
    }
    
    /**
     * 从配置文件获取等级数据
     */
    public static LevelData getLevelData(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        
        GhastConfig.LevelConfig config = GhastConfig.getInstance().getLevelConfig(level);
        float hungerDecayRate = GhastConfig.getInstance().getHungerDecayRate(level);
        
        return new LevelData(
            level,
            config.maxHealth,
            config.maxHunger,
            config.expToNextLevel,
            hungerDecayRate,
            config  // 传递config引用
        );
    }
    
    /**
     * 获取喂食食物的饱食度恢复量和经验值
     * @param foodItem 食物ID
     * @param isFavorite 是否为最喜欢的食物
     * @return [饱食度恢复, 经验值]
     */
    public static float[] getFoodValues(String foodItem, boolean isFavorite) {
        GhastConfig config = GhastConfig.getInstance();
        GhastConfig.FoodConfig foodConfig = config.foodConfig;
        
        float hungerRestore;
        int exp;
        
        // 雪球特殊处理：使用配置文件中的值
        if (foodItem.equals("minecraft:snowball")) {
            hungerRestore = foodConfig.snowballHunger;
            exp = foodConfig.snowballExp;
        }
        // 最喜欢的食物：使用配置文件中的值
        else if (isFavorite) {
            hungerRestore = foodConfig.favoriteHunger;
            exp = foodConfig.favoriteExp;
        }
        // 普通食物：根据类型给予不同的值
        else {
            hungerRestore = switch (foodItem) {
                case "minecraft:apple", "minecraft:carrot", "minecraft:potato" -> 10.0f;
                case "minecraft:bread", "minecraft:cooked_chicken", "minecraft:cooked_mutton" -> 15.0f;
                case "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:golden_carrot" -> 20.0f;
                case "minecraft:golden_apple" -> 30.0f;
                case "minecraft:enchanted_golden_apple" -> 40.0f;
                default -> foodConfig.defaultHunger;
            };
            
            exp = switch (foodItem) {
                case "minecraft:apple", "minecraft:carrot", "minecraft:potato" -> 5;
                case "minecraft:bread", "minecraft:cooked_chicken", "minecraft:cooked_mutton" -> 10;
                case "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:golden_carrot" -> 20;
                case "minecraft:golden_apple" -> 30;
                case "minecraft:enchanted_golden_apple" -> 50;
                default -> foodConfig.defaultExp;
            };
        }
        
        return new float[]{hungerRestore, exp};
    }
    
    public static boolean canLevelUp(int currentLevel, int currentExp) {
        if (currentLevel >= MAX_LEVEL) return false;
        LevelData data = getLevelData(currentLevel);
        return currentExp >= data.getExpToNextLevel();
    }
    
    /**
     * 获取指定等级的火球威力
     * @param level 等级
     * @return 火球威力（爆炸强度）
     */
    public static int getFireballPower(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        
        GhastConfig.LevelConfig config = GhastConfig.getInstance().getLevelConfig(level);
        return config.fireballPower;
    }
    
    /**
     * 获取指定等级的攻击冷却时间
     * @param level 等级
     * @return 攻击冷却时间（ticks，20 ticks = 1秒）
     */
    public static int getAttackCooldown(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        
        GhastConfig.LevelConfig config = GhastConfig.getInstance().getLevelConfig(level);
        return config.attackCooldown;
    }
}
