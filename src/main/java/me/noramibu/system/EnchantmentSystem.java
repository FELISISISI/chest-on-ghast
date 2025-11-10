package me.noramibu.system;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.enchantment.EnchantmentData;
import me.noramibu.enchantment.FireballEnchantment;
import me.noramibu.processor.MultishotProcessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * 附魔系统 - 负责根据附魔创建和发射火球
 * 
 * 职责：
 * 1. 检查附魔并选择对应的发射方式
 * 2. 创建火球实体（只在服务端）
 * 3. 委托给各个Processor处理特殊附魔
 * 
 * 设计原则：
 * - 静态方法：无状态，可被多个系统调用
 * - 委托模式：附魔效果委托给Processor
 * - 服务端执行：所有火球创建只在服务端
 */
public class EnchantmentSystem {
    
    /**
     * 根据附魔发射火球
     * 
     * @param ghast 快乐恶魂
     * @param direction 发射方向
     * @param power 火球威力
     * @param target 目标实体（可能为null）
     */
    public static void shootFireball(HappyGhastEntity ghast, Vec3d direction, int power, LivingEntity target) {
        // 确保只在服务端执行
        if (!(ghast.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        
        // 获取附魔数据
        HappyGhastData data = ((HappyGhastDataAccessor) ghast).getHappyGhastData();
        EnchantmentData enchantments = data.getEnchantmentData();
        
        // 检查连射附魔
        int multishotLevel = enchantments.getLevel(FireballEnchantment.MULTISHOT);
        if (multishotLevel > 0) {
            // 委托给MultishotProcessor
            MultishotProcessor.shoot(ghast, world, direction, power, multishotLevel);
        } else {
            // 发射单个火球
            shootSingleFireball(ghast, world, direction, power);
        }
    }
    
    /**
     * 发射单个火球（内部方法）
     * 
     * @param ghast 快乐恶魂
     * @param world 服务端世界
     * @param direction 方向向量（已标准化）
     * @param power 火球威力
     */
    public static void shootSingleFireball(HappyGhastEntity ghast, ServerWorld world, Vec3d direction, int power) {
        // 创建火球实体
        FireballEntity fireball = new FireballEntity(world, ghast, direction, power);
        
        // 计算生成位置（恶魂前方2格）
        double spawnX = ghast.getX() + direction.x * 2.0;
        double spawnY = ghast.getY() + ghast.getHeight() / 2.0;
        double spawnZ = ghast.getZ() + direction.z * 2.0;
        
        fireball.setPosition(spawnX, spawnY, spawnZ);
        
        // 生成火球
        boolean success = world.spawnEntity(fireball);
        
        if (success) {
            // 播放音效
            world.playSound(
                null,  // null = 所有玩家都能听到
                ghast.getX(), ghast.getY(), ghast.getZ(),
                SoundEvents.ENTITY_GHAST_SHOOT,
                SoundCategory.HOSTILE,
                10.0f, 1.0f
            );
            
            // 通知EffectCloudSystem追踪这个火球
            EffectCloudSystem effectCloudSystem = EffectCloudSystemHolder.get(ghast);
            if (effectCloudSystem != null) {
                effectCloudSystem.trackFireball(fireball, ghast);
            }
        }
    }
    
    /**
     * 获取附魔等级的辅助方法
     * 
     * @param ghast 快乐恶魂
     * @param enchantment 附魔类型
     * @return 附魔等级（0表示没有该附魔）
     */
    public static int getEnchantmentLevel(HappyGhastEntity ghast, FireballEnchantment enchantment) {
        HappyGhastData data = ((HappyGhastDataAccessor) ghast).getHappyGhastData();
        return data.getEnchantmentData().getLevel(enchantment);
    }
}
