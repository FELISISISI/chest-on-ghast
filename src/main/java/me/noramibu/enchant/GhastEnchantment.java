package me.noramibu.enchant;

/**
 * 单个快乐恶魂附魔槽位的快照
 * 仅包含附魔类型和等级
 */
public record GhastEnchantment(GhastEnchantmentType type, int level) {
	public static final int MAX_SLOTS = 3;
	public static final GhastEnchantment EMPTY = new GhastEnchantment(GhastEnchantmentType.NONE, 0);

	public GhastEnchantment {
		if (type == null) {
			throw new IllegalArgumentException("Ghast enchantment type cannot be null");
		}

		int clampedLevel = Math.max(0, Math.min(level, type.getMaxLevel()));
		if (type == GhastEnchantmentType.NONE) {
			clampedLevel = 0;
		} else if (clampedLevel == 0) {
			clampedLevel = 1;
		}

		level = clampedLevel;
	}

	public boolean isEmpty() {
		return type == GhastEnchantmentType.NONE;
	}
}
