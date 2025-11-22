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
    
    // 调试模式，用于输出战斗细节
    public boolean debugMode = false;
    
    // 用于生成默认快乐恶魂名字的全局计数器
    public int nextGhastNameIndex = 1;
    
    /**
     * 等级配置类
     */
    public static class LevelConfig {
        public float maxHealth;          // 最大血量
        public float maxHunger;          // 最大饱食度
        public int expToNextLevel;       // 升级所需经验
        public float hungerDecayMultiplier; // 饱食度消耗倍率（每级递减10%）
        public int fireballPower = 1;    // 火球爆炸威力
        public int attackCooldownTicks = 60; // 攻击冷却（ticks）
        public float fireballDamage = 6.0f;  // 火球直接伤害
        
        public LevelConfig() {}
        
        public LevelConfig(float maxHealth, float maxHunger, int expToNextLevel, float hungerDecayMultiplier,
                           int fireballPower, int attackCooldownTicks, float fireballDamage) {
            this.maxHealth = maxHealth;
            this.maxHunger = maxHunger;
            this.expToNextLevel = expToNextLevel;
            this.hungerDecayMultiplier = hungerDecayMultiplier;
            this.fireballPower = fireballPower;
            this.attackCooldownTicks = attackCooldownTicks;
            this.fireballDamage = fireballDamage;
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
                    config.ensureCombatDefaults();
                    config.save();
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
     * 为旧版本配置补全战斗相关字段，避免缺省值导致战斗失衡
     */
    private void ensureCombatDefaults() {
        GhastConfig defaults = createDefault();
        
        for (int level = 1; level <= 6; level++) {
            LevelConfig defaultConfig = defaults.levels.get(level);
            LevelConfig current = this.levels.computeIfAbsent(level, lvl -> new LevelConfig());
            
            if (current.maxHealth == 0) current.maxHealth = defaultConfig.maxHealth;
            if (current.maxHunger == 0) current.maxHunger = defaultConfig.maxHunger;
            if (current.expToNextLevel == 0 && level != 6) current.expToNextLevel = defaultConfig.expToNextLevel;
            if (current.hungerDecayMultiplier == 0) current.hungerDecayMultiplier = defaultConfig.hungerDecayMultiplier;
            if (current.fireballPower <= 0) current.fireballPower = defaultConfig.fireballPower;
            if (current.attackCooldownTicks <= 0) current.attackCooldownTicks = defaultConfig.attackCooldownTicks;
            if (current.fireballDamage <= 0) current.fireballDamage = defaultConfig.fireballDamage;
        }
        
        if (this.nextGhastNameIndex < 1) {
            this.nextGhastNameIndex = 1;
        }
    }
    
    /**
     * 申请下一个快乐恶魂编号，用于生成“快乐恶魂XX”名称
     */
    public synchronized int claimNextGhastNameIndex() {
        int currentIndex = this.nextGhastNameIndex;
        this.nextGhastNameIndex = Math.max(currentIndex + 1, 1);
        this.save();
        return currentIndex;
    }
    
    /**
     * 创建默认配置
     */
    private static GhastConfig createDefault() {
        GhastConfig config = new GhastConfig();
        config.nextGhastNameIndex = 1;
        
        // MC一昼夜 = 1200秒
        float mcDaySeconds = 1200.0f;
        
        // 配置6个等级，每级饱食度翻倍，消耗速率递减10%
        config.levels.put(1, new LevelConfig(20.0f, 100.0f, 100, 1.0f, 1, 60, 6.0f));   // 入门：慢速低伤
        config.levels.put(2, new LevelConfig(30.0f, 200.0f, 200, 0.9f, 2, 48, 8.0f));   // 稍快并提高爆炸
        config.levels.put(3, new LevelConfig(45.0f, 400.0f, 350, 0.81f, 3, 36, 11.0f)); // 中级：明显提速
        config.levels.put(4, new LevelConfig(65.0f, 800.0f, 550, 0.729f, 4, 26, 15.0f));// 高级：半自动火力
        config.levels.put(5, new LevelConfig(90.0f, 1600.0f, 800, 0.6561f, 5, 18, 20.0f));// 终盘前：高爆高伤
        config.levels.put(6, new LevelConfig(120.0f, 3200.0f, 0, 0.59049f, 6, 10, 26.0f));// 满级：压制火力
        
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
