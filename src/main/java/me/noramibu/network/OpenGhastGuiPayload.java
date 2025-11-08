package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 打开快乐恶魂GUI的网络包
 * 客户端发送到服务端，请求打开GUI
 */
public record OpenGhastGuiPayload(int entityId) implements CustomPayload {
    // 网络包标识符
    public static final CustomPayload.Id<OpenGhastGuiPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "open_ghast_gui"));
    
    // 编解码器，用于序列化和反序列化网络包
    public static final PacketCodec<PacketByteBuf, OpenGhastGuiPayload> CODEC = 
        PacketCodec.of(
            (value, buf) -> buf.writeInt(value.entityId), // 编码器：写入实体ID
            buf -> new OpenGhastGuiPayload(buf.readInt())  // 解码器：读取实体ID
        );

    /**
     * 获取网络包ID
     * @return 网络包的唯一标识符
     */
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
