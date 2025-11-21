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
        private final int fireballPower;
        private final int attackCooldownTicks;
        
        public LevelData(int level, float maxHealth, float maxHunger, int expToNextLevel, float hungerDecayRate,
                         int fireballPower, int attackCooldownTicks) {
            this.level = level;
            this.maxHealth = maxHealth;
            this.maxHunger = maxHunger;
            this.expToNextLevel = expToNextLevel;
            this.hungerDecayRate = hungerDecayRate;
            this.fireballPower = fireballPower;
            this.attackCooldownTicks = attackCooldownTicks;
        }
        
        public int getLevel() { return level; }
        public float getMaxHealth() { return maxHealth; }
        public float getMaxHunger() { return maxHunger; }
        public int getExpToNextLevel() { return expToNextLevel; }
        public float getHungerDecayRate() { return hungerDecayRate; }
        public int getFireballPower() { return fireballPower; }
        public int getAttackCooldownTicks() { return attackCooldownTicks; }
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
            config.fireballPower,
            config.attackCooldownTicks
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
}
