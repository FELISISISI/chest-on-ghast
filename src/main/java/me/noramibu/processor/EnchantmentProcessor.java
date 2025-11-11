package me.noramibu.processor;

import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * 附魔处理器接口
 * 
 * 所有附魔效果的处理器都实现这个接口
 * 
 * 职责：
 * 1. applyToCloud - 配置效果云的初始属性（粒子、颜色等）
 * 2. process - 每tick处理效果云的逻辑（魅惑、引力等）
 * 
 * 设计原则：
 * - 单一附魔一个Processor
 * - 无状态或最小状态
 * - 线程安全
 */
public interface EnchantmentProcessor {
    
    /**
     * 将附魔效果应用到效果云
     * 在效果云创建时调用一次
     * 
     * @param cloud 效果云实体
     * @param level 附魔等级
     */
    void applyToCloud(AreaEffectCloudEntity cloud, int level);
    
    /**
     * 处理效果云的持续效果
     * 每tick调用
     * 
     * @param world 服务端世界
     * @param cloud 效果云实体
     * @param level 附魔等级
     */
    void process(ServerWorld world, AreaEffectCloudEntity cloud, int level);
}
