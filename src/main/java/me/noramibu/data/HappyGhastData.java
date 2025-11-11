package me.noramibu.data;

import me.noramibu.enchantment.EnchantmentData;
import me.noramibu.level.LevelConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    
    // 最喜欢的食物列表（3个）
    private List<String> favoriteFoods;
    
    // 自定义名字
    private String customName;
    
    // 附魔数据
    private EnchantmentData enchantmentData;
    
    // 所有可能的食物列表
    private static final String[] ALL_FOODS = {
        "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
        "minecraft:melon_slice", "minecraft:sweet_berries", "minecraft:glow_berries",
        "minecraft:carrot", "minecraft:golden_carrot", "minecraft:potato", "minecraft:baked_potato",
        "minecraft:poisonous_potato", "minecraft:beetroot",
        "minecraft:bread", "minecraft:cookie", "minecraft:pumpkin_pie", "minecraft:cake",
        "minecraft:beef", "minecraft:cooked_beef", "minecraft:porkchop", "minecraft:cooked_porkchop",
        "minecraft:mutton", "minecraft:cooked_mutton", "minecraft:chicken", "minecraft:cooked_chicken",
        "minecraft:rabbit", "minecraft:cooked_rabbit", "minecraft:rabbit_stew",
        "minecraft:cod", "minecraft:cooked_cod", "minecraft:salmon", "minecraft:cooked_salmon",
        "minecraft:tropical_fish", "minecraft:pufferfish", "minecraft:dried_kelp",
        "minecraft:mushroom_stew", "minecraft:beetroot_soup", "minecraft:suspicious_stew",
        "minecraft:rotten_flesh", "minecraft:spider_eye", "minecraft:chorus_fruit",
        "minecraft:honey_bottle"
    };
    
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
        this.favoriteFoods = generateRandomFavoriteFoods();
        this.enchantmentData = new EnchantmentData();
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
        this.favoriteFoods = generateRandomFavoriteFoods();
        this.enchantmentData = new EnchantmentData();
    }
    
    // Getter方法
    public int getLevel() { return level; }
    public int getExperience() { return experience; }
    public float getHunger() { return hunger; }
    public List<String> getFavoriteFoods() { return favoriteFoods; }
    public String getCustomName() { return customName; }
    public EnchantmentData getEnchantmentData() { return enchantmentData; }
    
    // Setter方法
    public void setCustomName(String name) { this.customName = name; }
    
    /**
     * 检查某个食物是否为最喜欢的食物
     */
    public boolean isFavoriteFood(String foodItem) {
        return favoriteFoods != null && favoriteFoods.contains(foodItem);
    }
    
    /**
     * 生成3个随机的最喜欢的食物
     */
    private List<String> generateRandomFavoriteFoods() {
        List<String> favorites = new ArrayList<>();
        Random random = new Random();
        
        while (favorites.size() < 3) {
            String food = ALL_FOODS[random.nextInt(ALL_FOODS.length)];
            if (!favorites.contains(food)) {
                favorites.add(food);
            }
        }
        
        return favorites;
    }
    
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
        
        // 保存最喜欢的食物
        if (favoriteFoods != null && !favoriteFoods.isEmpty()) {
            NbtList foodList = new NbtList();
            for (String food : favoriteFoods) {
                foodList.add(NbtString.of(food));
            }
            nbt.put("FavoriteFoods", foodList);
        }
        
        // 保存自定义名字
        if (customName != null && !customName.isEmpty()) {
            nbt.putString("CustomName", customName);
        }
        
        // 保存附魔数据
        if (enchantmentData != null) {
            nbt.put("EnchantmentData", enchantmentData.writeToNbt());
        }
    }
    
    /**
     * 从NBT反序列化数据
     * 用于读取数据
     * @param nbt NBT标签
     * @return 反序列化的数据对象
     */
    public static HappyGhastData readFromNbt(NbtCompound nbt) {
        HappyGhastData data = new HappyGhastData();
        
        // NBT读取（使用Optional API）
        data.level = nbt.getInt("Level").orElse(1);
        if (data.level < 1 || data.level > 6) {
            data.level = 1;
        }
        
        data.experience = nbt.getInt("Experience").orElse(0);
        if (data.experience < 0) data.experience = 0;
        
        data.hunger = nbt.getFloat("Hunger").orElse(LevelConfig.getLevelData(data.level).getMaxHunger());
        if (data.hunger < 0) data.hunger = 0;
        
        data.lastHungerDecayTime = nbt.getLong("LastHungerDecayTime").orElse(System.currentTimeMillis());
        
        // 读取最喜欢的食物
        nbt.getList("FavoriteFoods").ifPresent(foodList -> {
            data.favoriteFoods = new ArrayList<>();
            for (int i = 0; i < foodList.size(); i++) {
                foodList.getString(i).ifPresent(data.favoriteFoods::add);
            }
        });
        
        // 如果没有最喜欢的食物或数量不足3个，重新生成
        if (data.favoriteFoods == null || data.favoriteFoods.size() != 3) {
            data.favoriteFoods = data.generateRandomFavoriteFoods();
        }
        
        // 读取自定义名字
        nbt.getString("CustomName").ifPresent(name -> data.customName = name);
        
        // 读取附魔数据
        data.enchantmentData = new EnchantmentData();
        nbt.getCompound("EnchantmentData").ifPresent(enchantNbt -> {
            data.enchantmentData.readFromNbt(enchantNbt);
        });
        
        return data;
    }
    
    /**
     * 创建数据的副本
     * @return 新的数据对象
     */
    public HappyGhastData copy() {
        HappyGhastData copy = new HappyGhastData(this.level, this.experience, this.hunger);
        copy.lastHungerDecayTime = this.lastHungerDecayTime;
        copy.favoriteFoods = new ArrayList<>(this.favoriteFoods);
        copy.customName = this.customName;
        
        // 复制附魔数据
        NbtCompound enchantNbt = this.enchantmentData.writeToNbt();
        copy.enchantmentData = new EnchantmentData();
        copy.enchantmentData.readFromNbt(enchantNbt);
        
        return copy;
    }
}
