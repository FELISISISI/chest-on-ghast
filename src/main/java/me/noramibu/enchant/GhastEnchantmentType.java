package me.noramibu.enchant;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * 快乐恶魂附魔类型定义
 * 描述每一种可用附魔的元数据（等级要求、最大等级、翻译Key等）
 */
public enum GhastEnchantmentType {
	/**
	 * 空槽，用于表示该槽位没有附魔
	 */
	NONE("chest-on-ghast:none", 1, 0, "gui.chest-on-ghast.enchantment.none", "gui.chest-on-ghast.enchantment.none_desc"),

	/**
	 * 连射附魔：一次发射多个火球
	 */
	MULTISHOT("chest-on-ghast:multishot", 1, 3, "enchantment.chest-on-ghast.multishot", "enchantment.chest-on-ghast.multishot.desc"),

	/**
	 * 持久附魔：延长效果云时间
	 */
	DURATION("chest-on-ghast:duration", 3, 3, "enchantment.chest-on-ghast.duration", "enchantment.chest-on-ghast.duration.desc"),

	/**
	 * 冰冻附魔：冻结敌对生物
	 */
	FREEZING("chest-on-ghast:freezing", 3, 3, "enchantment.chest-on-ghast.freezing", "enchantment.chest-on-ghast.freezing.desc"),

	/**
	 * 魅惑附魔：让怪物互相攻击
	 */
	CHARM("chest-on-ghast:charm", 3, 3, "enchantment.chest-on-ghast.charm", "enchantment.chest-on-ghast.charm.desc"),

	/**
	 * 引力奇点附魔：生成吸引效果
	 */
	GRAVITY("chest-on-ghast:gravity", 3, 3, "enchantment.chest-on-ghast.gravity", "enchantment.chest-on-ghast.gravity.desc"),

	/**
	 * 变形附魔：把怪物变成被动物
	 */
	POLYMORPH("chest-on-ghast:polymorph", 3, 3, "enchantment.chest-on-ghast.polymorph", "enchantment.chest-on-ghast.polymorph.desc"),

	/**
	 * 穿透追踪附魔：计划中，暂未实现核心逻辑
	 */
	PIERCING_TRACKER("chest-on-ghast:piercing_tracker", 1, 3, "enchantment.chest-on-ghast.piercing_tracker", "enchantment.chest-on-ghast.piercing_tracker.desc");

	/**
	 * 所有可选择的附魔列表（包含NONE）
	 */
	public static final List<GhastEnchantmentType> SELECTABLE_TYPES = List.of(values());

	private final Identifier id;
	private final int requiredLevel;
	private final int maxLevel;
	private final String nameTranslationKey;
	private final String descriptionTranslationKey;

	GhastEnchantmentType(String id, int requiredLevel, int maxLevel, String nameTranslationKey, String descriptionTranslationKey) {
		this.id = Identifier.tryParse(id);
		if (this.id == null) {
			throw new IllegalArgumentException("Invalid identifier for Ghast enchantment: " + id);
		}
		this.requiredLevel = requiredLevel;
		this.maxLevel = maxLevel;
		this.nameTranslationKey = nameTranslationKey;
		this.descriptionTranslationKey = descriptionTranslationKey;
	}

	public Identifier getId() {
		return id;
	}

	public int getRequiredLevel() {
		return requiredLevel;
	}

	public int getMaxLevel() {
		return maxLevel;
	}

	public String getNameTranslationKey() {
		return nameTranslationKey;
	}

	public String getDescriptionTranslationKey() {
		return descriptionTranslationKey;
	}

	/**
	 * 生成显示名称
	 */
	public Text getDisplayText() {
		return Text.translatable(nameTranslationKey);
	}

	/**
     * 生成描述文本
	 */
	public Text getDescriptionText() {
		return Text.translatable(descriptionTranslationKey);
	}

	/**
	 * 根据ID查找附魔类型
	 */
	public static Optional<GhastEnchantmentType> fromId(String value) {
		if (value == null || value.isEmpty()) {
			return Optional.of(NONE);
		}
		return Arrays.stream(values())
			.filter(type -> type.id.toString().equals(value))
			.findFirst();
	}

	/**
	 * 获取下一个附魔类型（用于GUI循环选择）
	 */
	public GhastEnchantmentType next() {
		int index = SELECTABLE_TYPES.indexOf(this);
		int nextIndex = (index + 1) % SELECTABLE_TYPES.size();
		return SELECTABLE_TYPES.get(nextIndex);
	}

	/**
	 * 将附魔类型渲染为短名称（用于按钮）
	 */
	public Text getShortLabel() {
		if (this == NONE) {
			return Text.translatable("gui.chest-on-ghast.enchantment.short.none");
		}
		return Text.literal(getDisplayText().getString().toUpperCase(Locale.ROOT));
	}
}
