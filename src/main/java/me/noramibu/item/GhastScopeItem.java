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
     * 发射玩家控制的火球
     * @param ghast 快乐恶魂实体
     * @param player 玩家
     */
    private void shootPlayerControlledFireball(net.minecraft.entity.passive.HappyGhastEntity ghast, PlayerEntity player) {
        // 获取玩家视线方向
        net.minecraft.util.math.Vec3d lookVec = player.getRotationVec(1.0F);
        
        // 计算火球发射方向
        double dirX = lookVec.x;
        double dirY = lookVec.y;
        double dirZ = lookVec.z;
        
        // 获取快乐恶魂的等级和火球威力
        int fireballPower = 1; // 默认威力
        
        // 尝试获取快乐恶魂的等级数据
        if (ghast instanceof me.noramibu.accessor.HappyGhastDataAccessor accessor) {
            int level = accessor.getGhastData().getLevel();
            fireballPower = me.noramibu.level.LevelConfig.getFireballPower(level);
        }
        
        // 创建火球实体
        net.minecraft.entity.projectile.FireballEntity fireball = new net.minecraft.entity.projectile.FireballEntity(
            ghast.getEntityWorld(),
            ghast,
            new net.minecraft.util.math.Vec3d(dirX, dirY, dirZ),
            fireballPower
        );
        
        // 设置火球位置（从快乐恶魂中心稍微往前发射）
        fireball.setPosition(
            ghast.getX() + dirX * 2.0,
            ghast.getY() + ghast.getHeight() / 2.0,
            ghast.getZ() + dirZ * 2.0
        );
        
        // 生成火球
        ghast.getEntityWorld().spawnEntity(fireball);
        
        // 播放发射音效
        ghast.playSound(
            net.minecraft.sound.SoundEvents.ENTITY_GHAST_SHOOT,
            10.0f,
            (ghast.getRandom().nextFloat() - ghast.getRandom().nextFloat()) * 0.2f + 1.0f
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
