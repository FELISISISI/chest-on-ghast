package me.noramibu.mixin;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.level.LevelConfig;
import me.noramibu.network.SyncGhastDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.EquipmentSlot;

/**
 * Mixin for HappyGhastEntity
 * 添加等级系统、喂食系统、饱食度系统和GUI交互
 * 保留原有的箱子矿车放置功能
 */
@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin implements HappyGhastDataAccessor {
    // 存储快乐恶魂的数据（等级、经验、饱食度等）
    @Unique
    private HappyGhastData ghastData;
    
    // 用于跟踪tick计数，控制饱食度更新频率
    @Unique
    private int tickCounter = 0;
    
    /**
     * 实现数据访问器接口 - 获取数据
     */
    @Override
    public HappyGhastData getGhastData() {
        if (this.ghastData == null) {
            this.ghastData = new HappyGhastData();
        }
        return this.ghastData;
    }
    
    /**
     * 实现数据访问器接口 - 设置数据
     */
    @Override
    public void setGhastData(HappyGhastData data) {
        this.ghastData = data;
    }
    
    /**
     * 注入到实体初始化方法
     * 在实体创建时初始化数据
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // 初始化快乐恶魂数据
        this.ghastData = new HappyGhastData();
    }
    
    /**
     * 注入到tick方法
     * 每个游戏tick都会执行，用于更新饱食度和血量上限
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        
        // 确保数据已初始化
        if (this.ghastData == null) {
            this.ghastData = new HappyGhastData();
        }
        
        // 只在服务端执行
        if (!ghast.getEntityWorld().isClient()) {
            // 每20个tick（1秒）更新一次饱食度
            tickCounter++;
            if (tickCounter >= 20) {
                ghastData.updateHunger();
                tickCounter = 0;
            }
            
            // 确保血量上限正确
            updateMaxHealth(ghast);
        }
    }
    
    /**
     * 注入到interactMob方法
     * 处理玩家与快乐恶魂的交互
     */
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteractMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        ItemStack itemStack = player.getStackInHand(hand);
        
        // 确保数据已初始化
        if (this.ghastData == null) {
            this.ghastData = new HappyGhastData();
        }
        
        // 只在服务端处理交互
        if (!ghast.getEntityWorld().isClient()) {
            // 检查是否按住Shift键 - 打开GUI
            if (player.isSneaking()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // 读取快乐恶魂的数据
                    HappyGhastData data = this.getGhastData();
                    
                    // 直接发送数据到客户端并打开GUI
                    // 使用SyncGhastDataPayload（已正确注册为S2C）
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
                    
                    ServerPlayNetworking.send(serverPlayer, syncPayload);
                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }
            }
            
            // 检查是否手持雪球或食物 - 喂食
            String foodId = Registries.ITEM.getId(itemStack.getItem()).toString();
            boolean isSnowball = itemStack.isOf(Items.SNOWBALL);
            boolean isFood = itemStack.getItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD);
            
            if (isSnowball || isFood) {
                // 检查是否为最喜欢的食物
                boolean isFavorite = ghastData.isFavoriteFood(foodId);
                
                // 获取饱食度恢复量和经验值
                float hungerRestore;
                int expGain;
                
                if (isSnowball) {
                    // 雪球：最高恢复，但不是最喜欢的食物
                    hungerRestore = 50.0f;
                    expGain = 30;
                } else if (isFavorite) {
                    // 最喜欢的食物：比雪球更高的恢复和经验
                    hungerRestore = 80.0f;
                    expGain = 50;
                    
                    // 生成爱心粒子效果
                    if (ghast.getEntityWorld() instanceof ServerWorld serverWorld) {
                        // 在快乐恶魂周围生成多个爱心粒子
                        for (int i = 0; i < 7; i++) {
                            double offsetX = (Math.random() - 0.5) * 1.5;
                            double offsetY = Math.random() * 1.5;
                            double offsetZ = (Math.random() - 0.5) * 1.5;
                            
                            serverWorld.spawnParticles(
                                net.minecraft.particle.ParticleTypes.HEART,
                                ghast.getX() + offsetX,
                                ghast.getY() + offsetY + 1.0,
                                ghast.getZ() + offsetZ,
                                1, // 粒子数量
                                0.0, 0.0, 0.0, // 速度偏移
                                0.0 // 速度
                            );
                        }
                    }
                    
                    // 发送特殊消息
                    player.sendMessage(
                        Text.translatable("message.chest-on-ghast.favorite_food"),
                        true
                    );
                } else {
                    // 普通食物：根据食物类型决定
                    float[] values = LevelConfig.getFoodValues(foodId, false);
                    hungerRestore = values[0];
                    expGain = (int) values[1];
                }
                
                // 添加饱食度
                ghastData.addHunger(hungerRestore);
                
                // 添加经验值
                boolean leveledUp = ghastData.addExperience(expGain);
                
                // 如果升级了，更新血量上限并发送消息
                if (leveledUp) {
                    updateMaxHealth(ghast);
                    player.sendMessage(
                        Text.translatable("message.chest-on-ghast.level_up", ghastData.getLevel()),
                        false
                    );
                }
                
                // 显示喂食反馈
                if (!isFavorite) {
                    player.sendMessage(
                        Text.translatable("message.chest-on-ghast.fed", 
                            (int)hungerRestore, expGain),
                        true  // 显示在物品栏上方
                    );
                }
                
                // 消耗食物
                if (!player.getAbilities().creativeMode) {
                    itemStack.decrement(1);
                }
                
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
            
            // 检查是否使用箱子矿车（保留原有功能）
            if (itemStack.isOf(Items.CHEST_MINECART) && 
                ghast.getPassengerList().size() < 3 && 
                !ghast.getEquippedStack(EquipmentSlot.BODY).isEmpty()) {
                
                // 创建并生成箱子矿车
                ChestMinecartEntity chestMinecart = new ChestMinecartEntity(
                    EntityType.CHEST_MINECART, 
                    ghast.getEntityWorld()
                );
                chestMinecart.refreshPositionAndAngles(
                    ghast.getX(), ghast.getY(), ghast.getZ(), 
                    ghast.getYaw(), ghast.getPitch()
                );
                ghast.getEntityWorld().spawnEntity(chestMinecart);
                chestMinecart.startRiding(ghast);
                
                // 消耗物品
                if (!player.getAbilities().creativeMode) {
                    itemStack.decrement(1);
                }
                
                // 设置无碰撞团队
                if (ghast.getEntityWorld() instanceof ServerWorld serverWorld) {
                    Scoreboard scoreboard = serverWorld.getScoreboard();
                    Team team = scoreboard.getTeam("NoCollision");
                    
                    if (team == null) {
                        team = scoreboard.addTeam("NoCollision");
                        team.setCollisionRule(Team.CollisionRule.NEVER);
                    } else if (team.getCollisionRule() != Team.CollisionRule.NEVER) {
                        team.setCollisionRule(Team.CollisionRule.NEVER);
                    }
                    
                    String command = String.format("team join NoCollision %s", 
                        chestMinecart.getUuidAsString());
                    try {
                        serverWorld.getServer().getCommandManager().executeWithPrefix(
                            serverWorld.getServer().getCommandSource().withSilent(), 
                            command
                        );
                    } catch (Exception ignored) {
                        // 忽略命令执行错误
                    }
                }
                
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
    
    
    /**
     * 注入到writeCustomDataToNbt方法
     * 保存快乐恶魂的数据到NBT
     */
    @Inject(method = "writeCustomDataToNbt", at = @At("RETURN"))
    private void onWriteCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (this.ghastData != null) {
            NbtCompound dataCompound = new NbtCompound();
            this.ghastData.writeToNbt(dataCompound);
            nbt.put("HappyGhastData", dataCompound);
        }
    }
    
    /**
     * 注入到readCustomDataFromNbt方法
     * 从NBT读取快乐恶魂的数据
     */
    @Inject(method = "readCustomDataFromNbt", at = @At("RETURN"))
    private void onReadCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (nbt.contains("HappyGhastData")) {
            nbt.getCompound("HappyGhastData").ifPresent(dataCompound -> {
                this.ghastData = HappyGhastData.readFromNbt(dataCompound);
            });
        }
        
        if (this.ghastData == null) {
            this.ghastData = new HappyGhastData();
        }
        
        // 确保血量上限正确
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        updateMaxHealth(ghast);
    }
    
    /**
     * 更新快乐恶魂的最大血量
     * 根据当前等级设置正确的血量上限
     */
    @Unique
    private void updateMaxHealth(HappyGhastEntity ghast) {
        if (this.ghastData == null) return;
        
        float maxHealth = ghastData.getMaxHealth();
        
        // 获取当前血量上限属性
        var maxHealthAttribute = ghast.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHealthAttribute != null && maxHealthAttribute.getBaseValue() != maxHealth) {
            // 设置新的血量上限
            maxHealthAttribute.setBaseValue(maxHealth);
            
            // 如果当前血量超过新上限，调整当前血量
            if (ghast.getHealth() > maxHealth) {
                ghast.setHealth(maxHealth);
            }
        }
    }
}
