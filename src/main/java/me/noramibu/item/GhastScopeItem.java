package me.noramibu.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/**
 * 快乐恶魂瞄准镜
 * 当玩家骑在快乐恶魂上时，可以使用此物品瞄准并发射火球
 */
public class GhastScopeItem extends Item {
    // 最大使用时间（持续按住右键的时间）
    private static final int MAX_USE_TIME = 72000; // 1小时，实际上是无限
    
    /**
     * 构造函数
     * @param settings 物品设置
     */
    public GhastScopeItem(Settings settings) {
        super(settings);
    }
    
    /**
     * 右键使用物品
     * 开始瞄准模式
     */
    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // 检查玩家是否正在骑乘快乐恶魂
        if (user.hasVehicle() && user.getVehicle() instanceof net.minecraft.entity.passive.HappyGhastEntity) {
            // 开始使用瞄准镜（进入瞄准模式）
            user.setCurrentHand(hand);
            return ActionResult.CONSUME;
        }
        
        // 如果没有骑乘快乐恶魂，返回失败
        return ActionResult.FAIL;
    }
    
    /**
     * 停止使用物品（松开右键）
     * 发射火球
     */
    @Override
    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (user instanceof PlayerEntity player && !world.isClient()) {
            // 检查玩家是否还在骑乘快乐恶魂
            if (player.hasVehicle() && player.getVehicle() instanceof net.minecraft.entity.passive.HappyGhastEntity ghast) {
                // 计算使用时间（按住右键的时间）
                int useTime = this.getMaxUseTime(stack, user) - remainingUseTicks;
                
                // 至少需要按住10 ticks（0.5秒）才能发射
                if (useTime >= 10) {
                    // 触发火球发射
                    shootPlayerControlledFireball(ghast, player);
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 发射玩家控制的火球（支持附魔效果）
     * @param ghast 快乐恶魂实体
     * @param player 玩家
     */
    private void shootPlayerControlledFireball(net.minecraft.entity.passive.HappyGhastEntity ghast, PlayerEntity player) {
        // 获取玩家视线方向
        net.minecraft.util.math.Vec3d direction = player.getRotationVec(1.0F);
        
        // 获取快乐恶魂的等级和火球威力
        int fireballPower = 1; // 默认威力
        
        // 尝试获取快乐恶魂的等级数据
        if (ghast instanceof me.noramibu.accessor.HappyGhastDataAccessor accessor) {
            int level = accessor.getHappyGhastData().getLevel();
            fireballPower = me.noramibu.level.LevelConfig.getFireballPower(level);
        }
        
        // 使用附魔辅助类发射火球（支持连射等附魔）
        me.noramibu.enchantment.EnchantmentHelper.shootFireballWithEnchantments(
            ghast, 
            direction, 
            fireballPower, 
            null  // 玩家控制的火球没有特定目标
        );
        
        // 给玩家反馈消息
        player.sendMessage(
            net.minecraft.text.Text.translatable("message.chest-on-ghast.fireball_shot"),
            true
        );
    }
    
    /**
     * 获取最大使用时间
     */
    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return MAX_USE_TIME;
    }
}
