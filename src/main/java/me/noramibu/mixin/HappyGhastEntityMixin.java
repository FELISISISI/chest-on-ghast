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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.entity.EquipmentSlot;

@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin {
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        ItemStack itemStack = player.getStackInHand(hand);
        if (itemStack.isOf(Items.CHEST_MINECART) && ghast.getPassengerList().size() < 3 && !ghast.getEquippedStack(EquipmentSlot.BODY).isEmpty()) {
            if (!ghast.getEntityWorld().isClient()) {
                ChestMinecartEntity chestMinecart = new ChestMinecartEntity(EntityType.CHEST_MINECART, ghast.getEntityWorld());
                chestMinecart.refreshPositionAndAngles(ghast.getX(), ghast.getY(), ghast.getZ(), ghast.getYaw(), ghast.getPitch());
                ghast.getEntityWorld().spawnEntity(chestMinecart);
                chestMinecart.startRiding(ghast);
                if (!player.getAbilities().creativeMode)
                    itemStack.decrement(1);
                if (ghast.getEntityWorld() instanceof ServerWorld serverWorld) {
                    Scoreboard scoreboard = serverWorld.getScoreboard();
                    Team team = scoreboard.getTeam("NoCollision");
                    if (team == null) {
                        team = scoreboard.addTeam("NoCollision");
                        team.setCollisionRule(Team.CollisionRule.NEVER);
                    } else if (team.getCollisionRule() != Team.CollisionRule.NEVER) {
                        team.setCollisionRule(Team.CollisionRule.NEVER);
                    }
                    String command = String.format("team join NoCollision %s", chestMinecart.getUuidAsString());
                    try {
                        serverWorld.getServer().getCommandManager().executeWithPrefix(
                            serverWorld.getServer().getCommandSource().withSilent(), command
                        );
                    } catch (Exception ignored) {}
                }
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}