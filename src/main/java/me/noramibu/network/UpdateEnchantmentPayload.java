package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 更新附魔槽位的网络包
 * 客户端 -> 服务端
 */
public record UpdateEnchantmentPayload(
    int ghastEntityId,
    int slotIndex,
    ItemStack bookStack  // 附魔书物品栈（可能为空）
) implements CustomPayload {
    public static final CustomPayload.Id<UpdateEnchantmentPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "update_enchantment"));
    
    public static final PacketCodec<RegistryByteBuf, UpdateEnchantmentPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, UpdateEnchantmentPayload::ghastEntityId,
            PacketCodecs.VAR_INT, UpdateEnchantmentPayload::slotIndex,
            ItemStack.PACKET_CODEC, UpdateEnchantmentPayload::bookStack,
            UpdateEnchantmentPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
