package me.noramibu;

import me.noramibu.network.GreetGhastPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 网络包处理器
 * 负责注册网络包类型和处理服务端接收到的网络包
 */
public class NetworkHandler {
    /**
     * 注册网络包类型和服务端接收器
     * 当服务端接收到GreetGhastPayload包时，执行相应的逻辑
     */
    public static void registerServerReceivers() {
        // 注册网络包类型
        PayloadTypeRegistry.playC2S().register(
            GreetGhastPayload.ID, 
            GreetGhastPayload.CODEC
        );
        
        // 注册服务端网络包接收器
        ServerPlayNetworking.registerGlobalReceiver(
            GreetGhastPayload.ID,
            (payload, context) -> {
                // 在服务端主线程中执行，确保线程安全
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    
                    // 检测玩家是否正在看着一个实体
                    HitResult hitResult = raycastEntity(player, 5.0);
                    
                    // 如果射线检测到实体
                    if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                        EntityHitResult entityHit = (EntityHitResult) hitResult;
                        Entity targetEntity = entityHit.getEntity();
                        
                        // 检查目标实体是否是快乐恶魂
                        if (targetEntity instanceof HappyGhastEntity) {
                            // 让快乐恶魂向玩家发送"你好！！！"消息
                            player.sendMessage(Text.literal("你好！！！"), false);
                            
                            // 记录日志，用于调试
                            Chestonghast.LOGGER.info("玩家 {} 对快乐恶魂按下了H键", player.getName().getString());
                        }
                    }
                });
            }
        );
    }

    /**
     * 对玩家的视线进行射线检测，查找玩家正在看着的实体
     * 
     * @param player 进行检测的玩家
     * @param maxDistance 最大检测距离
     * @return 如果检测到实体则返回EntityHitResult，否则返回null
     */
    private static HitResult raycastEntity(ServerPlayerEntity player, double maxDistance) {
        // 获取玩家的视线方向
        Vec3d eyePos = player.getEyePos();
        Vec3d lookVec = player.getRotationVec(1.0F);
        Vec3d endPos = eyePos.add(lookVec.multiply(maxDistance));
        
        // 获取玩家所在的世界
        World world = player.getEntityWorld();
        
        // 在玩家周围5格范围内查找快乐恶魂
        for (Entity entity : world.getEntitiesByClass(
            HappyGhastEntity.class, 
            player.getBoundingBox().expand(maxDistance),
            e -> !e.isSpectator() && e.canHit()
        )) {
            // 检查实体的边界框是否与玩家的视线相交
            Vec3d hitPos = entity.getBoundingBox().raycast(eyePos, endPos).orElse(null);
            if (hitPos != null) {
                // 计算到实体的距离
                double distance = eyePos.distanceTo(hitPos);
                if (distance <= maxDistance) {
                    // 返回实体命中结果
                    return new EntityHitResult(entity, hitPos);
                }
            }
        }
        
        return null;
    }
}
