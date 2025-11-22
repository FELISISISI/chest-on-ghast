package me.noramibu.combat;

import me.noramibu.element.GhastElement;

public record GhastCombatStats(
    GhastElement element,
    float damage,
    int cooldownTicks,
    int explosionPower,
    float controlStrength,
    boolean homeBoost
) {}
