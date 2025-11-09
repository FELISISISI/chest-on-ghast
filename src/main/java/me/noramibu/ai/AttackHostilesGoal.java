package me.noramibu.ai;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.level.LevelConfig;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * 自定义AI Goal: 攻击附近的敌对生物
 * 快乐恶魂会向附近的怪物发射火球来保护玩家
 * 火球威力和冷却时间随等级变化
 */
public class AttackHostilesGoal extends Goal {
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
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
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
