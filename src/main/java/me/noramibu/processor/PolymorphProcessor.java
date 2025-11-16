package me.noramibu.processor;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 滑稽变形附魔处理器
 * 
 * 效果：将怪物变成无害的被动生物
 * - I级：33%概率变形
 * - II级：66%概率变形
 * - III级：100%概率变形
 */
public class PolymorphProcessor implements EnchantmentProcessor {
    
    private int tickCounter = 0;  // 处理间隔计数器
    private final Set<Integer> polymorphedEntities = new HashSet<>();  // 已变形的实体ID
    
    // 可变形的被动生物类型
    private static final EntityType<?>[] PASSIVE_TYPES = {
        EntityType.CHICKEN,
        EntityType.RABBIT,
        EntityType.PIG,
        EntityType.SHEEP,
        EntityType.COW
    };
    
    /**
     * 配置效果云：金色图腾粒子
     */
    @Override
    public void applyToCloud(AreaEffectCloudEntity cloud, int level) {
        // 设置金色图腾粒子
        cloud.setParticleType(ParticleTypes.TOTEM_OF_UNDYING);
    }
    
    /**
     * 每5 ticks处理一次变形效果
     */
    @Override
    public void process(ServerWorld world, AreaEffectCloudEntity cloud, int level) {
        tickCounter++;
        if (tickCounter < 5) {
            return;  // 还没到处理时间
        }
        tickCounter = 0;
        
        // 获取效果云范围内的所有敌对生物
        Vec3d cloudPos = new Vec3d(cloud.getX(), cloud.getY(), cloud.getZ());
        float radius = cloud.getRadius();
        
        Box searchBox = new Box(
            cloudPos.x - radius, cloudPos.y - radius, cloudPos.z - radius,
            cloudPos.x + radius, cloudPos.y + radius, cloudPos.z + radius
        );
        
        List<HostileEntity> hostiles = world.getEntitiesByClass(
            HostileEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved()
        );
        
        // 计算变形概率
        double polymorphChance = switch (level) {
            case 1 -> 0.33;
            case 2 -> 0.66;
            case 3 -> 1.0;
            default -> 0.33;
        };
        
        // 限制处理数量
        int maxProcessed = Math.min(hostiles.size(), 10);
        
        // 对每个怪物尝试变形
        for (int i = 0; i < maxProcessed; i++) {
            HostileEntity hostile = hostiles.get(i);
            
            // 检查是否已经变形过
            if (polymorphedEntities.contains(hostile.getId())) {
                continue;
            }
            
            // 概率判定
            if (world.getRandom().nextDouble() < polymorphChance) {
                polymorphHostileToPassive(world, hostile);
                polymorphedEntities.add(hostile.getId());
            }
        }
    }
    
    /**
     * 将敌对生物变形为被动生物
     */
    private void polymorphHostileToPassive(ServerWorld world, HostileEntity hostile) {
        // 保存原怪物信息
        double x = hostile.getX();
        double y = hostile.getY();
        double z = hostile.getZ();
        float yaw = hostile.getYaw();
        float pitch = hostile.getPitch();
        
        // 随机选择被动生物类型
        EntityType<?> passiveType = PASSIVE_TYPES[world.getRandom().nextInt(PASSIVE_TYPES.length)];
        
        // 创建新实体
        Entity passiveEntity = passiveType.create(world, SpawnReason.MOB_SUMMONED);
        if (passiveEntity == null) {
            return;  // 创建失败
        }
        
        // 设置位置和朝向
        passiveEntity.refreshPositionAndAngles(x, y, z, yaw, pitch);
        
        // 继承自定义名字
        if (hostile.hasCustomName()) {
            passiveEntity.setCustomName(hostile.getCustomName());
            passiveEntity.setCustomNameVisible(hostile.isCustomNameVisible());
        }
        
        // 生成新实体
        world.spawnEntity(passiveEntity);
        
        // 移除原怪物（不掉落战利品）
        hostile.discard();
        
        // 生成变形粒子和音效
        spawnPolymorphEffects(world, x, y, z);
    }
    
    /**
     * 生成变形特效
     */
    private void spawnPolymorphEffects(ServerWorld world, double x, double y, double z) {
        // 第1层：金色图腾粒子（最显眼）
        world.spawnParticles(
            ParticleTypes.TOTEM_OF_UNDYING,
            x, y + 1.0, z,
            30, 0.5, 0.5, 0.5, 0.1
        );
        
        // 第2层：爆炸粒子（白色闪光）
        world.spawnParticles(
            ParticleTypes.EXPLOSION,
            x, y + 1.0, z,
            5, 0.3, 0.3, 0.3, 0.0
        );
        
        // 第3层：快乐村民粒子（绿色爱心）
        world.spawnParticles(
            ParticleTypes.HAPPY_VILLAGER,
            x, y + 1.0, z,
            20, 0.4, 0.4, 0.4, 0.0
        );
        
        // 第4层：传送门粒子（紫色烟雾）
        world.spawnParticles(
            ParticleTypes.PORTAL,
            x, y + 1.0, z,
            50, 0.3, 0.3, 0.3, 0.5
        );
        
        // 播放音效（高音调）
        world.playSound(
            null,
            x, y, z,
            SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
            SoundCategory.HOSTILE,
            1.0f, 1.5f  // 高音调，更滑稽
        );
    }
}
