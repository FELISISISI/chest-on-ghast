package me.noramibu.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HappyGhastEntity.class)
public abstract class HappyGhastEntityMixin {
    @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
    private void interactMob(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        HappyGhastEntity ghast = (HappyGhastEntity) (Object) this;
        ItemStack itemStack = player.getStackInHand(hand);
        // Only spawn if the player is holding a CHEST_MINECART
        if (itemStack.isOf(Items.CHEST_MINECART) && ghast.getPassengerList().size() < 3 && !ghast.getEquippedStack(EquipmentSlot.BODY).isEmpty()) {
            if (!ghast.getWorld().isClient) {
                ChestMinecartEntity chestMinecart = new ChestMinecartEntity(EntityType.CHEST_MINECART, ghast.getWorld());
                chestMinecart.refreshPositionAndAngles(ghast.getX(), ghast.getY(), ghast.getZ(), ghast.getYaw(), ghast.getPitch());
                ghast.getWorld().spawnEntity(chestMinecart);
                chestMinecart.startRiding(ghast);
                if (!player.getAbilities().creativeMode)
                    itemStack.decrement(1);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }
}