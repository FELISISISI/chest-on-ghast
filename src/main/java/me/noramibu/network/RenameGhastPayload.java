package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 客户端发送给服务端的改名网络包（C2S）
 */
public record RenameGhastPayload(int entityId, String newName) implements CustomPayload {
    
    public static final CustomPayload.Id<RenameGhastPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "rename_ghast"));
    
    public static final PacketCodec<PacketByteBuf, RenameGhastPayload> CODEC = 
        PacketCodec.of(
            (value, buf) -> {
                buf.writeInt(value.entityId);
                buf.writeString(value.newName);
            },
            buf -> new RenameGhastPayload(buf.readInt(), buf.readString())
        );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
