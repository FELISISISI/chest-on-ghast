package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestGhastConfigPayload() implements CustomPayload {
    public static final CustomPayload.Id<RequestGhastConfigPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "request_ghast_config"));

    public static final PacketCodec<PacketByteBuf, RequestGhastConfigPayload> CODEC =
        PacketCodec.of((value, buf) -> {}, buf -> new RequestGhastConfigPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
