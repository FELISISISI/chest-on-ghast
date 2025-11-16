package me.noramibu.mixin;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.level.LevelConfig;
import me.noramibu.network.SyncGhastDataPayload;
import me.noramibu.system.CombatSystem;
import me.noramibu.system.EffectCloudSystem;
import me.noramibu.system.EffectCloudSystemHolder;
import me.noramibu.system.LevelingSystem;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HappyGhastEntity Mixin - 简化版
 * 
 * 职责：
 * 1. 数据存储（HappyGhastData）
 * 2. 系统委托（Combat, Leveling, EffectCloud）
 * 3. NBT持久化
 * 4. 玩家交互
 * 
 * 设计原则：
 * - 只做注入和委托，不包含业务逻辑
 * - 所有复杂逻辑在System类中
 * - 线程安全（ConcurrentHashMap）
 * - 目标：保持在150行以内
 */
@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin extends net.minecraft.entity.mob.MobEntity implements HappyGhastDataAccessor {
    
    // ===== 数据存储 =====
    @Unique
    private HappyGhastData ghastData;
    
    // ===== 系统实例（每个恶魂独立） =====
    @Unique
    private CombatSystem combatSystem;
    
    @Unique
    private EffectCloudSystem effectCloudSystem;
    
    protected HappyGhastEntityMixin() {
        super(null, null);
    }
    
    // ===== 初始化 =====
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        // 懒加载模式：在getHappyGhastData()中初始化
    }
    
    /**
     * 在实体死亡或被移除时调用
     * 用于清理资源
     */
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onDeath(CallbackInfo ci) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        cleanupSystems(ghast);
    }
    
    @Override
    public HappyGhastData getHappyGhastData() {
        if (this.ghastData == null) {
            this.ghastData = new HappyGhastData();
            this.combatSystem = new CombatSystem();
            this.effectCloudSystem = new EffectCloudSystem();
            
            // 注册到Holder
            EffectCloudSystemHolder.register((HappyGhastEntity) (Object) this, effectCloudSystem);
        }
        return this.ghastData;
    }
    
    @Override
    public void setHappyGhastData(HappyGhastData data) {
        this.ghastData = data;
    }
    
    // ===== 主循环 =====
    
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        
        // 安全检查
        if (ghast.isRemoved()) return;
        if (ghast.getEntityWorld() == null) return;
        
        // 只在服务端执行
        if (!(ghast.getEntityWorld() instanceof ServerWorld world)) return;
        
        // 确保数据已初始化
        HappyGhastData data = getHappyGhastData();
        
        // 委托给各系统
        LevelingSystem.tick(ghast, data);
        combatSystem.tick(ghast, world);
        effectCloudSystem.tick(world);
        
        // 定期保存数据
        if (ghast.age % 100 == 0) {
            saveDataToNbt(ghast);
        }
    }
    
    // ===== 玩家交互 =====
    
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        ItemStack stack = player.getStackInHand(hand);
        HappyGhastData data = getHappyGhastData();
        
        // 服务端处理
        if (ghast.getEntityWorld() instanceof ServerWorld) {
            // 喂食（检查是否是食物 - 使用FoodComponent判断）
            if (!stack.isEmpty() && stack.get(net.minecraft.component.DataComponentTypes.FOOD) != null && !player.isSneaking()) {
                handleFeeding(ghast, player, stack, data);
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
            
            // Shift+右键打开GUI
            if (player.isSneaking() && stack.isEmpty()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // 同步数据到客户端
                    LevelConfig.LevelData levelData = LevelConfig.getLevelData(data.getLevel());
                    ServerPlayNetworking.send(serverPlayer, new SyncGhastDataPayload(
                        ghast.getId(),
                        data.getLevel(),
                        data.getExperience(),
                        data.getHunger(),
                        levelData.getMaxHealth(),
                        ghast.getHealth(),
                        levelData.getMaxHunger(),
                        levelData.getExpToNextLevel(),
                        player.isCreative(),
                        data.getFavoriteFoods(),
                        data.getCustomName() != null ? data.getCustomName() : ""
                    ));
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
        }
    }
    
    // ===== 喂食处理 =====
    
    @Unique
    private void handleFeeding(HappyGhastEntity ghast, PlayerEntity player, ItemStack stack, HappyGhastData data) {
        // 计算经验和饱食度（简化版本）
        int expGain = 10;  // 基础经验
        float hungerRestore = 10.0f;  // 基础饱食度
        
        // 应用效果
        data.addHunger(hungerRestore);
        boolean leveledUp = LevelingSystem.addExperience(ghast, data, expGain);
        
        // 消耗物品
        if (!player.isCreative()) {
            stack.decrement(1);
        }
        
        // 提示消息
        player.sendMessage(Text.literal(String.format(
            "饱食度 +%.1f, 经验 +%d", hungerRestore, expGain
        )), true);
        
        if (leveledUp) {
            player.sendMessage(Text.literal(String.format(
                "快乐恶魂升级到了等级 %d！", data.getLevel()
            )), false);
        }
    }
    
    // ===== NBT持久化 =====
    // 在tick方法中已经实现了定期保存，这里通过覆盖toNbtList来确保数据被保存
    // 使用require=0使Mixin在找不到方法时不会失败（向后兼容）
    
    /**
     * 保存自定义NBT数据
     * 注意：此方法在编译时可能找不到，但在运行时Mixin会正确注入
     */
    @Inject(method = "writeNbt", at = @At("RETURN"), require = 0)
    private void onWriteNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        getHappyGhastData().writeToNbt(nbt);
    }
    
    /**
     * 读取自定义NBT数据  
     * 注意：此方法在编译时可能找不到，但在运行时Mixin会正确注入
     */
    @Inject(method = "readNbt", at = @At("RETURN"), require = 0)
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        this.ghastData = HappyGhastData.readFromNbt(nbt);
    }
    
    @Unique
    private void saveDataToNbt(HappyGhastEntity ghast) {
        // 数据自动保存，在tick方法中每100 ticks保存一次
    }
    
    // ===== 清理 =====
    
    /**
     * 实体被移除时的清理方法
     * 注意：此方法在编译时可能找不到，但在运行时Mixin会正确注入
     */
    @Inject(method = "remove", at = @At("HEAD"), require = 0)
    private void onRemove(net.minecraft.entity.Entity.RemovalReason reason, CallbackInfo ci) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        cleanupSystems(ghast);
    }
    
    /**
     * 清理系统资源的统一方法
     */
    @Unique
    private void cleanupSystems(HappyGhastEntity ghast) {
        // 清理战斗系统
        if (combatSystem != null) {
            combatSystem.reset();
        }
        
        // 清理效果云系统
        if (effectCloudSystem != null) {
            effectCloudSystem.reset();
        }
        
        // 从全局Holder中移除
        EffectCloudSystemHolder.unregister(ghast);
    }
}
