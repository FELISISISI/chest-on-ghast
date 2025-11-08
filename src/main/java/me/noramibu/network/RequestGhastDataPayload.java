package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 客户端请求快乐恶魂数据的网络包（C2S）
 */
public record RequestGhastDataPayload(int entityId) implements CustomPayload {
    
    public static final CustomPayload.Id<RequestGhastDataPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "request_ghast_data"));
    
    public static final PacketCodec<PacketByteBuf, RequestGhastDataPayload> CODEC = 
        PacketCodec.of(
            (value, buf) -> buf.writeInt(value.entityId),
            buf -> new RequestGhastDataPayload(buf.readInt())
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
