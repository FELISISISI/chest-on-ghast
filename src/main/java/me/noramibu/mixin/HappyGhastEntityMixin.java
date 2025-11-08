package me.noramibu.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.EquipmentSlot;

/**
 * Mixin for HappyGhastEntity to add custom interaction behaviors
 * This mixin handles chest minecart placement on the ghast
 */
@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin {
    /**
     * Injects into the interactMob method to handle custom interactions
     * 
     * @param player The player interacting with the ghast
     * @param hand The hand used for interaction
     * @param cir Callback info for the return value
     */
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        ItemStack itemStack = player.getStackInHand(hand);
        
        // Check if player is using a chest minecart to add storage to the ghast
        if (itemStack.isOf(Items.CHEST_MINECART) && ghast.getPassengerList().size() < 3 && !ghast.getEquippedStack(EquipmentSlot.BODY).isEmpty()) {
            // Only execute on server side to prevent desync
            if (!ghast.getEntityWorld().isClient()) {
                // Create and spawn a chest minecart at the ghast's position
                ChestMinecartEntity chestMinecart = new ChestMinecartEntity(EntityType.CHEST_MINECART, ghast.getEntityWorld());
                chestMinecart.refreshPositionAndAngles(ghast.getX(), ghast.getY(), ghast.getZ(), ghast.getYaw(), ghast.getPitch());
                ghast.getEntityWorld().spawnEntity(chestMinecart);
                chestMinecart.startRiding(ghast);
                
                // Consume the chest minecart item if not in creative mode
                if (!player.getAbilities().creativeMode)
                    itemStack.decrement(1);
                
                // Set up team with no collision for the chest minecart to prevent clipping issues
                if (ghast.getEntityWorld() instanceof ServerWorld serverWorld) {
                    Scoreboard scoreboard = serverWorld.getScoreboard();
                    Team team = scoreboard.getTeam("NoCollision");
                    
                    // Create or update the NoCollision team
                    if (team == null) {
                        team = scoreboard.addTeam("NoCollision");
                        team.setCollisionRule(Team.CollisionRule.NEVER);
                    } else if (team.getCollisionRule() != Team.CollisionRule.NEVER) {
                        team.setCollisionRule(Team.CollisionRule.NEVER);
                    }
                    
                    // Add the chest minecart to the NoCollision team
                    String command = String.format("team join NoCollision %s", chestMinecart.getUuidAsString());
                    try {
                        serverWorld.getServer().getCommandManager().executeWithPrefix(
                            serverWorld.getServer().getCommandSource().withSilent(), command
                        );
                    } catch (Exception ignored) {
                        // Silently catch any command execution errors
                    }
                }
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}