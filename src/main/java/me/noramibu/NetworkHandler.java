package me.noramibu;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.enchant.GhastEnchantment;
import me.noramibu.enchant.GhastEnchantmentType;
import me.noramibu.network.GreetGhastPayload;
import me.noramibu.network.OpenGhastGuiPayload;
import me.noramibu.network.RenameGhastPayload;
import me.noramibu.network.RequestGhastDataPayload;
import me.noramibu.network.SyncGhastDataPayload;
import me.noramibu.network.UpdateGhastEnchantmentsPayload;
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
        
        // 注册更新附魔的网络包（客户端到服务端）
        PayloadTypeRegistry.playC2S().register(
            UpdateGhastEnchantmentsPayload.ID,
            UpdateGhastEnchantmentsPayload.CODEC
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
                          ServerPlayNetworking.send(player, createSyncPayload(ghast, player, data));
                        
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
                          ServerPlayNetworking.send(player, createSyncPayload(ghast, player, data));
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
        
        // 注册更新附魔的处理器
        ServerPlayNetworking.registerGlobalReceiver(
            UpdateGhastEnchantmentsPayload.ID,
            (payload, context) -> context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                Entity entity = player.getEntityWorld().getEntityById(payload.entityId());
                
                if (!(entity instanceof HappyGhastEntity ghast)) {
                    return;
                }
                
                HappyGhastData data = getOrCreateGhastData(ghast);
                int slot = payload.slot();
                
                if (slot < 0 || slot >= GhastEnchantment.MAX_SLOTS) {
                    player.sendMessage(Text.translatable("message.chest-on-ghast.enchantment_invalid_slot"), true);
                    return;
                }
                
                GhastEnchantmentType type = GhastEnchantmentType.fromId(payload.enchantmentId())
                    .orElse(GhastEnchantmentType.NONE);
                
                if (type != GhastEnchantmentType.NONE && data.getLevel() < type.getRequiredLevel()) {
                    player.sendMessage(
                        Text.translatable(
                            "message.chest-on-ghast.enchantment_level_requirement",
                            type.getDisplayText(),
                            type.getRequiredLevel()
                        ),
                        true
                    );
                    ServerPlayNetworking.send(player, createSyncPayload(ghast, player, data));
                    return;
                }
                
                GhastEnchantment enchantment = type == GhastEnchantmentType.NONE
                    ? GhastEnchantment.EMPTY
                    : new GhastEnchantment(type, payload.level());
                
                data.setEnchantment(slot, enchantment);
                saveGhastData(ghast, data);
                
                ServerPlayNetworking.send(player, createSyncPayload(ghast, player, data));
                
                player.sendMessage(
                    Text.translatable(
                        "message.chest-on-ghast.enchantment_updated",
                        slot + 1,
                        type.getDisplayText(),
                        formatEnchantmentLevel(enchantment.level())
                    ),
                    false
                );
                
                Chestonghast.LOGGER.info(
                    "玩家 {} 更新快乐恶魂 {} 的附魔槽 {} 为 {} 等级 {}",
                    player.getName().getString(),
                    ghast.getUuidAsString(),
                    slot,
                    type.getId(),
                    enchantment.level()
                );
            })
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
    
    /**
     * 构建同步数据网络包，避免重复的字段拼装
     */
    public static SyncGhastDataPayload createSyncPayload(HappyGhastEntity ghast, ServerPlayerEntity player, HappyGhastData data) {
        return new SyncGhastDataPayload(
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
            data.getCustomName() != null ? data.getCustomName() : "",
            data.getEnchantments()
        );
    }
    
    /**
     * 将等级数字转换成罗马数字，用于UI提示
     */
    private static Text formatEnchantmentLevel(int level) {
        if (level <= 0) {
            return Text.literal("-");
        }
        return switch (level) {
            case 1 -> Text.literal("I");
            case 2 -> Text.literal("II");
            case 3 -> Text.literal("III");
            default -> Text.literal(String.valueOf(level));
        };
    }
}

