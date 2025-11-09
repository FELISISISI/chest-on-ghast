package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 打开附魔编辑GUI的网络包
 * 客户端 -> 服务端
 */
public record OpenEnchantmentGuiPayload(int ghastEntityId) implements CustomPayload {
    public static final CustomPayload.Id<OpenEnchantmentGuiPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "open_enchantment_gui"));
    
    public static final PacketCodec<RegistryByteBuf, OpenEnchantmentGuiPayload> CODEC = 
        PacketCodec.tuple(
            PacketCodecs.VAR_INT, OpenEnchantmentGuiPayload::ghastEntityId,
            OpenEnchantmentGuiPayload::new
        );
    
    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
