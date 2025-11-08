package me.noramibu.data;

import me.noramibu.level.LevelConfig;
import net.minecraft.nbt.NbtCompound;

/**
 * 快乐恶魂数据类
 * 用于存储和管理快乐恶魂的等级、经验值、饱食度等数据
 * 提供数据的序列化和反序列化功能
 */
public class HappyGhastData {
    // 当前等级（1-6）
    private int level;
    
    // 当前经验值
    private int experience;
    
    // 当前饱食度
    private float hunger;
    
    // 记录上次饱食度降低的时间（用于计算饱食度消耗）
    private long lastHungerDecayTime;
    
    /**
     * 默认构造函数
     * 初始化为等级1的新生快乐恶魂
     */
    public HappyGhastData() {
        this.level = 1;
        this.experience = 0;
        LevelConfig.LevelData levelData = LevelConfig.getLevelData(1);
        this.hunger = levelData.getMaxHunger();
        this.lastHungerDecayTime = System.currentTimeMillis();
    }
    
    /**
     * 带参数的构造函数
     * @param level 等级
     * @param experience 经验值
     * @param hunger 饱食度
     */
    public HappyGhastData(int level, int experience, float hunger) {
        this.level = level;
        this.experience = experience;
        this.hunger = hunger;
        this.lastHungerDecayTime = System.currentTimeMillis();
    }
    
    // Getter方法
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public float getHunger() { return hunger; }
    
    /**
     * 获取当前等级的最大血量
     * @return 最大血量
     */
    public float getMaxHealth() {
        return LevelConfig.getLevelData(level).getMaxHealth();
    }
    
    /**
     * 获取当前等级的最大饱食度
     * @return 最大饱食度
     */
    public float getMaxHunger() {
        return LevelConfig.getLevelData(level).getMaxHunger();
    }
    
    /**
     * 获取升级所需的经验值
     * @return 升级所需经验值，如果已满级返回0
     */
    public int getExpToNextLevel() {
        return LevelConfig.getLevelData(level).getExpToNextLevel();
    }
    
    /**
     * 获取经验进度百分比
     * @return 0.0-1.0之间的进度值
     */
    public float getExpProgress() {
        if (level >= LevelConfig.MAX_LEVEL) return 1.0f;
        int expNeeded = getExpToNextLevel();
        if (expNeeded == 0) return 1.0f;
        return Math.min(1.0f, (float) experience / expNeeded);
    }
    
    /**
     * 添加经验值
     * 如果经验值足够会自动升级
     * @param amount 经验值数量
     * @return 如果升级了返回true
     */
    public boolean addExperience(int amount) {
        // 如果已经满级，不再增加经验
        if (level >= LevelConfig.MAX_LEVEL) {
            return false;
        }
        
        this.experience += amount;
        
        // 检查是否可以升级
        boolean leveledUp = false;
        while (LevelConfig.canLevelUp(level, experience)) {
            levelUp();
            leveledUp = true;
        }
        
        return leveledUp;
    }
    
    /**
     * 升级操作
     * 提升等级并重置经验值，同时恢复饱食度到满值
     */
    private void levelUp() {
        if (level >= LevelConfig.MAX_LEVEL) return;
        
        // 扣除升级所需的经验值
        experience -= LevelConfig.getLevelData(level).getExpToNextLevel();
        
        // 提升等级
        level++;
        
        // 升级后恢复饱食度到新等级的满值
        LevelConfig.LevelData newLevelData = LevelConfig.getLevelData(level);
        this.hunger = newLevelData.getMaxHunger();
    }
    
    /**
     * 设置饱食度
     * @param hunger 新的饱食度值
     */
    public void setHunger(float hunger) {
        this.hunger = Math.max(0, Math.min(hunger, getMaxHunger()));
    }
    
    /**
     * 增加饱食度（喂食时调用）
     * @param amount 增加的量
     */
    public void addHunger(float amount) {
        setHunger(this.hunger + amount);
    }
    
    /**
     * 更新饱食度（每tick调用）
     * 根据等级和时间降低饱食度
     */
    public void updateHunger() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastHungerDecayTime;
        
        // 每秒更新一次饱食度
        if (timeDiff >= 1000) {
            LevelConfig.LevelData levelData = LevelConfig.getLevelData(level);
            float decayAmount = levelData.getHungerDecayRate();
            
            // 根据经过的秒数计算饱食度降低量
            float totalDecay = decayAmount * (timeDiff / 1000.0f);
            this.hunger = Math.max(0, this.hunger - totalDecay);
            
            this.lastHungerDecayTime = currentTime;
        }
    }
    
    /**
     * 将数据序列化到NBT
     * 用于保存数据
     * @param nbt NBT标签
     */
    public void writeToNbt(NbtCompound nbt) {
        nbt.putInt("Level", level);
        nbt.putInt("Experience", experience);
        nbt.putFloat("Hunger", hunger);
        nbt.putLong("LastHungerDecayTime", lastHungerDecayTime);
    }
    
    /**
     * 从NBT反序列化数据
     * 用于读取数据
     * @param nbt NBT标签
     * @return 反序列化的数据对象
     */
    public static HappyGhastData readFromNbt(NbtCompound nbt) {
        HappyGhastData data = new HappyGhastData();
        
        // 使用Optional处理NBT读取
        data.level = nbt.getInt("Level").orElse(1);
        data.experience = nbt.getInt("Experience").orElse(0);
        data.hunger = nbt.getFloat("Hunger").orElse(LevelConfig.getLevelData(1).getMaxHunger());
        data.lastHungerDecayTime = nbt.getLong("LastHungerDecayTime").orElse(System.currentTimeMillis());
        
        return data;
    }
    
    /**
     * 创建数据的副本
     * @return 新的数据对象
     */
    public HappyGhastData copy() {
        HappyGhastData copy = new HappyGhastData(this.level, this.experience, this.hunger);
        copy.lastHungerDecayTime = this.lastHungerDecayTime;
        return copy;
    }
}
