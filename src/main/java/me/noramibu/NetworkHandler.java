package me.noramibu;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.network.GreetGhastPayload;
import me.noramibu.network.OpenEnchantmentGuiPayload;
import me.noramibu.network.OpenGhastGuiPayload;
import me.noramibu.network.RenameGhastPayload;
import me.noramibu.network.RequestGhastDataPayload;
import me.noramibu.network.SyncEnchantmentDataPayload;
import me.noramibu.network.SyncGhastDataPayload;
import me.noramibu.network.UpdateEnchantmentPayload;
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
        
        // 注册请求数据的网络包（客户端到服务端）
        PayloadTypeRegistry.playC2S().register(
            RequestGhastDataPayload.ID,
            RequestGhastDataPayload.CODEC
        );
        
        // 注册改名的网络包（客户端到服务端）
        PayloadTypeRegistry.playC2S().register(
            RenameGhastPayload.ID,
            RenameGhastPayload.CODEC
        );
        
        // 注册同步数据的网络包（服务端到客户端）
        PayloadTypeRegistry.playS2C().register(
            SyncGhastDataPayload.ID,
            SyncGhastDataPayload.CODEC
        );
        
        // 注册打开附魔编辑GUI的网络包
        PayloadTypeRegistry.playC2S().register(
            OpenEnchantmentGuiPayload.ID,
            OpenEnchantmentGuiPayload.CODEC
        );
        
        // 注册更新附魔的网络包
        PayloadTypeRegistry.playC2S().register(
            UpdateEnchantmentPayload.ID,
            UpdateEnchantmentPayload.CODEC
        );
        
        // 注册同步附魔数据的网络包（服务端到客户端）
        PayloadTypeRegistry.playS2C().register(
            SyncEnchantmentDataPayload.ID,
            SyncEnchantmentDataPayload.CODEC
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
        
        // 注册打开GUI的处理器
        ServerPlayNetworking.registerGlobalReceiver(
            OpenGhastGuiPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    
                    // 根据实体ID获取快乐恶魂
                    Entity entity = player.getEntityWorld().getEntityById(payload.entityId());
                    
                    if (entity instanceof HappyGhastEntity ghast) {
                        // 读取快乐恶魂的数据
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
                            data.getExpToNextLevel(),
                            player.isCreative(),  // 玩家创造模式状态
                            data.getFavoriteFoods(),  // 最喜欢的食物列表
                            data.getCustomName() != null ? data.getCustomName() : ""  // 自定义名字
                        );
                        
                        ServerPlayNetworking.send(player, syncPayload);
                        
                        Chestonghast.LOGGER.info("玩家 {} 打开了快乐恶魂GUI", player.getName().getString());
                    }
                });
            }
        );
        
        // 注册请求数据的处理器
        ServerPlayNetworking.registerGlobalReceiver(
            RequestGhastDataPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    World world = player.getEntityWorld();
                    Entity entity = world.getEntityById(payload.entityId());
                    
                    if (entity instanceof HappyGhastEntity ghast) {
                        // 获取快乐恶魂的数据
                        HappyGhastData data = getOrCreateGhastData(ghast);
                        
                        // 发送最新数据到客户端
                        SyncGhastDataPayload syncPayload = new SyncGhastDataPayload(
                            ghast.getId(),
                            data.getLevel(),
                            data.getExperience(),
                            data.getHunger(),
                            data.getMaxHealth(),
                            ghast.getHealth(),
                            data.getMaxHunger(),
                            data.getExpToNextLevel(),
                            player.isCreative(),
                            data.getFavoriteFoods(),
                            data.getCustomName() != null ? data.getCustomName() : ""
                        );
                        
                        ServerPlayNetworking.send(player, syncPayload);
                    }
                });
            }
        );
        
        // 注册改名的处理器
        ServerPlayNetworking.registerGlobalReceiver(
            RenameGhastPayload.ID,
            (payload, context) -> {
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    World world = player.getEntityWorld();
                    Entity entity = world.getEntityById(payload.entityId());
                    
                    if (entity instanceof HappyGhastEntity ghast) {
                        // 获取快乐恶魂的数据
                        HappyGhastData data = getOrCreateGhastData(ghast);
                        
                        // 设置自定义名字
                        String newName = payload.newName();
                        data.setCustomName(newName);
                        
                        // 在实体头上显示名字
                        if (newName != null && !newName.isEmpty()) {
                            ghast.setCustomName(Text.literal(newName));
                            ghast.setCustomNameVisible(true);
                        } else {
                            ghast.setCustomName(null);
                            ghast.setCustomNameVisible(false);
                        }
                        
                        Chestonghast.LOGGER.info("玩家 {} 将快乐恶魂改名为：{}", 
                            player.getName().getString(), newName);
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
    
    /**
     * 获取或创建快乐恶魂的数据
     * 通过访问器接口获取数据
     * 
     * @param ghast 快乐恶魂实体
     * @return 快乐恶魂数据对象
     */
    public static HappyGhastData getOrCreateGhastData(HappyGhastEntity ghast) {
        // 使用访问器接口获取数据
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            return accessor.getGhastData();
        }
        // 如果访问器不可用，返回新数据（不应该发生）
        return new HappyGhastData();
    }
    
    /**
     * 保存快乐恶魂的数据
     * 通过访问器接口保存数据
     * 
     * @param ghast 快乐恶魂实体
     * @param data 要保存的数据
     */
    public static void saveGhastData(HappyGhastEntity ghast, HappyGhastData data) {
        // 使用访问器接口设置数据
        if (ghast instanceof HappyGhastDataAccessor accessor) {
            accessor.setGhastData(data);
        }
    }
}

