package me.noramibu.enchantment;

/**
 * 火球附魔枚举
 * 定义所有可用的火球附魔类型
 */
public enum FireballEnchantment {
    /**
     * 连射附魔
     * 一次发射多个火球到不同位置
     * 等级越高，发射火球数量越多
     * I级=3个火球，II级=5个火球，III级=7个火球
     */
    MULTISHOT("multishot", "连射", 3),
    
    /**
     * 冰冻附魔
     * 效果云会冻住怪物，使其无法移动
     * 等级越高，冰冻时间越长
     * I级=3秒，II级=5秒，III级=8秒
     */
    FREEZING("freezing", "冰冻", 3),
    
    /**
     * 魅惑附魔
     * 被效果云攻击的怪物会攻击其他怪物
     * 等级越高，持续时间越长
     * I级=10秒，II级=20秒，III级=30秒
     */
    CHARM("charm", "魅惑", 3),
    
    /**
     * 穿透追踪附魔
     * 火球穿透第一个目标后会追踪最近的敌人继续飞行
     * 等级越高，能攻击的怪物数量越多
     * I级=2个怪物，II级=4个怪物，III级=6个怪物
     */
    PIERCING("piercing", "穿透追踪", 3),
    
    /**
     * 持久附魔
     * 效果云持续时间翻倍
     * I级=1.5倍，II级=2倍，III级=3倍
     */
    DURATION("duration", "持久", 3);
    
    // 附魔ID（用于配置文件和NBT存储）
    private final String id;
    
    // 附魔显示名称
    private final String displayName;
    
    // 最大等级
    private final int maxLevel;
    
    /**
     * 构造函数
     */
    FireballEnchantment(String id, String displayName, int maxLevel) {
        this.id = id;
        this.displayName = displayName;
        this.maxLevel = maxLevel;
    }
    
    /**
     * 获取附魔ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 获取最大等级
     */
    public int getMaxLevel() {
        return maxLevel;
    }
    
    /**
     * 根据ID获取附魔类型
     */
    public static FireballEnchantment fromId(String id) {
        for (FireballEnchantment enchantment : values()) {
            if (enchantment.id.equals(id)) {
                return enchantment;
            }
        }
        return null;
    }
    
    /**
     * 获取翻译键
     */
    public String getTranslationKey() {
        return "enchantment.chest-on-ghast." + id;
    }
    
    /**
     * 获取描述翻译键
     */
    public String getDescriptionKey() {
        return "enchantment.chest-on-ghast." + id + ".desc";
    }
}
