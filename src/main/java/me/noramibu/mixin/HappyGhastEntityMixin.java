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
            // 喂食（检查是否是食物）
            if (!stack.isEmpty() && stack.getItem().isFood() && !player.isSneaking()) {
                handleFeeding(ghast, player, stack, data);
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
            
            // Shift+右键打开GUI
            if (player.isSneaking() && stack.isEmpty()) {
                if (player instanceof ServerPlayerEntity serverPlayer) {
                    // 同步数据到客户端
                    ServerPlayNetworking.send(serverPlayer, new SyncGhastDataPayload(
                        ghast.getId(),
                        data.getLevel(),
                        data.getExperience(),
                        data.getMaxHunger(),
                        data.getHunger(),
                        data.getCustomName()
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
    
    @Inject(method = "writeNbt", at = @At("TAIL"))
    private void onWriteNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        getHappyGhastData().writeToNbt(nbt);
    }
    
    @Inject(method = "readNbt", at = @At("TAIL"))
    private void onReadNbt(NbtCompound nbt, CallbackInfo ci) {
        this.ghastData = HappyGhastData.readFromNbt(nbt);
    }
    
    @Unique
    private void saveDataToNbt(HappyGhastEntity ghast) {
        // 数据自动保存，这里留空即可
    }
    
    // ===== 清理 =====
    
    @Inject(method = "onRemoved", at = @At("HEAD"))
    private void onRemove(CallbackInfo ci) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        
        // 清理系统
        if (combatSystem != null) {
            combatSystem.reset();
        }
        if (effectCloudSystem != null) {
            effectCloudSystem.reset();
        }
        
        // 从Holder中移除
        EffectCloudSystemHolder.unregister(ghast);
    }
}
