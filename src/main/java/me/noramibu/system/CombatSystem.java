package me.noramibu.system;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.level.LevelConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

/**
 * 战斗系统 - 负责快乐恶魂的自动攻击逻辑
 * 
 * 职责：
 * 1. 管理攻击冷却时间
 * 2. 查找和验证攻击目标
 * 3. 触发火球发射（委托给EnchantmentSystem）
 * 
 * 设计原则：
 * - 单一职责：只管战斗，不管附魔
 * - 无状态共享：每个恶魂有独立的CombatSystem实例
 * - 服务端执行：所有逻辑只在服务端运行
 */
public class CombatSystem {
    
    // 攻击冷却计数器（ticks）
    private int attackCooldown = 0;
    
    // 当前攻击目标
    private LivingEntity currentTarget = null;
    
    // 最大攻击距离（16格）
    private static final double MAX_ATTACK_DISTANCE_SQ = 256.0;
    
    /**
     * 每tick调用一次（只在服务端）
     * 
     * @param ghast 快乐恶魂实体
     * @param world 服务端世界
     */
    public void tick(HappyGhastEntity ghast, ServerWorld world) {
        // 1. 冷却管理
        if (attackCooldown > 0) {
            attackCooldown--;
            return;
        }
        
        // 2. 目标验证和查找
        if (!isTargetValid(ghast)) {
            currentTarget = findNearestHostile(ghast, world);
            if (currentTarget == null) {
                return; // 没有有效目标
            }
        }
        
        // 3. 发射火球（委托给EnchantmentSystem）
        Vec3d direction = calculateDirection(ghast, currentTarget);
        HappyGhastData data = ((HappyGhastDataAccessor) ghast).getHappyGhastData();
        int power = LevelConfig.getFireballPower(data.getLevel());
        
        EnchantmentSystem.shootFireball(ghast, direction, power, currentTarget);
        
        // 4. 重置冷却
        attackCooldown = LevelConfig.getAttackCooldown(data.getLevel());
    }
    
    /**
     * 验证当前目标是否有效
     * 
     * @param ghast 快乐恶魂
     * @return 目标是否有效
     */
    private boolean isTargetValid(HappyGhastEntity ghast) {
        if (currentTarget == null) return false;
        if (currentTarget.isRemoved()) return false;
        if (!currentTarget.isAlive()) return false;
        if (currentTarget.getEntityWorld() == null) return false;
        if (currentTarget.getEntityWorld() != ghast.getEntityWorld()) return false;
        
        try {
            double distanceSq = ghast.squaredDistanceTo(currentTarget);
            return distanceSq <= MAX_ATTACK_DISTANCE_SQ;
        } catch (Exception e) {
            // 如果计算距离失败，认为目标无效
            return false;
        }
    }
    
    /**
     * 查找最近的敌对生物
     * 
     * @param ghast 快乐恶魂
     * @param world 世界
     * @return 最近的敌对生物，如果没有则返回null
     */
    private LivingEntity findNearestHostile(HappyGhastEntity ghast, ServerWorld world) {
        // 搜索范围：16格
        double searchRadius = 16.0;
        Box searchBox = new Box(
            ghast.getX() - searchRadius, ghast.getY() - searchRadius, ghast.getZ() - searchRadius,
            ghast.getX() + searchRadius, ghast.getY() + searchRadius, ghast.getZ() + searchRadius
        );
        
        // 获取范围内的所有敌对生物
        List<HostileEntity> hostiles = world.getEntitiesByClass(
            HostileEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved()
        );
        
        // 返回最近的一个
        return hostiles.stream()
            .min(Comparator.comparingDouble(hostile -> hostile.squaredDistanceTo(ghast)))
            .orElse(null);
    }
    
    /**
     * 计算从恶魂到目标的方向向量
     * 
     * @param ghast 快乐恶魂
     * @param target 目标
     * @return 标准化的方向向量
     */
    private Vec3d calculateDirection(HappyGhastEntity ghast, LivingEntity target) {
        double deltaX = target.getX() - ghast.getX();
        double deltaY = target.getBodyY(0.5) - ghast.getY();
        double deltaZ = target.getZ() - ghast.getZ();
        
        Vec3d direction = new Vec3d(deltaX, deltaY, deltaZ);
        double length = direction.length();
        
        // 防止除以零或NaN
        if (length < 0.001) {
            return new Vec3d(0, 0, 1); // 默认方向
        }
        
        Vec3d normalized = direction.normalize();
        
        // 额外检查：确保没有NaN
        if (Double.isNaN(normalized.x) || Double.isNaN(normalized.y) || Double.isNaN(normalized.z)) {
            return new Vec3d(0, 0, 1);
        }
        
        return normalized;
    }
    
    /**
     * 重置战斗系统状态（当恶魂被移除或世界卸载时调用）
     */
    public void reset() {
        this.attackCooldown = 0;
        this.currentTarget = null;
    }
}
