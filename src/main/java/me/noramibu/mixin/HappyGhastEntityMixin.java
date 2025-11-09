package me.noramibu.mixin;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.level.LevelConfig;
import me.noramibu.network.SyncGhastDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.EquipmentSlot;

import java.util.Comparator;
import java.util.List;

/**
 * Mixin for HappyGhastEntity
 * 添加等级系统、喂食系统、饱食度系统、战斗系统和GUI交互
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
    
    // 用于追踪发射的火球和对应的等级
    @Unique
    private final java.util.Map<Integer, Integer> fireballLevels = new java.util.HashMap<>();
    
    // 用于存储火球最后已知位置
    @Unique
    private final java.util.Map<Integer, Vec3d> fireballPositions = new java.util.HashMap<>();
    
    // 用于追踪魅惑效果云（存储效果云ID和魅惑等级）
    @Unique
    private final java.util.Map<Integer, Integer> charmClouds = new java.util.HashMap<>();
    
    // 魅惑效果的处理间隔（ticks）
    @Unique
    private int charmTickCounter = 0;
    
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
        
        // 添加AI行为
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        
        // 优先级1：攻击附近的敌对生物（保护玩家）
        this.goalSelector.add(1, new AttackHostilesGoal(ghast));
        
        // 优先级3：跟随手持食物的玩家
        this.goalSelector.add(3, new FollowPlayerWithFoodGoal(ghast, 1.0, 6.0f, 3.0f));
    }
    
    /**
     * 自定义AI Goal: 攻击附近的敌对生物
     * 快乐恶魂会向附近的怪物发射火球来保护玩家
     * 火球威力和冷却时间随等级变化
     */
    @Unique
    private static class AttackHostilesGoal extends net.minecraft.entity.ai.goal.Goal {
        private final HappyGhastEntity ghast;
        private final HappyGhastDataAccessor dataAccessor;
        private LivingEntity targetHostile;
        private int fireballCooldown;
        private static final double ATTACK_RANGE = 16.0; // 攻击范围16格
        
        public AttackHostilesGoal(HappyGhastEntity ghast) {
            this.ghast = ghast;
            // 获取数据访问器引用，用于读取当前等级
            this.dataAccessor = (HappyGhastDataAccessor) ghast;
            this.fireballCooldown = 0;
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            // 查找附近最近的敌对生物
            this.targetHostile = findNearestHostile();
            return this.targetHostile != null;
        }
        
        @Override
        public boolean shouldContinue() {
            if (this.targetHostile == null || !this.targetHostile.isAlive()) {
                return false;
            }
            
            // 检查距离，如果目标太远则停止追击
            double distance = this.ghast.squaredDistanceTo(this.targetHostile);
            if (distance > (ATTACK_RANGE * ATTACK_RANGE * 1.5)) { // 稍微增加一些容差
                return false;
            }
            
            return true;
        }
        
        @Override
        public void start() {
            this.fireballCooldown = 0;
        }
        
        @Override
        public void stop() {
            this.targetHostile = null;
        }
        
        @Override
        public void tick() {
            if (this.targetHostile == null) {
                return;
            }
            
            // 减少冷却时间
            if (this.fireballCooldown > 0) {
                this.fireballCooldown--;
            }
            
            double distance = this.ghast.squaredDistanceTo(this.targetHostile);
            
            // 看向目标
            this.ghast.getLookControl().lookAt(this.targetHostile, 10.0f, this.ghast.getMaxLookPitchChange());
            
            // 如果距离合适且冷却完成，发射火球
            if (distance <= (ATTACK_RANGE * ATTACK_RANGE) && this.fireballCooldown <= 0) {
                shootFireball();
                
                // 根据当前等级获取冷却时间
                int currentLevel = this.dataAccessor.getGhastData().getLevel();
                this.fireballCooldown = LevelConfig.getAttackCooldown(currentLevel);
            }
        }
        
        /**
         * 查找附近最近的敌对生物
         * 优先攻击靠近玩家的怪物
         */
        private LivingEntity findNearestHostile() {
            // 获取攻击范围内的所有敌对生物
            Box searchBox = this.ghast.getBoundingBox().expand(ATTACK_RANGE);
            List<HostileEntity> hostiles = this.ghast.getEntityWorld().getEntitiesByClass(
                HostileEntity.class,
                searchBox,
                entity -> entity.isAlive() && !entity.isSpectator()
            );
            
            if (hostiles.isEmpty()) {
                return null;
            }
            
            // 查找最近的玩家作为参考点
            PlayerEntity nearestPlayer = this.ghast.getEntityWorld().getClosestPlayer(this.ghast, ATTACK_RANGE * 2);
            
            if (nearestPlayer != null) {
                // 如果有玩家在附近，优先攻击靠近玩家的怪物
                final PlayerEntity player = nearestPlayer;
                return hostiles.stream()
                    .min(Comparator.comparingDouble(hostile -> hostile.squaredDistanceTo(player)))
                    .orElse(null);
            } else {
                // 否则攻击离快乐恶魂最近的怪物
                return hostiles.stream()
                    .min(Comparator.comparingDouble(hostile -> hostile.squaredDistanceTo(this.ghast)))
                    .orElse(null);
            }
        }
        
        /**
         * 发射火球攻击目标
         * 使用类似原版恶魂的火球发射逻辑
         * 火球威力根据当前等级从配置文件读取
         */
        private void shootFireball() {
            if (this.targetHostile == null) {
                return;
            }
            
            // 获取当前等级和对应的火球威力
            int currentLevel = this.dataAccessor.getGhastData().getLevel();
            int fireballPower = LevelConfig.getFireballPower(currentLevel);
            
            // 计算发射方向
            double targetX = this.targetHostile.getX();
            double targetY = this.targetHostile.getBodyY(0.5);
            double targetZ = this.targetHostile.getZ();
            
            double deltaX = targetX - this.ghast.getX();
            double deltaY = targetY - this.ghast.getY();
            double deltaZ = targetZ - this.ghast.getZ();
            
            Vec3d direction = new Vec3d(deltaX, deltaY, deltaZ);
            
            // 使用附魔辅助类发射火球（支持连射等附魔）
            me.noramibu.enchantment.EnchantmentHelper.shootFireballWithEnchantments(
                this.ghast, 
                direction, 
                fireballPower, 
                this.targetHostile
            );
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
        
        // 安全检查：确保实体和世界有效
        if (ghast.isRemoved() || ghast.getEntityWorld() == null) {
            return;
        }
        
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
            
            // 检查并处理火球击中后的效果云生成
            checkAndSpawnEffectClouds(ghast);
            
            // 处理魅惑效果云（每10 ticks检查一次）
            charmTickCounter++;
            if (charmTickCounter >= 10) {
                processCharmClouds(ghast);
                charmTickCounter = 0;
            }
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
    
    /**
     * 追踪发射的火球
     * @param fireball 火球实体
     * @param level 快乐恶魂等级
     */
    @Unique
    public void trackFireball(FireballEntity fireball, int level) {
        fireballLevels.put(fireball.getId(), level);
        fireballPositions.put(fireball.getId(), new Vec3d(fireball.getX(), fireball.getY(), fireball.getZ()));
    }
    
    /**
     * 检查火球并生成效果云
     * 每tick检查追踪的火球是否已击中目标
     */
    @Unique
    private void checkAndSpawnEffectClouds(HappyGhastEntity ghast) {
        if (fireballLevels.isEmpty()) return;
        
        // 定期清理过期数据（每200 ticks / 10秒）
        if (tickCounter % 200 == 0 && fireballLevels.size() > 100) {
            // 如果Map过大，清理所有数据以防止内存泄漏
            fireballLevels.clear();
            fireballPositions.clear();
            return;
        }
        
        // 检查每个追踪的火球
        java.util.Iterator<java.util.Map.Entry<Integer, Integer>> iterator = fireballLevels.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Integer, Integer> entry = iterator.next();
            int fireballId = entry.getKey();
            int level = entry.getValue();
            
            // 尝试获取火球实体
            Entity entity = ghast.getEntityWorld().getEntityById(fireballId);
            
            if (entity instanceof FireballEntity fireball) {
                // 火球还存在，更新最后已知位置
                fireballPositions.put(fireballId, new Vec3d(fireball.getX(), fireball.getY(), fireball.getZ()));
            } else {
                // 火球已消失（击中目标），生成效果云
                Vec3d lastPos = fireballPositions.get(fireballId);
                if (lastPos != null && level >= 3) {
                    spawnEffectCloud(ghast.getEntityWorld(), lastPos, level);
                }
                
                // 清理记录
                iterator.remove();
                fireballPositions.remove(fireballId);
            }
        }
    }
    
    /**
     * 在指定位置生成效果云（支持附魔效果）
     * @param world 世界
     * @param pos 位置
     * @param level 快乐恶魂等级
     */
    @Unique
    private void spawnEffectCloud(net.minecraft.world.World world, Vec3d pos, int level) {
        // 获取等级配置
        me.noramibu.config.GhastConfig.LevelConfig config = me.noramibu.config.GhastConfig.getInstance().getLevelConfig(level);
        
        // 检查是否启用效果云
        if (!config.enableEffectCloud) return;
        
        // 获取快乐恶魂实体（用于检查附魔）
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        
        // 计算最终持续时间（考虑持久附魔）
        int finalDuration = calculateEffectCloudDuration(ghast, config.cloudDuration);
        
        // 创建效果云实体
        net.minecraft.entity.AreaEffectCloudEntity cloud = new net.minecraft.entity.AreaEffectCloudEntity(world, pos.x, pos.y, pos.z);
        
        // 设置基本属性
        cloud.setRadius(config.cloudRadius);  // 效果云半径
        cloud.setDuration(finalDuration);  // 持续时间（已应用附魔）
        cloud.setRadiusOnUse(-0.5F);  // 每次作用时半径缩小量
        cloud.setWaitTime(10);  // 生成后延迟10 ticks才开始生效
        cloud.setRadiusGrowth(-cloud.getRadius() / (float)finalDuration);  // 半径随时间缓慢缩小
        
        // 设置粒子效果（根据附魔选择不同粒子）
        int freezingLevel = me.noramibu.enchantment.EnchantmentHelper.getEnchantmentLevel(
            ghast, 
            me.noramibu.enchantment.FireballEnchantment.FREEZING
        );
        
        // 检查魅惑附魔（优先显示）
        int charmLevel = me.noramibu.enchantment.EnchantmentHelper.getEnchantmentLevel(
            ghast, 
            me.noramibu.enchantment.FireballEnchantment.CHARM
        );
        
        if (charmLevel > 0) {
            // 有魅惑附魔，使用魔法粒子（紫色）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.WITCH);
        } else if (freezingLevel > 0) {
            // 有冰冻附魔，使用雪花粒子（白色）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.SNOWFLAKE);
        } else {
            // 没有特殊附魔，使用治疗粒子（绿色）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER);
        }
        
        // 添加对怪物的伤害效果（瞬间伤害）
        net.minecraft.entity.effect.StatusEffectInstance damageEffect = new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.INSTANT_DAMAGE,
            1,  // 持续时间1 tick（瞬间效果）
            config.damageAmplifier,  // 强度（0=I级，1=II级）
            false,  // 不显示环境粒子
            false  // 不在HUD显示图标
        );
        cloud.addEffect(damageEffect);
        
        // 添加冰冻效果（如果有冰冻附魔）
        if (freezingLevel > 0) {
            addFreezingEffect(cloud, freezingLevel);
        }
        
        // 如果有魅惑附魔，追踪这个效果云（魅惑等级已在上面检查过）
        if (charmLevel > 0) {
            // 在生成效果云后追踪它
            world.spawnEntity(cloud);
            charmClouds.put(cloud.getId(), charmLevel);
            return;  // 提前返回，避免重复生成（魅惑云不需要治疗效果）
        }
        
        // 添加对玩家的生命恢复效果（使用增强后的持续时间）
        net.minecraft.entity.effect.StatusEffectInstance regenEffect = new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.REGENERATION,
            finalDuration,  // 持续整个效果云时间（已应用附魔）
            config.regenAmplifier,  // 强度（0=I级，1=II级，2=III级）
            false,  // 不显示环境粒子
            true  // 在HUD显示图标
        );
        cloud.addEffect(regenEffect);
        
        // 添加速度提升效果给玩家（小幅度，使用增强后的持续时间）
        net.minecraft.entity.effect.StatusEffectInstance speedEffect = new net.minecraft.entity.effect.StatusEffectInstance(
            net.minecraft.entity.effect.StatusEffects.SPEED,
            finalDuration / 2,  // 持续一半时间（已应用附魔）
            0,  // I级速度
            false,
            true
        );
        cloud.addEffect(speedEffect);
        
        // 生成效果云
        world.spawnEntity(cloud);
    }
    
    /**
     * 计算效果云持续时间（考虑持久附魔）
     * @param ghast 快乐恶魂实体
     * @param baseDuration 基础持续时间
     * @return 最终持续时间
     */
    @Unique
    private int calculateEffectCloudDuration(HappyGhastEntity ghast, int baseDuration) {
        // 检查持久附魔等级
        int durationLevel = me.noramibu.enchantment.EnchantmentHelper.getEnchantmentLevel(
            ghast, 
            me.noramibu.enchantment.FireballEnchantment.DURATION
        );
        
        if (durationLevel <= 0) {
            // 没有持久附魔，返回基础时间
            return baseDuration;
        }
        
        // 根据附魔等级计算倍数
        float multiplier;
        switch (durationLevel) {
            case 1:
                multiplier = 1.5f;  // I级：1.5倍
                break;
            case 2:
                multiplier = 2.0f;  // II级：2倍
                break;
            case 3:
                multiplier = 3.0f;  // III级：3倍
                break;
            default:
                multiplier = 1.0f;
        }
        
        // 返回增强后的持续时间
        return Math.round(baseDuration * multiplier);
    }
    
    /**
     * 添加冰冻效果到效果云
     * @param cloud 效果云实体
     * @param freezingLevel 冰冻附魔等级
     */
    @Unique
    private void addFreezingEffect(net.minecraft.entity.AreaEffectCloudEntity cloud, int freezingLevel) {
        // 根据附魔等级确定冰冻参数
        int duration;      // 持续时间（ticks）
        int amplifier;     // 缓慢强度
        
        switch (freezingLevel) {
            case 1:
                duration = 60;      // 3秒
                amplifier = 4;      // 缓慢V（基本无法移动）
                break;
            case 2:
                duration = 100;     // 5秒
                amplifier = 6;      // 缓慢VII（完全冻结）
                break;
            case 3:
                duration = 160;     // 8秒
                amplifier = 9;      // 缓慢X（超级冻结）
                break;
            default:
                duration = 60;
                amplifier = 4;
        }
        
        // 添加缓慢效果（对所有实体）
        net.minecraft.entity.effect.StatusEffectInstance slownessEffect = 
            new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SLOWNESS,
                duration,
                amplifier,
                false,  // 不显示环境粒子
                true    // 在HUD显示图标
            );
        cloud.addEffect(slownessEffect);
        
        // 添加挖掘疲劳效果（防止怪物攻击）
        net.minecraft.entity.effect.StatusEffectInstance miningFatigueEffect = 
            new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.MINING_FATIGUE,
                duration,
                amplifier,
                false,  // 不显示环境粒子
                true    // 在HUD显示图标
            );
        cloud.addEffect(miningFatigueEffect);
        
        // 为玩家添加额外的速度效果来部分抵消缓慢
        // 注意：效果云的效果会对所有实体生效，所以玩家也会被减速
        // 但玩家通常有装备和其他buff可以抵消
        // 这里添加一个额外的速度buff给玩家一些补偿
        net.minecraft.entity.effect.StatusEffectInstance speedBoostEffect = 
            new net.minecraft.entity.effect.StatusEffectInstance(
                net.minecraft.entity.effect.StatusEffects.SPEED,
                duration,
                Math.min(amplifier / 2, 2),  // 速度效果（较弱，给玩家一些补偿）
                false,
                true
            );
        cloud.addEffect(speedBoostEffect);
    }
    
    /**
     * 处理所有魅惑效果云
     * 检查追踪的效果云并应用魅惑效果
     */
    @Unique
    private void processCharmClouds(HappyGhastEntity ghast) {
        if (charmClouds.isEmpty()) return;
        
        // 定期清理过期数据（每200 ticks / 10秒）
        if (tickCounter % 200 == 0 && charmClouds.size() > 50) {
            // 如果Map过大，清理所有数据以防止内存泄漏
            charmClouds.clear();
            return;
        }
        
        // 检查每个追踪的效果云
        java.util.Iterator<java.util.Map.Entry<Integer, Integer>> iterator = charmClouds.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Integer, Integer> entry = iterator.next();
            int cloudId = entry.getKey();
            int charmLevel = entry.getValue();
            
            // 尝试获取效果云实体
            Entity entity = ghast.getEntityWorld().getEntityById(cloudId);
            
            if (entity instanceof net.minecraft.entity.AreaEffectCloudEntity cloud) {
                // 效果云还存在，应用魅惑效果
                applyCharmEffect(ghast.getEntityWorld(), cloud, charmLevel);
            } else {
                // 效果云已消失，移除追踪
                iterator.remove();
            }
        }
    }
    
    /**
     * 应用魅惑效果：让效果云范围内的怪物互相攻击
     * @param world 世界
     * @param cloud 效果云实体
     * @param charmLevel 魅惑附魔等级
     */
    @Unique
    private void applyCharmEffect(net.minecraft.world.World world, net.minecraft.entity.AreaEffectCloudEntity cloud, int charmLevel) {
        // 获取效果云位置和半径
        Vec3d cloudPos = new Vec3d(cloud.getX(), cloud.getY(), cloud.getZ());
        double radius = cloud.getRadius();
        
        // 根据附魔等级确定魅惑参数
        float damageAmount;    // 互相伤害量
        
        switch (charmLevel) {
            case 1:
                damageAmount = 2.0f;  // 1颗心
                break;
            case 2:
                damageAmount = 4.0f;  // 2颗心
                break;
            case 3:
                damageAmount = 6.0f;  // 3颗心
                break;
            default:
                damageAmount = 2.0f;
        }
        
        // 获取效果云范围内的所有敌对生物
        Box searchBox = new Box(
            cloudPos.x - radius, cloudPos.y - radius, cloudPos.z - radius,
            cloudPos.x + radius, cloudPos.y + radius, cloudPos.z + radius
        );
        
        List<HostileEntity> hostiles = world.getEntitiesByClass(
            HostileEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved()
        );
        
        // 如果范围内有2个或更多怪物，让它们互相攻击
        if (hostiles.size() >= 2) {
            // 限制处理的怪物数量，防止性能问题
            int maxProcessed = Math.min(hostiles.size(), 20);  // 最多处理20个怪物
            
            // 只在服务端执行
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            
            // 让每个怪物攻击范围内的另一个随机怪物
            for (int i = 0; i < maxProcessed; i++) {
                HostileEntity attacker = hostiles.get(i);
                
                // 随机选择一个不是自己的目标（优化：直接从列表中随机选，避免创建副本）
                int targetIndex = world.getRandom().nextInt(hostiles.size());
                HostileEntity target = hostiles.get(targetIndex);
                
                // 如果选到自己，选择下一个（循环）
                if (target == attacker) {
                    targetIndex = (targetIndex + 1) % hostiles.size();
                    target = hostiles.get(targetIndex);
                }
                
                // 让attacker对target造成伤害
                target.damage(
                    serverWorld,
                    world.getDamageSources().mobAttack(attacker),
                    damageAmount
                );
                
                // 生成攻击粒子效果（愤怒粒子）
                serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.ANGRY_VILLAGER,
                    attacker.getX(),
                    attacker.getY() + attacker.getHeight() / 2,
                    attacker.getZ(),
                    3,  // 粒子数量
                    0.3, 0.3, 0.3,  // 偏移
                    0.0  // 速度
                );
            }
        }
    }
}
