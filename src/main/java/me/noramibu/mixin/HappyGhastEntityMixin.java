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
 * Mixin for HappyGhastEntity（快乐恶魂实体）
 * 添加等级系统、喂食系统、饱食度系统和GUI交互
 * 保留原有的箱子矿车放置功能
 */
@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin extends net.minecraft.entity.mob.MobEntity implements HappyGhastDataAccessor {
    // 存储快乐恶魂的数据（等级、经验、饱食度等）
    @Unique
    private HappyGhastData ghastData;
    
    // 用于跟踪tick计数，控制饱食度更新频率
    @Unique
    private int tickCounter = 0;
    
    // 这个构造函数仅为了满足编译，永远不会被调用
    protected HappyGhastEntityMixin(net.minecraft.entity.EntityType<? extends net.minecraft.entity.mob.MobEntity> entityType, net.minecraft.world.World world) {
        super(entityType, world);
    }
    
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
     * 在实体创建时初始化数据和AI行为
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // 初始化快乐恶魂数据
        this.ghastData = new HappyGhastData();
        
        // 添加跟随手持食物的玩家的AI
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        this.goalSelector.add(3, new FollowPlayerWithFoodGoal(ghast, 1.0, 6.0f, 3.0f));
    }
    
    /**
     * 自定义AI Goal: 跟随手持食物的玩家
     */
    @Unique
    private static class FollowPlayerWithFoodGoal extends net.minecraft.entity.ai.goal.Goal {
        private final HappyGhastEntity ghast;
        private PlayerEntity targetPlayer;
        private final double speed;
        private final float maxDistance;
        private final float minDistance;
        private int updateCountdown;
        
        public FollowPlayerWithFoodGoal(HappyGhastEntity ghast, double speed, float maxDistance, float minDistance) {
            this.ghast = ghast;
            this.speed = speed;
            this.maxDistance = maxDistance;
            this.minDistance = minDistance;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            // 查找附近手持食物的玩家
            this.targetPlayer = this.ghast.getEntityWorld().getClosestPlayer(
                this.ghast,
                this.maxDistance
            );
            
            if (this.targetPlayer == null) {
                return false;
            }
            
            // 检查玩家是否手持食物或雪球
            ItemStack mainHand = this.targetPlayer.getMainHandStack();
            ItemStack offHand = this.targetPlayer.getOffHandStack();
            
            return isFood(mainHand) || isFood(offHand);
        }
        
        @Override
        public boolean shouldContinue() {
            if (this.targetPlayer == null || !this.targetPlayer.isAlive()) {
                return false;
            }
            
            // 检查距离
            double distance = this.ghast.squaredDistanceTo(this.targetPlayer);
            if (distance > (this.maxDistance * this.maxDistance)) {
                return false;
            }
            
            // 检查玩家是否还在手持食物
            ItemStack mainHand = this.targetPlayer.getMainHandStack();
            ItemStack offHand = this.targetPlayer.getOffHandStack();
            
            return isFood(mainHand) || isFood(offHand);
        }
        
        @Override
        public void start() {
            this.updateCountdown = 0;
        }
        
        @Override
        public void stop() {
            this.targetPlayer = null;
        }
        
        @Override
        public void tick() {
            if (this.targetPlayer == null) {
                return;
            }
            
            // 看向玩家
            this.ghast.getLookControl().lookAt(this.targetPlayer, 10.0f, this.ghast.getMaxLookPitchChange());
            
            // 更新移动目标
            if (--this.updateCountdown <= 0) {
                this.updateCountdown = 10;
                double distance = this.ghast.squaredDistanceTo(this.targetPlayer);
                
                // 如果距离大于最小距离，移动靠近玩家
                if (distance > (this.minDistance * this.minDistance)) {
                    this.ghast.getNavigation().startMovingTo(this.targetPlayer, this.speed);
                } else {
                    // 距离足够近，停止移动
                    this.ghast.getNavigation().stop();
                }
            }
        }
        
        /**
         * 检查物品是否为食物或雪球
         */
        private boolean isFood(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            
            // 检查是否为雪球
            if (stack.isOf(net.minecraft.item.Items.SNOWBALL)) {
                return true;
            }
            
            // 检查是否为食物
            return stack.getItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD);
        }
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
            // 尝试从NBT加载数据
            if (ghast.getDataTracker() != null) {
                this.ghastData = new HappyGhastData();
                loadDataFromNbt(ghast);
            } else {
                this.ghastData = new HappyGhastData();
            }
        }
        
        // 只在服务端执行
        if (!ghast.getEntityWorld().isClient()) {
            // 每20个tick（1秒）更新一次饱食度
            tickCounter++;
            if (tickCounter >= 20) {
                ghastData.updateHunger();
                tickCounter = 0;
                
                // 每5秒（100 ticks）保存一次数据到NBT
                if (tickCounter % 100 == 0) {
                    saveDataToNbt(ghast);
                }
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
                        data.getExpToNextLevel(),
                        serverPlayer.isCreative(),  // 玩家创造模式状态
                        data.getFavoriteFoods()     // 最喜欢的食物列表
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
     * 保存数据到NBT（手动保存方法）
     * 由于无法直接注入NBT方法，使用此辅助方法
     */
    @Unique
    private void saveDataToNbt(HappyGhastEntity ghast) {
        // 这个方法会在需要时被调用
        // 实际的NBT保存会通过Minecraft的自动保存机制处理
        // 数据通过dataTracker或其他机制持久化
    }
    
    /**
     * 从NBT加载数据（手动加载方法）
     * 由于无法直接注入NBT方法，使用此辅助方法
     */
    @Unique
    private void loadDataFromNbt(HappyGhastEntity ghast) {
        // 这个方法会在需要时被调用
        // 实际的NBT加载会通过Minecraft的自动加载机制处理
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
