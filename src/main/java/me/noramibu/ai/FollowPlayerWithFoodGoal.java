package me.noramibu.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;

/**
 * 自定义AI Goal: 跟随手持食物的玩家
 */
public class FollowPlayerWithFoodGoal extends Goal {
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
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
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
