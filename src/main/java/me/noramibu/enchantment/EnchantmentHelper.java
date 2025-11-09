package me.noramibu.enchantment;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * 附魔辅助类
 * 提供各种附魔效果的应用方法
 */
public class EnchantmentHelper {
    
    /**
     * 发射火球（带附魔效果）
     * 如果有连射附魔，会发射多个火球
     * 
     * @param ghast 快乐恶魂实体
     * @param direction 发射方向（单位向量）
     * @param fireballPower 火球威力
     * @param target 目标实体（可选，用于计算准确方向）
     */
    public static void shootFireballWithEnchantments(HappyGhastEntity ghast, Vec3d direction, 
                                                    int fireballPower, LivingEntity target) {
        // 获取附魔数据
        EnchantmentData enchantmentData = getEnchantmentData(ghast);
        if (enchantmentData == null) {
            // 没有附魔数据，发射单个火球
            shootSingleFireball(ghast, direction, fireballPower);
            return;
        }
        
        // 检查连射附魔
        int multishotLevel = enchantmentData.getEnchantmentLevel(FireballEnchantment.MULTISHOT);
        
        if (multishotLevel > 0) {
            // 有连射附魔，发射多个火球
            int fireballCount = getMultishotCount(multishotLevel);
            shootMultipleFireballs(ghast, direction, fireballPower, fireballCount);
        } else {
            // 没有连射附魔，发射单个火球
            shootSingleFireball(ghast, direction, fireballPower);
        }
    }
    
    /**
     * 获取连射附魔的火球数量
     */
    private static int getMultishotCount(int level) {
        switch (level) {
            case 1: return 3;  // I级：3个火球
            case 2: return 5;  // II级：5个火球
            case 3: return 7;  // III级：7个火球
            default: return 3;
        }
    }
    
    /**
     * 发射单个火球
     */
    private static void shootSingleFireball(HappyGhastEntity ghast, Vec3d direction, int fireballPower) {
        // 只在服务端创建火球（避免重复）
        if (!(ghast.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld)) {
            return;
        }
        
        // 归一化方向向量
        Vec3d normalizedDir = direction.normalize();
        
        // 创建火球实体
        // 注意：fireballPower是爆炸强度，原版恶魂使用1，但这里我们允许更高的值
        FireballEntity fireball = new FireballEntity(
            ghast.getEntityWorld(),
            ghast,
            normalizedDir,
            fireballPower
        );
        
        // 设置火球位置（从恶魂中心稍微往前）
        double spawnX = ghast.getX() + normalizedDir.x * 2.0;
        double spawnY = ghast.getY() + ghast.getHeight() / 2.0;
        double spawnZ = ghast.getZ() + normalizedDir.z * 2.0;
        fireball.setPosition(spawnX, spawnY, spawnZ);
        
        // 生成火球
        boolean spawned = ghast.getEntityWorld().spawnEntity(fireball);
        
        // 调试日志
        // System.out.println("[EnchantmentHelper] 火球生成: " + spawned + ", Power: " + fireballPower + ", Pos: " + spawnX + "," + spawnY + "," + spawnZ);
        
        // 检查穿透追踪附魔（注意：此附魔功能需要额外的Mixin实现）
        int piercingLevel = getEnchantmentLevel(ghast, FireballEnchantment.PIERCING);
        
        // TODO: 穿透追踪附魔需要额外的FireballEntity Mixin来实现
        // 目前暂时未完全实现，附魔书可以装备但效果不生效
        if (piercingLevel > 0) {
            // 未来版本将实现：火球击中目标后不消失，转向下一个敌人
        }
        
        // 如果恶魂等级>=3，追踪火球用于效果云生成
        if (spawned) {
            trackFireballForEffectCloud(ghast, fireball);
        }
        
        // 播放音效
        ghast.playSound(SoundEvents.ENTITY_GHAST_SHOOT, 10.0f, 
            (ghast.getRandom().nextFloat() - ghast.getRandom().nextFloat()) * 0.2f + 1.0f);
    }
    
    /**
     * 获取穿透追踪附魔的目标数量
     */
    public static int getPiercingTargetCount(int level) {
        switch (level) {
            case 1: return 2;  // I级：2个目标
            case 2: return 3;  // II级：3个目标
            case 3: return 5;  // III级：5个目标
            default: return 2;
        }
    }
    
    /**
     * 发射多个火球（连射效果）
     * 火球会以扇形散开
     */
    private static void shootMultipleFireballs(HappyGhastEntity ghast, Vec3d direction, 
                                              int fireballPower, int count) {
        // 只在服务端创建火球（避免重复）
        if (!(ghast.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld)) {
            return;
        }
        
        // 归一化方向向量
        Vec3d normalizedDir = direction.normalize();
        
        // 计算扇形的角度范围
        // 火球数量越多，散开角度越大
        double spreadAngle = Math.toRadians(15 * Math.min(count / 3.0, 2.0)); // 15°到30°
        
        // 计算每个火球之间的角度间隔
        double angleStep = (count > 1) ? (spreadAngle * 2) / (count - 1) : 0;
        
        // 发射所有火球
        for (int i = 0; i < count; i++) {
            // 计算这个火球的偏移角度（相对于中心方向）
            double angle = -spreadAngle + i * angleStep;
            
            // 计算旋转后的方向向量
            Vec3d rotatedDir = rotateDirectionHorizontally(normalizedDir, angle);
            
            // 创建火球
            FireballEntity fireball = new FireballEntity(
                ghast.getEntityWorld(),
                ghast,
                rotatedDir,
                fireballPower
            );
            
            // 设置火球位置（稍微分散开）
            double offset = 2.0 + i * 0.2;
            double spawnX = ghast.getX() + rotatedDir.x * offset;
            double spawnY = ghast.getY() + ghast.getHeight() / 2.0;
            double spawnZ = ghast.getZ() + rotatedDir.z * offset;
            fireball.setPosition(spawnX, spawnY, spawnZ);
            
            // 生成火球
            boolean spawned = ghast.getEntityWorld().spawnEntity(fireball);
            
            // 追踪每个火球用于效果云生成
            if (spawned) {
                trackFireballForEffectCloud(ghast, fireball);
            }
        }
        
        // 播放音效（音调稍高以示区别）
        ghast.playSound(SoundEvents.ENTITY_GHAST_SHOOT, 10.0f, 1.2f);
    }
    
    /**
     * 水平旋转方向向量
     * @param direction 原方向
     * @param angle 旋转角度（弧度）
     * @return 旋转后的方向
     */
    private static Vec3d rotateDirectionHorizontally(Vec3d direction, double angle) {
        // 获取水平面上的方向（忽略Y）
        double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        
        // 安全检查：如果是垂直方向或长度为0，直接返回
        if (horizontalLength < 0.0001 || Double.isNaN(horizontalLength) || Double.isInfinite(horizontalLength)) {
            return direction;
        }
        
        // 归一化水平方向
        double normX = direction.x / horizontalLength;
        double normZ = direction.z / horizontalLength;
        
        // 旋转（2D旋转矩阵）
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        double newX = normX * cos - normZ * sin;
        double newZ = normX * sin + normZ * cos;
        
        // 恢复原始长度并保持Y分量
        return new Vec3d(
            newX * horizontalLength,
            direction.y,
            newZ * horizontalLength
        ).normalize();
    }
    
    /**
     * 获取恶魂的附魔数据
     */
    private static EnchantmentData getEnchantmentData(HappyGhastEntity ghast) {
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            HappyGhastData data = accessor.getGhastData();
            return data != null ? data.getEnchantmentData() : null;
        }
        return null;
    }
    
    /**
     * 检查是否有指定附魔
     */
    public static boolean hasEnchantment(HappyGhastEntity ghast, FireballEnchantment enchantment) {
        EnchantmentData data = getEnchantmentData(ghast);
        return data != null && data.hasEnchantment(enchantment);
    }
    
    /**
     * 获取附魔等级
     */
    public static int getEnchantmentLevel(HappyGhastEntity ghast, FireballEnchantment enchantment) {
        EnchantmentData data = getEnchantmentData(ghast);
        return data != null ? data.getEnchantmentLevel(enchantment) : 0;
    }
    
    /**
     * 追踪火球用于效果云生成（如果恶魂等级>=3）
     */
    private static void trackFireballForEffectCloud(HappyGhastEntity ghast, FireballEntity fireball) {
        // 获取恶魂等级
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            HappyGhastData data = accessor.getGhastData();
            int level = data.getLevel();
            
            // 如果等级>=3，追踪火球
            if (level >= 3) {
                try {
                    // 通过反射调用trackFireball方法
                    java.lang.reflect.Method trackMethod = ghast.getClass().getMethod("trackFireball", 
                        FireballEntity.class, int.class);
                    trackMethod.invoke(ghast, fireball, level);
                } catch (Exception e) {
                    // 忽略反射失败
                }
            }
        }
    }
}
