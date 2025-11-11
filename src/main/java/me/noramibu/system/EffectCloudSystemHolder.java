package me.noramibu.system;

import net.minecraft.entity.passive.HappyGhastEntity;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EffectCloudSystem持有者
 * 用于从EnchantmentSystem访问每个恶魂的EffectCloudSystem实例
 */
public class EffectCloudSystemHolder {
    private static final ConcurrentHashMap<Integer, EffectCloudSystem> instances = new ConcurrentHashMap<>();
    
    public static void register(HappyGhastEntity ghast, EffectCloudSystem system) {
        instances.put(ghast.getId(), system);
    }
    
    public static EffectCloudSystem get(HappyGhastEntity ghast) {
        return instances.get(ghast.getId());
    }
    
    public static void unregister(HappyGhastEntity ghast) {
        instances.remove(ghast.getId());
    }
}
