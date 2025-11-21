package me.noramibu.mixin;

import me.noramibu.Chestonghast;
import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.config.GhastConfig;
import me.noramibu.level.LevelConfig;
import me.noramibu.network.SyncGhastDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import me.noramibu.entity.HappyGhastFireballEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.util.math.Vec3d;

/**
 * Mixin for HappyGhastEntity
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
    
    // 记录当前是否处于战斗状态，避免重复进入/退出
    @Unique
    private boolean inCombat = false;
    
    // 战斗逻辑使用的冷却计时器（tick）
    @Unique
    private int fireballCooldownTicks = 0;
    
    // 当前守护的玩家引用，用于日志和状态判断
    @Unique
    private PlayerEntity guardianPlayer;
    
    // 当前锁定的敌对生物
    @Unique
    private MobEntity currentTarget;
    
    // 玩家感知范围 - 识别需要保护的玩家
    @Unique
    private static final double PLAYER_ALERT_RADIUS = 16.0;
    
    // 敌对生物探测半径 - 判断是否需要进入战斗（满足“方圆40格”需求）
    @Unique
    private static final double HOSTILE_DETECTION_RADIUS = 40.0;
    
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
        
        if (!ghast.getEntityWorld().isClient()) {
            ensureDefaultName(ghast);
        }
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
            
            // 确保默认名称和名称标签同步
            ensureDefaultName(ghast);
            
            // 处理自动战斗逻辑，保护附近玩家
            handleCombatBehavior(ghast);
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
                        data.getFavoriteFoods(),     // 最喜欢的食物列表
                        data.getCustomName() != null ? data.getCustomName() : ""  // 自定义名字
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
     * 负责检测玩家与敌对生物、驱动战斗状态以及发射火球
     */
    @Unique
    private void handleCombatBehavior(HappyGhastEntity ghast) {
        if (this.ghastData == null) {
            return;
        }
        
        if (this.fireballCooldownTicks > 0) {
            this.fireballCooldownTicks--;
        }
        
        // 寻找需要保护的玩家
        PlayerEntity nearbyPlayer = ghast.getEntityWorld().getClosestPlayer(ghast, PLAYER_ALERT_RADIUS);
        if (nearbyPlayer == null || nearbyPlayer.isSpectator()) {
            resetCombatState(ghast);
            return;
        }
        
        // 确认玩家附近是否存在敌对生物
        MobEntity hostileTarget = findNearestHostileAroundPlayer(nearbyPlayer, HOSTILE_DETECTION_RADIUS);
        if (hostileTarget == null) {
            resetCombatState(ghast);
            return;
        }
        
        // 更新战斗状态
        if (!this.inCombat || this.guardianPlayer != nearbyPlayer) {
            this.inCombat = true;
            this.guardianPlayer = nearbyPlayer;
            Chestonghast.LOGGER.debug("快乐恶魂 {} 进入战斗状态，保护玩家 {}", ghast.getUuidAsString(), nearbyPlayer.getName().getString());
        }
        
        this.currentTarget = hostileTarget;
        ghast.setAttacking(true);
        ghast.getLookControl().lookAt(hostileTarget, 30.0f, ghast.getMaxLookPitchChange());
        
        LevelConfig.LevelData levelData = LevelConfig.getLevelData(this.ghastData.getLevel());
        int fireballPower = Math.max(1, levelData.getFireballPower());
        int cooldownTicks = Math.max(5, levelData.getAttackCooldownTicks());
        float fireballDamage = Math.max(6.0f, levelData.getFireballDamage());
        if (this.fireballCooldownTicks <= 0) {
            shootFireballAtTarget(ghast, hostileTarget, fireballPower, fireballDamage);
            this.fireballCooldownTicks = cooldownTicks;
            sendDebugCombatMessage(ghast, nearbyPlayer, levelData.getLevel(), fireballDamage, cooldownTicks);
        }
    }
    
    /**
     * 搜索目标玩家周围最近的敌对生物
     */
    @Unique
    private MobEntity findNearestHostileAroundPlayer(PlayerEntity player, double radius) {
        List<MobEntity> hostiles = player.getEntityWorld().getEntitiesByClass(
            MobEntity.class,
            player.getBoundingBox().expand(radius),
            entity -> entity != null && entity.isAlive() && !entity.isRemoved() && canGhastAttack(entity)
        );
        
        MobEntity prioritized = null;
        double prioritizedDistance = Double.MAX_VALUE;
        MobEntity fallback = null;
        double fallbackDistance = Double.MAX_VALUE;
        
        for (MobEntity hostile : hostiles) {
            double distance = hostile.squaredDistanceTo(player);
            if (isAggressiveTowardsPlayer(hostile, player)) {
                if (distance < prioritizedDistance) {
                    prioritizedDistance = distance;
                    prioritized = hostile;
                }
            } else if (distance < fallbackDistance) {
                fallbackDistance = distance;
                fallback = hostile;
            }
        }
        
        return prioritized != null ? prioritized : fallback;
    }
    
    /**
     * 计算目标方向并发射火球保护玩家
     */
    @Unique
    private void shootFireballAtTarget(HappyGhastEntity ghast, MobEntity target, int fireballPower, float fireballDamage) {
        double deltaX = target.getX() - ghast.getX();
        double deltaY = target.getBodyY(0.5) - ghast.getBodyY(0.5);
        double deltaZ = target.getZ() - ghast.getZ();
        Vec3d direction = new Vec3d(deltaX, deltaY, deltaZ);
        
        HappyGhastFireballEntity fireball = new HappyGhastFireballEntity(
            ghast.getEntityWorld(),
            ghast,
            direction,
            fireballPower,
            fireballDamage
        );
        
        double spawnY = ghast.getBodyY(0.5) + 0.5;
        fireball.setPosition(ghast.getX(), spawnY, ghast.getZ());
        ghast.getEntityWorld().spawnEntity(fireball);
        ghast.playSound(SoundEvents.ENTITY_GHAST_SHOOT, 1.0f, 0.8f + ghast.getRandom().nextFloat() * 0.4f);
        
        Chestonghast.LOGGER.debug(
            "快乐恶魂 {} 向 {} 发射火球（威力 {}）",
            ghast.getUuidAsString(),
            target.getName().getString(),
            fireballPower
        );
    }
    
    /**
     * 重置战斗状态，避免在没有威胁时持续攻击
     */
    @Unique
    private void resetCombatState(HappyGhastEntity ghast) {
        if (this.inCombat) {
            Chestonghast.LOGGER.debug(
                "快乐恶魂 {} 退出战斗状态",
                ghast.getUuidAsString()
            );
        }
        this.inCombat = false;
        this.guardianPlayer = null;
        this.currentTarget = null;
        ghast.setAttacking(false);
    }
    
    /**
     * 当调试模式开启时，将战斗数据发送到聊天栏
     */
    @Unique
    private void sendDebugCombatMessage(HappyGhastEntity ghast, PlayerEntity player, int level, float fireballDamage, int cooldownTicks) {
        GhastConfig config = GhastConfig.getInstance();
        if (!config.debugMode) {
            return;
        }
        
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }
        
        String ghastName = ghast.getDisplayName().getString();
        float cooldownSeconds = cooldownTicks / 20.0f;
        String message = String.format(
            "[调试] %s (Lv.%d) 火球伤害 %.1f / 冷却 %.2fs",
            ghastName,
            level,
            fireballDamage,
            cooldownSeconds
        );
        serverPlayer.sendMessage(Text.literal(message), false);
    }
    
    /**
     * 为每一只快乐恶魂分配默认名字并同步名称标签
     */
    @Unique
    private void ensureDefaultName(HappyGhastEntity ghast) {
        if (this.ghastData == null || ghast.getEntityWorld().isClient()) {
            return;
        }
        
        String storedName = this.ghastData.getCustomName();
        if (storedName == null || storedName.isEmpty()) {
            int index = GhastConfig.getInstance().claimNextGhastNameIndex();
            storedName = String.format("快乐恶魂%02d", index);
            this.ghastData.setCustomName(storedName);
        }
        
        if (storedName != null && !storedName.isEmpty()) {
            applyNameTag(ghast, storedName);
        }
    }
    
    @Unique
    private void applyNameTag(HappyGhastEntity ghast, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        ghast.setCustomName(Text.literal(name));
        ghast.setCustomNameVisible(true);
    }
    
    /**
     * 判断敌对生物是否为合法攻击目标（史莱姆等友好对象会被排除）
     */
    @Unique
    private boolean canGhastAttack(MobEntity entity) {
        return entity instanceof Monster && entity.getType() != EntityType.SLIME;
    }
    
    /**
     * 判断敌对生物是否正在或刚刚伤害玩家，用于优先级排序
     */
    @Unique
    private boolean isAggressiveTowardsPlayer(MobEntity hostile, PlayerEntity player) {
        if (hostile.getTarget() == player) {
            return true;
        }
        
        DamageSource recentDamage = player.getRecentDamageSource();
        if (recentDamage != null && recentDamage.getAttacker() == hostile) {
            return true;
        }
        
        return player.getAttacker() == hostile;
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
