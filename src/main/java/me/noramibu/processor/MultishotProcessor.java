package me.noramibu.processor;

import me.noramibu.system.EnchantmentSystem;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * 连射附魔处理器
 * 
 * 效果：一次发射多个火球（扇形散开）
 * - I级：3个火球
 * - II级：5个火球
 * - III级：7个火球
 */
public class MultishotProcessor implements EnchantmentProcessor {
    
    /**
     * 连射不需要效果云处理
     */
    @Override
    public void applyToCloud(AreaEffectCloudEntity cloud, int level) {
        // 连射不影响效果云
    }
    
    /**
     * 连射不需要持续处理
     */
    @Override
    public void process(ServerWorld world, AreaEffectCloudEntity cloud, int level) {
        // 连射不需要持续处理
    }
    
    /**
     * 发射多个火球
     * 
     * @param ghast 快乐恶魂
     * @param world 服务端世界
     * @param direction 基准方向
     * @param power 火球威力
     * @param level 附魔等级
     */
    public static void shoot(HappyGhastEntity ghast, ServerWorld world, Vec3d direction, int power, int level) {
        // 计算火球数量
        int count = switch (level) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 7;
            default -> 1;
        };
        
        // 计算扇形角度
        double spreadAngle = Math.toRadians(15 * Math.min(count / 3.0, 2.0));
        double angleStep = (2 * spreadAngle) / (count - 1);
        
        // 发射每个火球
        for (int i = 0; i < count; i++) {
            // 计算当前火球的角度偏移
            double angle = -spreadAngle + i * angleStep;
            
            // 旋转方向向量
            Vec3d rotatedDir = rotateDirectionHorizontally(direction, angle);
            
            // 发射火球
            EnchantmentSystem.shootSingleFireball(ghast, world, rotatedDir, power);
        }
        
        // 播放特殊音效（音调稍高）
        world.playSound(
            null,
            ghast.getX(), ghast.getY(), ghast.getZ(),
            SoundEvents.ENTITY_GHAST_SHOOT,
            SoundCategory.HOSTILE,
            10.0f, 1.2f  // 音调提高20%
        );
    }
    
    /**
     * 水平旋转方向向量
     * 
     * @param direction 原方向
     * @param angle 旋转角度（弧度）
     * @return 旋转后的方向
     */
    private static Vec3d rotateDirectionHorizontally(Vec3d direction, double angle) {
        double x = direction.x;
        double y = direction.y;
        double z = direction.z;
        
        // 计算水平分量
        double horizontalLength = Math.sqrt(x * x + z * z);
        
        // 安全检查：防止除以零或NaN
        if (horizontalLength < 0.0001 || Double.isNaN(horizontalLength) || Double.isInfinite(horizontalLength)) {
            return direction;  // 返回原方向
        }
        
        // 应用2D旋转矩阵（只旋转水平分量）
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        double newX = x * cos - z * sin;
        double newZ = x * sin + z * cos;
        
        // 保持Y分量不变，重新标准化
        Vec3d result = new Vec3d(newX, y, newZ);
        
        // 安全检查
        double length = result.length();
        if (length < 0.0001 || Double.isNaN(length)) {
            return direction;
        }
        
        return result.normalize();
    }
}
