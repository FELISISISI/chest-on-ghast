package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record UpdateGhastConfigPayload(
    List<SyncGhastConfigPayload.LevelEntry> levels,
    List<SyncGhastConfigPayload.ElementEntry> elements,
    boolean debugMode
) implements CustomPayload {
    public static final CustomPayload.Id<UpdateGhastConfigPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "update_ghast_config"));

    public static final PacketCodec<PacketByteBuf, UpdateGhastConfigPayload> CODEC =
        PacketCodec.of(UpdateGhastConfigPayload::write, UpdateGhastConfigPayload::read);

    private static void write(UpdateGhastConfigPayload value, PacketByteBuf buf) {
        buf.writeInt(value.levels.size());
        for (SyncGhastConfigPayload.LevelEntry level : value.levels) {
            buf.writeInt(level.level());
            buf.writeInt(level.fireballPower());
            buf.writeInt(level.attackCooldownTicks());
            buf.writeFloat(level.fireballDamage());
        }

        buf.writeInt(value.elements.size());
        for (SyncGhastConfigPayload.ElementEntry element : value.elements) {
            buf.writeString(element.id());
            buf.writeFloat(element.sameBiomeDamageBonus());
            buf.writeFloat(element.sameBiomeEffectBonus());
        }

        buf.writeBoolean(value.debugMode);
    }

    private static UpdateGhastConfigPayload read(PacketByteBuf buf) {
        int levelCount = buf.readInt();
        List<SyncGhastConfigPayload.LevelEntry> levels = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            int level = buf.readInt();
            int power = buf.readInt();
            int cooldown = buf.readInt();
            float damage = buf.readFloat();
            levels.add(new SyncGhastConfigPayload.LevelEntry(level, power, cooldown, damage));
        }

        int elementCount = buf.readInt();
        List<SyncGhastConfigPayload.ElementEntry> elements = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {
            String id = buf.readString();
            float dmgBonus = buf.readFloat();
            float effectBonus = buf.readFloat();
            elements.add(new SyncGhastConfigPayload.ElementEntry(id, dmgBonus, effectBonus));
        }

        boolean debugMode = buf.readBoolean();
        return new UpdateGhastConfigPayload(levels, elements, debugMode);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
