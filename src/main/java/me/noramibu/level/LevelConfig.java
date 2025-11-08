package me.noramibu.level;

/**
 * 快乐恶魂等级配置类
 * 定义了6个等级，每个等级有不同的属性值
 * 设计理念：等级越高，各项属性越强，游戏体验更好
 */
public class LevelConfig {
    // 最大等级常量
    public static final int MAX_LEVEL = 6;
    
    /**
     * 等级属性数据类
     * 封装每个等级的所有属性
     */
    public static class LevelData {
        private final int level;              // 等级
        private final float maxHealth;        // 最大血量
        private final float maxHunger;        // 最大饱食度
        private final int expToNextLevel;     // 升级所需经验值
        private final float hungerDecayRate;  // 饱食度降低速率（每秒）
        
        /**
         * 构造函数
         * @param level 等级
         * @param maxHealth 最大血量
         * @param maxHunger 最大饱食度
         * @param expToNextLevel 升级所需经验值
         * @param hungerDecayRate 饱食度降低速率
         */
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
    
    // 等级数据数组，索引0对应等级1
    private static final LevelData[] LEVEL_DATA = new LevelData[] {
        // 等级1：新生的快乐恶魂，属性较弱
        new LevelData(1, 20.0f, 20.0f, 100, 0.05f),
        
        // 等级2：开始成长，属性提升
        new LevelData(2, 30.0f, 30.0f, 200, 0.04f),
        
        // 等级3：逐渐强大，饱食度消耗减慢
        new LevelData(3, 45.0f, 45.0f, 350, 0.03f),
        
        // 等级4：成熟的快乐恶魂，各项属性平衡
        new LevelData(4, 65.0f, 65.0f, 550, 0.025f),
        
        // 等级5：精英级别，接近满级
        new LevelData(5, 90.0f, 90.0f, 800, 0.02f),
        
        // 等级6：最高等级，属性达到巅峰
        new LevelData(6, 120.0f, 120.0f, 0, 0.015f)  // 满级不需要经验值
    };
    
    /**
     * 获取指定等级的配置数据
     * @param level 等级（1-6）
     * @return 对应等级的数据，如果等级无效则返回等级1的数据
     */
    public static LevelData getLevelData(int level) {
        // 边界检查：确保等级在有效范围内
        if (level < 1) level = 1;
        if (level > MAX_LEVEL) level = MAX_LEVEL;
        return LEVEL_DATA[level - 1];
    }
    
    /**
     * 计算喂食获得的经验值
     * 根据食物类型返回不同的经验值
     * @param foodItem 食物物品ID
     * @return 经验值
     */
    public static int getExpFromFood(String foodItem) {
        // 基于食物价值设定经验值
        // 这里可以根据需要扩展更多食物类型
        return switch (foodItem) {
            // 基础食物：低经验值
            case "minecraft:apple", "minecraft:carrot", "minecraft:potato" -> 5;
            
            // 中级食物：中等经验值
            case "minecraft:bread", "minecraft:cooked_chicken", "minecraft:cooked_mutton" -> 10;
            
            // 高级食物：高经验值
            case "minecraft:cooked_beef", "minecraft:cooked_porkchop", "minecraft:golden_carrot" -> 20;
            
            // 顶级食物：超高经验值
            case "minecraft:golden_apple", "minecraft:enchanted_golden_apple" -> 50;
            
            // 默认：所有其他可食用物品
            default -> 8;
        };
    }
    
    /**
     * 检查是否可以升级
     * @param currentLevel 当前等级
     * @param currentExp 当前经验值
     * @return 如果可以升级返回true
     */
    public static boolean canLevelUp(int currentLevel, int currentExp) {
        if (currentLevel >= MAX_LEVEL) return false;
        LevelData data = getLevelData(currentLevel);
        return currentExp >= data.getExpToNextLevel();
    }
}
