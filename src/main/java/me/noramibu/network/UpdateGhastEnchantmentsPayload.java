package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * 客户端→服务端：更新快乐恶魂附魔槽位的请求
 */
public record UpdateGhastEnchantmentsPayload(
	int entityId,
	int slot,
	String enchantmentId,
	int level
) implements CustomPayload {

	public static final CustomPayload.Id<UpdateGhastEnchantmentsPayload> ID =
		new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "update_ghast_enchantments"));

	public static final PacketCodec<PacketByteBuf, UpdateGhastEnchantmentsPayload> CODEC =
		PacketCodec.of(
			(value, buf) -> {
				buf.writeInt(value.entityId);
				buf.writeInt(value.slot);
				buf.writeString(value.enchantmentId != null ? value.enchantmentId : "");
				buf.writeInt(value.level);
			},
			buf -> new UpdateGhastEnchantmentsPayload(
				buf.readInt(),
				buf.readInt(),
				buf.readString(),
				buf.readInt()
			)
		);

	@Override
	public CustomPayload.Id<? extends CustomPayload> getId() {
		return ID;
	}
}
