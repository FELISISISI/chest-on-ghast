package me.noramibu.level;

/**
 * 快乐恶魂等级配置类
 * 定义了6个等级，每个等级有不同的属性值
 */
public class LevelConfig {
    public static final int MAX_LEVEL = 6;
    
    // MC一昼夜时长（秒）= 20分钟 = 1200秒
    private static final float MC_DAY_SECONDS = 1200.0f;
    
    /**
     * 等级属性数据类
     */
    public static class LevelData {
        private final int level;
        private final float maxHealth;
        private final float maxHunger;
        private final int expToNextLevel;
        private final float hungerDecayRate;
        
        public LevelData(int level, float maxHealth, float maxHunger, int expToNextLevel, float hungerDecayRate) {
            this.level = level;
            this.maxHealth = maxHealth;
            this.maxHunger = maxHunger;
            this.expToNextLevel = expToNextLevel;
            this.hungerDecayRate = hungerDecayRate;
        }
        
        public int getLevel() { return level; }
        public float getMaxHealth() { return maxHealth; }
        public float getMaxHunger() { return maxHunger; }
        public int getExpToNextLevel() { return expToNextLevel; }
        public float getHungerDecayRate() { return hungerDecayRate; }
    }
    
    // 等级数据：饱食度每级翻倍，消耗速率确保一昼夜消耗完
    private static final LevelData[] LEVEL_DATA = new LevelData[] {
        // 等级1：100饱食度，一昼夜消耗完 (100/1200 = 0.0833/秒)
        new LevelData(1, 20.0f, 100.0f, 100, 100.0f / MC_DAY_SECONDS),
        
        // 等级2：200饱食度，一昼夜消耗完
        new LevelData(2, 30.0f, 200.0f, 200, 200.0f / MC_DAY_SECONDS),
        
        // 等级3：400饱食度，一昼夜消耗完
        new LevelData(3, 45.0f, 400.0f, 350, 400.0f / MC_DAY_SECONDS),
        
        // 等级4：800饱食度，一昼夜消耗完
        new LevelData(4, 65.0f, 800.0f, 550, 800.0f / MC_DAY_SECONDS),
        
        // 等级5：1600饱食度，一昼夜消耗完
        new LevelData(5, 90.0f, 1600.0f, 800, 1600.0f / MC_DAY_SECONDS),
        
        // 等级6：3200饱食度，一昼夜消耗完
        new LevelData(6, 120.0f, 3200.0f, 0, 3200.0f / MC_DAY_SECONDS)
    };
    
    public static LevelData getLevelData(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        return LEVEL_DATA[level - 1];
    }
    
    /**
     * 获取喂食食物的饱食度恢复量和经验值
     * @param foodItem 食物ID
     * @param isFavorite 是否为最喜欢的食物
     * @return [饱食度恢复, 经验值]
     */
    public static float[] getFoodValues(String foodItem, boolean isFavorite) {
        float hungerRestore;
        int exp;
        
        // 雪球特殊处理：最高饱食度恢复
        if (foodItem.equals("minecraft:snowball")) {
            hungerRestore = 50.0f;
            exp = 30;
        }
        // 最喜欢的食物：比雪球还高的恢复和经验
        else if (isFavorite) {
            hungerRestore = 80.0f;
            exp = 50;
        }
        // 普通食物：根据类型给予不同的值
        else {
            hungerRestore = switch (foodItem) {
                case "minecraft:apple", "minecraft:carrot", "minecraft:potato" -> 10.0f;
                case "minecraft:bread", "minecraft:cooked_chicken", "minecraft:cooked_mutton" -> 15.0f;
                case "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:golden_carrot" -> 20.0f;
                case "minecraft:golden_apple" -> 30.0f;
                case "minecraft:enchanted_golden_apple" -> 40.0f;
                default -> 12.0f;
            };
            
            exp = switch (foodItem) {
                case "minecraft:apple", "minecraft:carrot", "minecraft:potato" -> 5;
                case "minecraft:bread", "minecraft:cooked_chicken", "minecraft:cooked_mutton" -> 10;
                case "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:golden_carrot" -> 20;
                case "minecraft:golden_apple" -> 30;
                case "minecraft:enchanted_golden_apple" -> 50;
                default -> 8;
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
