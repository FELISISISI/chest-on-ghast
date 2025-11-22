package me.noramibu.combat;

import me.noramibu.config.GhastConfig;
import me.noramibu.data.HappyGhastData;
import me.noramibu.element.GhastElement;
import me.noramibu.level.LevelConfig;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public final class GhastCombatHelper {
    private GhastCombatHelper() {}

    public static GhastCombatStats compute(HappyGhastEntity ghast, HappyGhastData data) {
        if (data == null) {
            return new GhastCombatStats(GhastElement.FIRE, 6.0f, 20, 1, 1.0f, false);
        }

        GhastConfig config = GhastConfig.getInstance();
        int level = Math.max(1, Math.min(LevelConfig.MAX_LEVEL, data.getLevel()));
        GhastElement element = data.getElement();
        LevelConfig.LevelData levelData = LevelConfig.getLevelData(level);
        GhastConfig.ElementLevelConfig elementLevel = config.getElementLevelConfig(element, level);
        GhastConfig.ElementConfig elementConfig = config.getElementConfig(element);

        float damage = Math.max(1.0f, levelData.getFireballDamage()) * elementLevel.damageMultiplier;
        int cooldown = Math.max(5, Math.round(levelData.getAttackCooldownTicks() * elementLevel.cooldownMultiplier));
        int explosionPower = Math.max(1, Math.round(levelData.getFireballPower() * elementLevel.damageMultiplier));
        float control = elementLevel.controlStrength;

        boolean inHome = isElementHomeBiome(ghast, element);
        if (inHome) {
            damage *= 1.0f + elementConfig.sameBiomeDamageBonus;
            control *= 1.0f + elementConfig.sameBiomeEffectBonus;
            cooldown = Math.max(4, Math.round(cooldown * (1.0f - elementConfig.sameBiomeEffectBonus * 0.3f)));
        }

        return new GhastCombatStats(element, damage, cooldown, explosionPower, control, inHome);
    }

    private static boolean isElementHomeBiome(HappyGhastEntity ghast, GhastElement element) {
        if (!(ghast.getEntityWorld() instanceof ServerWorld serverWorld)) {
            return false;
        }

        RegistryEntry<net.minecraft.world.biome.Biome> biomeEntry = serverWorld.getBiome(BlockPos.ofFloored(ghast.getX(), ghast.getY(), ghast.getZ()));
        float temperature = biomeEntry.value().getTemperature();

        return switch (element) {
            case FIRE -> biomeEntry.isIn(BiomeTags.IS_FOREST) || biomeEntry.isIn(BiomeTags.IS_JUNGLE) || temperature >= 1.0f;
            case ICE -> biomeEntry.isIn(BiomeTags.IS_TAIGA) || temperature <= 0.2f;
            case WIND -> biomeEntry.isIn(BiomeTags.IS_MOUNTAIN) || biomeEntry.isIn(BiomeTags.IS_HILL);
            case SAND -> biomeEntry.isIn(BiomeTags.IS_BADLANDS) || temperature >= 1.2f;
        };
    }
}
