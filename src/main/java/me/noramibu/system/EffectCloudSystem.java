package me.noramibu.system;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.enchantment.EnchantmentData;
import me.noramibu.enchantment.FireballEnchantment;
import me.noramibu.level.LevelConfig;
import me.noramibu.processor.*;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 效果云系统 - 负责管理火球击中后生成的效果云
 * 
 * 职责：
 * 1. 追踪火球位置
 * 2. 在火球击中时生成效果云
 * 3. 管理效果云的生命周期
 * 4. 委托给各Processor处理附魔效果
 * 
 * 设计原则：
 * - 实例化：每个恶魂有独立的EffectCloudSystem
 * - 线程安全：使用ConcurrentHashMap
 * - 自动清理：定期清理过期的追踪数据
 */
public class EffectCloudSystem {
    
    // 追踪的火球（火球ID -> 快乐恶魂）
    private final ConcurrentHashMap<Integer, HappyGhastEntity> trackedFireballs = new ConcurrentHashMap<>();
    
    // 追踪的火球位置（用于检测击中）
    private final ConcurrentHashMap<Integer, Vec3d> fireballPositions = new ConcurrentHashMap<>();
    
    // 追踪的效果云（效果云ID -> 处理器数据）
    private final ConcurrentHashMap<Integer, CloudData> trackedClouds = new ConcurrentHashMap<>();
    
    // 清理计数器
    private int cleanupCounter = 0;
    
    // 最大追踪数量（防止内存泄漏）
    private static final int MAX_TRACKED_FIREBALLS = 50;
    private static final int MAX_TRACKED_CLOUDS = 30;
    private static final int CLEANUP_INTERVAL = 40;  // 每2秒清理一次
    
    /**
     * 追踪一个火球
     * 
     * @param fireball 火球实体
     * @param ghast 发射者
     */
    public void trackFireball(FireballEntity fireball, HappyGhastEntity ghast) {
        // 限制追踪数量
        if (trackedFireballs.size() >= MAX_TRACKED_FIREBALLS) {
            // 清理最老的一个
            Iterator<Integer> iterator = trackedFireballs.keySet().iterator();
            if (iterator.hasNext()) {
                Integer oldestId = iterator.next();
                trackedFireballs.remove(oldestId);
                fireballPositions.remove(oldestId);
            }
        }
        
        trackedFireballs.put(fireball.getId(), ghast);
        fireballPositions.put(fireball.getId(), new Vec3d(fireball.getX(), fireball.getY(), fireball.getZ()));
    }
    
    /**
     * 每tick调用（只在服务端）
     * 
     * @param world 服务端世界
     */
    public void tick(ServerWorld world) {
        // 检查火球是否击中
        checkFireballHits(world);
        
        // 处理追踪的效果云
        processTrackedClouds(world);
        
        // 定期清理
        cleanupCounter++;
        if (cleanupCounter >= CLEANUP_INTERVAL) {
            cleanup(world);
            cleanupCounter = 0;
        }
    }
    
    /**
     * 检查火球是否击中
     */
    private void checkFireballHits(ServerWorld world) {
        trackedFireballs.forEach((fireballId, ghast) -> {
            Entity entity = world.getEntityById(fireballId);
            
            if (entity instanceof FireballEntity fireball) {
                Vec3d currentPos = new Vec3d(fireball.getX(), fireball.getY(), fireball.getZ());
                Vec3d lastPos = fireballPositions.get(fireballId);
                
                // 更新位置
                fireballPositions.put(fireballId, currentPos);
                
                // 检查是否击中（火球停止移动或被移除）
                if (lastPos != null && (fireball.isRemoved() || currentPos.squaredDistanceTo(lastPos) < 0.01)) {
                    onFireballHit(world, ghast, currentPos);
                    trackedFireballs.remove(fireballId);
                    fireballPositions.remove(fireballId);
                }
            } else {
                // 火球已不存在
                trackedFireballs.remove(fireballId);
                fireballPositions.remove(fireballId);
            }
        });
    }
    
    /**
     * 火球击中时的处理
     * 
     * @param world 世界
     * @param ghast 快乐恶魂
     * @param position 击中位置
     */
    private void onFireballHit(ServerWorld world, HappyGhastEntity ghast, Vec3d position) {
        // 检查等级（3级以上才生成效果云）
        HappyGhastData data = ((HappyGhastDataAccessor) ghast).getHappyGhastData();
        if (data.getLevel() < 3) {
            return;
        }
        
        EnchantmentData enchantments = data.getEnchantmentData();
        
        // 创建效果云
        AreaEffectCloudEntity cloud = new AreaEffectCloudEntity(world, position.x, position.y, position.z);
        
        // 获取配置
        LevelConfig.LevelData levelData = LevelConfig.getLevelData(data.getLevel());
        
        // 应用持久附魔（影响持续时间）
        int baseDuration = levelData.getCloudDuration();
        int finalDuration = DurationProcessor.calculateDuration(ghast, baseDuration);
        
        cloud.setDuration(finalDuration);
        cloud.setRadius(levelData.getCloudRadius());
        cloud.setRadiusGrowth(-cloud.getRadius() / finalDuration);
        
        // 根据附魔选择处理器（优先级：变形 > 引力 > 魅惑 > 冰冻 > 默认）
        if (enchantments.has(FireballEnchantment.POLYMORPH)) {
            PolymorphProcessor.applyToCloud(cloud, enchantments.getLevel(FireballEnchantment.POLYMORPH));
            trackCloud(cloud, new PolymorphProcessor(), enchantments.getLevel(FireballEnchantment.POLYMORPH));
        }
        else if (enchantments.has(FireballEnchantment.GRAVITY)) {
            GravityProcessor.applyToCloud(cloud, enchantments.getLevel(FireballEnchantment.GRAVITY));
            trackCloud(cloud, new GravityProcessor(), enchantments.getLevel(FireballEnchantment.GRAVITY));
        }
        else if (enchantments.has(FireballEnchantment.CHARM)) {
            CharmProcessor.applyToCloud(cloud, enchantments.getLevel(FireballEnchantment.CHARM));
            trackCloud(cloud, new CharmProcessor(), enchantments.getLevel(FireballEnchantment.CHARM));
        }
        else if (enchantments.has(FireballEnchantment.FREEZING)) {
            FreezingProcessor.applyToCloud(cloud, enchantments.getLevel(FireballEnchantment.FREEZING));
            // 冰冻效果不需要持续处理，只需要添加状态效果
        }
        else {
            // 默认治疗效果云
            applyDefaultEffects(cloud, data.getLevel(), finalDuration);
        }
        
        // 生成效果云
        world.spawnEntity(cloud);
    }
    
    /**
     * 应用默认效果（治疗云）
     */
    private void applyDefaultEffects(AreaEffectCloudEntity cloud, int level, int duration) {
        cloud.setParticleType(ParticleTypes.HAPPY_VILLAGER);
        
        // 计算效果强度
        int regenAmplifier = Math.min(level - 3, 2);  // 3级=0, 4-5级=1, 6级=2
        
        // 添加生命恢复效果
        cloud.addEffect(new StatusEffectInstance(
            StatusEffects.REGENERATION,
            duration,
            regenAmplifier,
            false, true
        ));
        
        // 添加速度提升效果
        cloud.addEffect(new StatusEffectInstance(
            StatusEffects.SPEED,
            duration / 2,
            0,
            false, true
        ));
    }
    
    /**
     * 追踪一个效果云
     */
    private void trackCloud(AreaEffectCloudEntity cloud, EnchantmentProcessor processor, int level) {
        // 限制追踪数量
        if (trackedClouds.size() >= MAX_TRACKED_CLOUDS) {
            Iterator<Integer> iterator = trackedClouds.keySet().iterator();
            if (iterator.hasNext()) {
                trackedClouds.remove(iterator.next());
            }
        }
        
        trackedClouds.put(cloud.getId(), new CloudData(processor, level, System.currentTimeMillis()));
    }
    
    /**
     * 处理追踪的效果云
     */
    private void processTrackedClouds(ServerWorld world) {
        trackedClouds.forEach((cloudId, data) -> {
            Entity entity = world.getEntityById(cloudId);
            
            if (entity instanceof AreaEffectCloudEntity cloud && !cloud.isRemoved()) {
                // 委托给对应的Processor处理
                data.processor.process(world, cloud, data.level);
            } else {
                // 效果云已消失
                trackedClouds.remove(cloudId);
            }
        });
    }
    
    /**
     * 清理过期数据
     */
    private void cleanup(ServerWorld world) {
        long currentTime = System.currentTimeMillis();
        
        // 清理火球追踪（超过5秒未更新）
        trackedFireballs.keySet().removeIf(fireballId -> {
            Entity entity = world.getEntityById(fireballId);
            return entity == null || entity.isRemoved();
        });
        
        fireballPositions.keySet().removeIf(fireballId -> !trackedFireballs.containsKey(fireballId));
        
        // 清理效果云追踪（超过30秒或云已消失）
        trackedClouds.entrySet().removeIf(entry -> {
            Entity entity = world.getEntityById(entry.getKey());
            return entity == null || 
                   entity.isRemoved() || 
                   (currentTime - entry.getValue().creationTime) > 30000;
        });
    }
    
    /**
     * 重置系统（恶魂被移除时调用）
     */
    public void reset() {
        trackedFireballs.clear();
        fireballPositions.clear();
        trackedClouds.clear();
    }
    
    /**
     * 效果云数据
     */
    private static class CloudData {
        final EnchantmentProcessor processor;
        final int level;
        final long creationTime;
        
        CloudData(EnchantmentProcessor processor, int level, long creationTime) {
            this.processor = processor;
            this.level = level;
            this.creationTime = creationTime;
        }
    }
}
