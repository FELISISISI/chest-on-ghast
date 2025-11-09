package me.noramibu.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.noramibu.Chestonghast;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 快乐恶魂配置管理器
 * 从JSON文件加载和保存配置
 */
public class GhastConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance()
        .getConfigDir()
        .resolve("chest-on-ghast.json")
        .toFile();
    
    private static GhastConfig INSTANCE = null;
    
    // 等级配置映射：等级 -> 等级配置
    public Map<Integer, LevelConfig> levels = new HashMap<>();
    
    // 喂食配置
    public FoodConfig foodConfig = new FoodConfig();
    
    /**
     * 等级配置类
     */
    public static class LevelConfig {
        public float maxHealth;          // 最大血量
        public float maxHunger;          // 最大饱食度
        public int expToNextLevel;       // 升级所需经验
        public float hungerDecayMultiplier; // 饱食度消耗倍率（每级递减10%）
        public int fireballPower;        // 火球威力（爆炸强度）
        public int attackCooldown;       // 攻击冷却时间（ticks，20 ticks = 1秒）
        
        public LevelConfig() {}
        
        public LevelConfig(float maxHealth, float maxHunger, int expToNextLevel, float hungerDecayMultiplier, int fireballPower, int attackCooldown) {
            this.maxHealth = maxHealth;
            this.maxHunger = maxHunger;
            this.expToNextLevel = expToNextLevel;
            this.hungerDecayMultiplier = hungerDecayMultiplier;
            this.fireballPower = fireballPower;
            this.attackCooldown = attackCooldown;
        }
    }
    
    /**
     * 喂食配置类
     */
    public static class FoodConfig {
        public float snowballHunger = 50.0f;      // 雪球恢复饱食度
        public int snowballExp = 10;              // 雪球给予经验（等级1基准）
        public float favoriteHunger = 80.0f;      // 最喜欢食物恢复饱食度
        public int favoriteExp = 20;              // 最喜欢食物给予经验（等级1基准）
        public float defaultHunger = 12.0f;       // 默认食物恢复饱食度
        public int defaultExp = 5;                // 默认食物给予经验（等级1基准）
    }
    
    /**
     * 获取配置实例（单例模式）
     */
    public static GhastConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }
    
    /**
     * 从文件加载配置
     */
    private static GhastConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                GhastConfig config = GSON.fromJson(reader, GhastConfig.class);
                if (config != null && config.levels != null && !config.levels.isEmpty()) {
                    Chestonghast.LOGGER.info("已从配置文件加载快乐恶魂配置：{}", CONFIG_FILE.getAbsolutePath());
                    return config;
                }
            } catch (IOException e) {
                Chestonghast.LOGGER.error("加载配置文件失败", e);
            }
        }
        
        // 配置文件不存在或无效，创建默认配置
        Chestonghast.LOGGER.info("创建默认快乐恶魂配置文件：{}", CONFIG_FILE.getAbsolutePath());
        GhastConfig config = createDefault();
        config.save();
        return config;
    }
    
    /**
     * 创建默认配置
     */
    private static GhastConfig createDefault() {
        GhastConfig config = new GhastConfig();
        
        // MC一昼夜 = 1200秒
        float mcDaySeconds = 1200.0f;
        
        // 配置6个等级，每级饱食度翻倍，消耗速率递减10%
        // 战斗参数：等级越高，火球威力越大，冷却时间越短
        config.levels.put(1, new LevelConfig(20.0f, 100.0f, 100, 1.0f, 1, 60));        // 等级1：威力1，冷却3秒
        config.levels.put(2, new LevelConfig(30.0f, 200.0f, 200, 0.9f, 2, 50));        // 等级2：威力2，冷却2.5秒
        config.levels.put(3, new LevelConfig(45.0f, 400.0f, 350, 0.81f, 3, 40));       // 等级3：威力3，冷却2秒
        config.levels.put(4, new LevelConfig(65.0f, 800.0f, 550, 0.729f, 4, 30));      // 等级4：威力4，冷却1.5秒
        config.levels.put(5, new LevelConfig(90.0f, 1600.0f, 800, 0.6561f, 5, 20));    // 等级5：威力5，冷却1秒
        config.levels.put(6, new LevelConfig(120.0f, 3200.0f, 0, 0.59049f, 6, 15));    // 等级6：威力6，冷却0.75秒
        
        return config;
    }
    
    /**
     * 保存配置到文件
     */
    public void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(this, writer);
                Chestonghast.LOGGER.info("配置已保存到：{}", CONFIG_FILE.getAbsolutePath());
            }
        } catch (IOException e) {
            Chestonghast.LOGGER.error("保存配置文件失败", e);
        }
    }
    
    /**
     * 获取指定等级的配置
     */
    public LevelConfig getLevelConfig(int level) {
        if (level < 1) level = 1;
        if (level > 6) level = 6;
        return levels.getOrDefault(level, levels.get(1));
    }
    
    /**
     * 计算实际饱食度消耗速率
     * @param level 等级
     * @return 每秒消耗的饱食度
     */
    public float getHungerDecayRate(int level) {
        LevelConfig config = getLevelConfig(level);
        // 基础速率 = 最大饱食度 / 1200秒（一昼夜）
        float baseRate = config.maxHunger / 1200.0f;
        // 实际速率 = 基础速率 * 消耗倍率
        return baseRate * config.hungerDecayMultiplier;
    }
}
