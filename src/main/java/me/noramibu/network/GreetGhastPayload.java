package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 自定义网络负载类，用于问候快乐恶魂的网络通信
 * 实现CustomPayload接口以符合Fabric API的新版网络系统
 */
public record GreetGhastPayload() implements CustomPayload {
    // 网络包标识符
    public static final CustomPayload.Id<GreetGhastPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "greet_ghast"));
    
    // 编解码器，用于序列化和反序列化网络包
    public static final PacketCodec<PacketByteBuf, GreetGhastPayload> CODEC = 
        PacketCodec.of(
            (value, buf) -> {}, // 编码器（无需传输数据）
            buf -> new GreetGhastPayload() // 解码器
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
