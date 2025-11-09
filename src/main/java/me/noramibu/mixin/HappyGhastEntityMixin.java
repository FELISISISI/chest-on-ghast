package me.noramibu.mixin;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.ai.AttackHostilesGoal;
import me.noramibu.ai.FollowPlayerWithFoodGoal;
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
    
    // 用于追踪引力奇点（存储效果云ID和引力等级）
    @Unique
    private final java.util.Map<Integer, Integer> gravityClouds = new java.util.HashMap<>();
    
    // 引力奇点的处理间隔（ticks）
    @Unique
    private int gravityTickCounter = 0;
    
    // 用于追踪变形效果云（存储效果云ID和变形等级）
    @Unique
    private final java.util.Map<Integer, Integer> polymorphClouds = new java.util.HashMap<>();
    
    // 变形效果的处理间隔（ticks）
    @Unique
    private int polymorphTickCounter = 0;
    
    // 用于追踪已变形的实体ID（防止重复变形）
    @Unique
    private final java.util.Set<Integer> polymorphedEntities = new java.util.HashSet<>();
    
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
            
            // 处理引力奇点（每2 ticks检查一次，更频繁以保证流畅拉取）
            gravityTickCounter++;
            if (gravityTickCounter >= 2) {
                processGravityClouds(ghast);
                gravityTickCounter = 0;
            }
            
            // 处理滑稽变形（每5 ticks检查一次）
            polymorphTickCounter++;
            if (polymorphTickCounter >= 5) {
                processPolymorphClouds(ghast);
                polymorphTickCounter = 0;
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
        // 只在服务端生成效果云
        if (!(world instanceof ServerWorld)) {
            return;
        }
        
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
        
        // 检查魅惑附魔
        int charmLevel = me.noramibu.enchantment.EnchantmentHelper.getEnchantmentLevel(
            ghast, 
            me.noramibu.enchantment.FireballEnchantment.CHARM
        );
        
        // 检查滑稽变形附魔
        int polymorphLevel = me.noramibu.enchantment.EnchantmentHelper.getEnchantmentLevel(
            ghast, 
            me.noramibu.enchantment.FireballEnchantment.POLYMORPH
        );
        
        // 检查引力奇点附魔
        int gravityLevel = me.noramibu.enchantment.EnchantmentHelper.getEnchantmentLevel(
            ghast, 
            me.noramibu.enchantment.FireballEnchantment.GRAVITY
        );
        
        if (polymorphLevel > 0) {
            // 有变形附魔，使用彩色爆炸粒子（欢乐感）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING);
        } else if (gravityLevel > 0) {
            // 有引力奇点附魔，使用黑洞粒子（黑色传送门粒子）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.PORTAL);
        } else if (charmLevel > 0) {
            // 有魅惑附魔，使用魔法粒子（紫色）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.WITCH);
        } else if (freezingLevel > 0) {
            // 有冰冻附魔，使用雪花粒子（白色）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.SNOWFLAKE);
        } else {
            // 没有特殊附魔，使用治疗粒子（绿色）
            cloud.setParticleType(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER);
        }
        
        // 对怪物不添加伤害效果，因为会导致AI异常
        // 火球本身的爆炸已经能造成伤害了
        // 效果云仅用于给玩家提供增益效果或特殊附魔效果
        
        // 添加冰冻效果（如果有冰冻附魔）
        if (freezingLevel > 0) {
            addFreezingEffect(cloud, freezingLevel);
        }
        
        // 如果有滑稽变形附魔，追踪这个效果云（最高优先级）
        if (polymorphLevel > 0) {
            // 变形效果不需要任何状态效果，只需要变形逻辑
            world.spawnEntity(cloud);
            polymorphClouds.put(cloud.getId(), polymorphLevel);
            return;  // 提前返回，变形效果不需要其他效果
        }
        
        // 如果有引力奇点附魔，追踪这个效果云
        if (gravityLevel > 0) {
            // 引力奇点不需要任何状态效果，只需要引力拉取
            world.spawnEntity(cloud);
            gravityClouds.put(cloud.getId(), gravityLevel);
            return;  // 提前返回，引力奇点不需要其他效果
        }
        
        // 如果有魅惑附魔，追踪这个效果云
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
    
    /**
     * 处理所有引力奇点
     * 检查追踪的奇点并应用引力拉取效果
     */
    @Unique
    private void processGravityClouds(HappyGhastEntity ghast) {
        if (gravityClouds.isEmpty()) return;
        
        // 定期清理过期数据（每200 ticks / 10秒）
        if (tickCounter % 200 == 0 && gravityClouds.size() > 30) {
            // 如果Map过大，清理所有数据以防止内存泄漏
            gravityClouds.clear();
            return;
        }
        
        // 检查每个追踪的引力奇点
        java.util.Iterator<java.util.Map.Entry<Integer, Integer>> iterator = gravityClouds.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Integer, Integer> entry = iterator.next();
            int cloudId = entry.getKey();
            int gravityLevel = entry.getValue();
            
            // 尝试获取效果云实体
            Entity entity = ghast.getEntityWorld().getEntityById(cloudId);
            
            if (entity instanceof net.minecraft.entity.AreaEffectCloudEntity cloud) {
                // 效果云还存在，应用引力拉取效果
                applyGravityEffect(ghast.getEntityWorld(), cloud, gravityLevel);
            } else {
                // 效果云已消失，移除追踪
                iterator.remove();
            }
        }
    }
    
    /**
     * 应用引力效果：将周围实体拉向奇点中心
     * @param world 世界
     * @param cloud 效果云实体（作为奇点中心）
     * @param gravityLevel 引力附魔等级
     */
    @Unique
    private void applyGravityEffect(net.minecraft.world.World world, net.minecraft.entity.AreaEffectCloudEntity cloud, int gravityLevel) {
        // 获取奇点位置
        Vec3d singularityPos = new Vec3d(cloud.getX(), cloud.getY(), cloud.getZ());
        
        // 根据附魔等级确定引力参数
        double pullRadius;    // 引力范围
        double pullStrength;  // 引力强度
        
        switch (gravityLevel) {
            case 1:
                pullRadius = 5.0;    // 5格范围
                pullStrength = 0.15;  // 较弱引力
                break;
            case 2:
                pullRadius = 8.0;    // 8格范围
                pullStrength = 0.25;  // 中等引力
                break;
            case 3:
                pullRadius = 12.0;   // 12格范围
                pullStrength = 0.4;   // 强力引力
                break;
            default:
                pullRadius = 5.0;
                pullStrength = 0.15;
        }
        
        // 搜索范围
        Box searchBox = new Box(
            singularityPos.x - pullRadius, singularityPos.y - pullRadius, singularityPos.z - pullRadius,
            singularityPos.x + pullRadius, singularityPos.y + pullRadius, singularityPos.z + pullRadius
        );
        
        // 获取范围内的所有敌对生物
        List<LivingEntity> entities = world.getEntitiesByClass(
            LivingEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved() && entity instanceof HostileEntity
        );
        
        // 限制处理的实体数量，防止性能问题
        int maxProcessed = Math.min(entities.size(), 30);  // 最多处理30个实体
        
        // 只在服务端执行
        if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        
        // 对每个实体应用引力
        for (int i = 0; i < maxProcessed; i++) {
            LivingEntity entity = entities.get(i);
            
            // 计算从实体到奇点的向量
            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            Vec3d toSingularity = singularityPos.subtract(entityPos);
            double distance = toSingularity.length();
            
            // 距离越近，引力越强（平方反比）
            if (distance > 0.5) {  // 避免除零和过度接近
                // 归一化方向向量
                Vec3d pullDirection = toSingularity.normalize();
                
                // 计算引力强度（距离越近越强）
                double distanceFactor = 1.0 - (distance / pullRadius);  // 0到1
                double actualPullStrength = pullStrength * distanceFactor * distanceFactor;  // 平方衰减
                
                // 应用速度（拉向中心）
                Vec3d pullVelocity = pullDirection.multiply(actualPullStrength);
                Vec3d currentVelocity = entity.getVelocity();
                Vec3d newVelocity = currentVelocity.add(pullVelocity);
                
                // 限制最大速度，防止实体被拉得太快
                double maxSpeed = 0.8;
                if (newVelocity.length() > maxSpeed) {
                    newVelocity = newVelocity.normalize().multiply(maxSpeed);
                }
                
                entity.setVelocity(newVelocity);
                entity.velocityModified = true;
            }
        }
        
        // 获取范围内的掉落物（物品实体）
        List<net.minecraft.entity.ItemEntity> items = world.getEntitiesByClass(
            net.minecraft.entity.ItemEntity.class,
            searchBox,
            item -> !item.isRemoved()
        );
        
        // 限制处理的物品数量
        int maxItems = Math.min(items.size(), 50);
        
        // 拉取掉落物
        for (int i = 0; i < maxItems; i++) {
            net.minecraft.entity.ItemEntity item = items.get(i);
            
            Vec3d itemPos = new Vec3d(item.getX(), item.getY(), item.getZ());
            Vec3d toSingularity = singularityPos.subtract(itemPos);
            double distance = toSingularity.length();
            
            if (distance > 0.5) {
                Vec3d pullDirection = toSingularity.normalize();
                double distanceFactor = 1.0 - (distance / pullRadius);
                double actualPullStrength = pullStrength * 0.5 * distanceFactor;  // 物品拉力稍弱
                
                Vec3d pullVelocity = pullDirection.multiply(actualPullStrength);
                Vec3d currentVelocity = item.getVelocity();
                Vec3d newVelocity = currentVelocity.add(pullVelocity);
                
                item.setVelocity(newVelocity);
                item.velocityModified = true;
            }
        }
        
        // 生成额外的黑洞粒子效果
        if (world.getTime() % 2 == 0) {  // 每2 ticks生成一次，减少性能消耗
            // 在奇点周围生成旋转的传送门粒子
            for (int i = 0; i < 5; i++) {
                double angle = (world.getTime() + i * 72) * 0.1;  // 旋转角度
                double radius = 1.0 + Math.sin(world.getTime() * 0.05) * 0.3;  // 脉动半径
                
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;
                
                serverWorld.spawnParticles(
                    net.minecraft.particle.ParticleTypes.PORTAL,
                    singularityPos.x + offsetX,
                    singularityPos.y + 0.5,
                    singularityPos.z + offsetZ,
                    1,  // 粒子数量
                    0.0, 0.1, 0.0,  // 偏移
                    0.1  // 速度
                );
            }
            
            // 中心暗色粒子（烟雾）
            serverWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.LARGE_SMOKE,
                singularityPos.x,
                singularityPos.y + 0.5,
                singularityPos.z,
                2,  // 粒子数量
                0.1, 0.1, 0.1,  // 偏移
                0.0  // 速度
            );
        }
    }
    
    /**
     * 处理所有滑稽变形效果云
     * 检查追踪的效果云并应用变形效果
     */
    @Unique
    private void processPolymorphClouds(HappyGhastEntity ghast) {
        if (polymorphClouds.isEmpty()) return;
        
        // 定期清理过期数据（每200 ticks / 10秒）
        if (tickCounter % 200 == 0) {
            if (polymorphClouds.size() > 20) {
                // 如果Map过大，清理所有数据以防止内存泄漏
                polymorphClouds.clear();
            }
            if (polymorphedEntities.size() > 100) {
                // 清理已变形实体记录
                polymorphedEntities.clear();
            }
            if (polymorphClouds.isEmpty() && polymorphedEntities.isEmpty()) {
                return;
            }
        }
        
        // 检查每个追踪的变形效果云
        java.util.Iterator<java.util.Map.Entry<Integer, Integer>> iterator = polymorphClouds.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<Integer, Integer> entry = iterator.next();
            int cloudId = entry.getKey();
            int polymorphLevel = entry.getValue();
            
            // 尝试获取效果云实体
            Entity entity = ghast.getEntityWorld().getEntityById(cloudId);
            
            if (entity instanceof net.minecraft.entity.AreaEffectCloudEntity cloud) {
                // 效果云还存在，应用变形效果
                applyPolymorphEffect(ghast.getEntityWorld(), cloud, polymorphLevel);
            } else {
                // 效果云已消失，移除追踪
                iterator.remove();
            }
        }
    }
    
    /**
     * 应用滑稽变形效果：将怪物变成无害的被动生物
     * @param world 世界
     * @param cloud 效果云实体
     * @param polymorphLevel 变形附魔等级
     */
    @Unique
    private void applyPolymorphEffect(net.minecraft.world.World world, net.minecraft.entity.AreaEffectCloudEntity cloud, int polymorphLevel) {
        // 获取效果云位置和半径
        Vec3d cloudPos = new Vec3d(cloud.getX(), cloud.getY(), cloud.getZ());
        double radius = cloud.getRadius();
        
        // 根据附魔等级确定变形概率
        double polymorphChance;
        
        switch (polymorphLevel) {
            case 1:
                polymorphChance = 0.33;  // 33%概率
                break;
            case 2:
                polymorphChance = 0.66;  // 66%概率
                break;
            case 3:
                polymorphChance = 1.0;   // 100%概率
                break;
            default:
                polymorphChance = 0.33;
        }
        
        // 搜索范围
        Box searchBox = new Box(
            cloudPos.x - radius, cloudPos.y - radius, cloudPos.z - radius,
            cloudPos.x + radius, cloudPos.y + radius, cloudPos.z + radius
        );
        
        // 获取范围内的所有敌对生物
        List<HostileEntity> hostiles = world.getEntitiesByClass(
            HostileEntity.class,
            searchBox,
            entity -> entity.isAlive() && !entity.isRemoved()
        );
        
        // 限制处理的怪物数量，防止性能问题
        int maxProcessed = Math.min(hostiles.size(), 10);  // 最多处理10个怪物
        
        // 只在服务端执行
        if (!(world instanceof net.minecraft.server.world.ServerWorld serverWorld)) {
            return;
        }
        
        // 对每个怪物尝试变形
        for (int i = 0; i < maxProcessed; i++) {
            HostileEntity hostile = hostiles.get(i);
            int entityId = hostile.getId();
            
            // 检查是否已经被变形过（直接使用this访问字段）
            if (this.polymorphedEntities.contains(entityId)) {
                continue;  // 已变形过，跳过
            }
            
            // 根据概率决定是否变形
            if (world.getRandom().nextDouble() < polymorphChance) {
                // 执行变形！
                polymorphHostileToPassive(serverWorld, hostile);
                // 标记已变形
                this.polymorphedEntities.add(entityId);
            }
        }
    }
    
    /**
     * 将敌对生物变形为被动生物
     * @param world 世界
     * @param hostile 敌对生物
     */
    @Unique
    private void polymorphHostileToPassive(net.minecraft.server.world.ServerWorld world, HostileEntity hostile) {
        // 保存原怪物的位置和信息
        double x = hostile.getX();
        double y = hostile.getY();
        double z = hostile.getZ();
        float yaw = hostile.getYaw();
        float pitch = hostile.getPitch();
        
        // 随机选择要变成的被动生物类型
        net.minecraft.entity.EntityType<?> passiveType;
        int choice = world.getRandom().nextInt(5);  // 5种被动生物
        
        switch (choice) {
            case 0:
                passiveType = net.minecraft.entity.EntityType.CHICKEN;  // 鸡
                break;
            case 1:
                passiveType = net.minecraft.entity.EntityType.RABBIT;   // 兔子
                break;
            case 2:
                passiveType = net.minecraft.entity.EntityType.PIG;      // 猪
                break;
            case 3:
                passiveType = net.minecraft.entity.EntityType.SHEEP;    // 羊
                break;
            case 4:
                passiveType = net.minecraft.entity.EntityType.COW;      // 牛
                break;
            default:
                passiveType = net.minecraft.entity.EntityType.CHICKEN;
        }
        
        // 创建新的被动生物实体（使用正确的create方法）
        Entity passiveEntity = passiveType.create(
            world,
            net.minecraft.entity.SpawnReason.MOB_SUMMONED
        );
        
        if (passiveEntity != null) {
            // 设置位置和朝向
            passiveEntity.refreshPositionAndAngles(x, y, z, yaw, pitch);
            
            // 如果原怪物有自定义名字，保留它
            if (hostile.hasCustomName()) {
                passiveEntity.setCustomName(hostile.getCustomName());
                passiveEntity.setCustomNameVisible(hostile.isCustomNameVisible());
            }
            
            // 生成被动生物
            world.spawnEntity(passiveEntity);
            
            // 移除原怪物
            hostile.discard();
            
            // 生成华丽的变形粒子效果
            spawnPolymorphParticles(world, x, y + hostile.getHeight() / 2, z);
            
            // 播放变形音效
            world.playSound(
                null,
                x, y, z,
                net.minecraft.sound.SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                net.minecraft.sound.SoundCategory.HOSTILE,
                1.0f,
                1.5f  // 高音调，更滑稽
            );
        }
    }
    
    /**
     * 生成变形粒子效果
     * @param world 世界
     * @param x X坐标
     * @param y Y坐标
     * @param z Z坐标
     */
    @Unique
    private void spawnPolymorphParticles(net.minecraft.server.world.ServerWorld world, double x, double y, double z) {
        // 不死图腾粒子（金色爆炸效果）
        world.spawnParticles(
            net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING,
            x, y, z,
            30,  // 大量粒子
            0.5, 0.5, 0.5,  // 扩散范围
            0.1  // 速度
        );
        
        // 爆炸粒子（白色闪光）
        world.spawnParticles(
            net.minecraft.particle.ParticleTypes.EXPLOSION,
            x, y, z,
            5,  // 粒子数量
            0.3, 0.3, 0.3,  // 扩散范围
            0.0  // 速度
        );
        
        // 快乐村民粒子（绿色爱心）
        world.spawnParticles(
            net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER,
            x, y, z,
            20,  // 粒子数量
            0.5, 0.5, 0.5,  // 扩散范围
            0.05  // 速度
        );
        
        // 末影人传送粒子（紫色烟雾）
        world.spawnParticles(
            net.minecraft.particle.ParticleTypes.PORTAL,
            x, y, z,
            50,  // 大量粒子
            0.5, 0.8, 0.5,  // 扩散范围
            0.2  // 速度
        );
    }
}
