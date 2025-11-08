package me.noramibu;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.network.GreetGhastPayload;
import me.noramibu.network.OpenGhastGuiPayload;
import me.noramibu.network.SyncGhastDataPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.GhastEntity;
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
     * 注册所有自定义网络包并设置处理逻辑
     */
    public static void registerServerReceivers() {
        // 注册问候快乐恶魂的网络包
        PayloadTypeRegistry.playC2S().register(
            GreetGhastPayload.ID, 
            GreetGhastPayload.CODEC
        );
        
        // 注册打开GUI的网络包
        PayloadTypeRegistry.playC2S().register(
            OpenGhastGuiPayload.ID,
            OpenGhastGuiPayload.CODEC
        );
        
        // 注册同步数据的网络包（服务端到客户端）
        PayloadTypeRegistry.playS2C().register(
            SyncGhastDataPayload.ID,
            SyncGhastDataPayload.CODEC
        );
        
        // 注册问候快乐恶魂的处理器
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
                        
                        // 检查目标实体是否是恶魂
                        if (targetEntity instanceof GhastEntity) {
                            // 让恶魂向玩家发送"你好！！！"消息
                            player.sendMessage(Text.literal("你好！！！"), false);
                            
                            // 记录日志，用于调试
                            Chestonghast.LOGGER.info("玩家 {} 对快乐恶魂按下了H键", player.getName().getString());
                        }
                    }
                });
            }
        );
        
        // 注册打开GUI的处理器
        ServerPlayNetworking.registerGlobalReceiver(
            OpenGhastGuiPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    
                    // 根据实体ID获取恶魂
                    Entity entity = player.getEntityWorld().getEntityById(payload.entityId());
                    
                    if (entity instanceof GhastEntity ghast) {
                        // 读取恶魂的数据
                        HappyGhastData data = getOrCreateGhastData(ghast);
                        
                        // 发送数据到客户端并打开GUI
                        SyncGhastDataPayload syncPayload = new SyncGhastDataPayload(
                            ghast.getId(),
                            data.getLevel(),
                            data.getExperience(),
                            data.getHunger(),
                            data.getMaxHealth(),
                            ghast.getHealth(),
                            data.getMaxHunger(),
                            data.getExpToNextLevel()
                        );
                        
                        ServerPlayNetworking.send(player, syncPayload);
                        
                        Chestonghast.LOGGER.info("玩家 {} 打开了快乐恶魂GUI", player.getName().getString());
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
        
        // 在玩家周围5格范围内查找恶魂
        for (Entity entity : world.getEntitiesByClass(
            GhastEntity.class, 
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
    
    /**
     * 获取或创建恶魂的数据
     * 通过访问器接口获取数据
     * 
     * @param ghast 恶魂实体
     * @return 恶魂数据对象
     */
    public static HappyGhastData getOrCreateGhastData(GhastEntity ghast) {
        // 使用访问器接口获取数据
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            return accessor.getGhastData();
        }
        // 如果访问器不可用，返回新数据（不应该发生）
        return new HappyGhastData();
    }
    
    /**
     * 保存恶魂的数据
     * 通过访问器接口保存数据
     * 
     * @param ghast 恶魂实体
     * @param data 要保存的数据
     */
    public static void saveGhastData(GhastEntity ghast, HappyGhastData data) {
        // 使用访问器接口设置数据
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            accessor.setGhastData(data);
        }
    }
}

