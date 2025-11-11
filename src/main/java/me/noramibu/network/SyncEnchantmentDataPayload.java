package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 同步附魔数据的网络包
 * 服务端 -> 客户端
 */
public record SyncEnchantmentDataPayload(
    int ghastEntityId,
    NbtCompound enchantmentData
) implements CustomPayload {
    public static final CustomPayload.Id<SyncEnchantmentDataPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "sync_enchantment_data"));
    
    public static final PacketCodec<RegistryByteBuf, SyncEnchantmentDataPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, SyncEnchantmentDataPayload::ghastEntityId,
            PacketCodecs.NBT_COMPOUND, SyncEnchantmentDataPayload::enchantmentData,
            SyncEnchantmentDataPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
