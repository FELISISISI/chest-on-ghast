package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public record SyncGhastConfigPayload(
    List<LevelEntry> levels,
    List<ElementEntry> elements,
    boolean debugMode
) implements CustomPayload {

    public static final CustomPayload.Id<SyncGhastConfigPayload> ID =
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "sync_ghast_config"));

    public static final PacketCodec<PacketByteBuf, SyncGhastConfigPayload> CODEC =
        PacketCodec.of(SyncGhastConfigPayload::write, SyncGhastConfigPayload::read);

    private static void write(SyncGhastConfigPayload value, PacketByteBuf buf) {
        buf.writeInt(value.levels.size());
        for (LevelEntry level : value.levels) {
            buf.writeInt(level.level());
            buf.writeInt(level.fireballPower());
            buf.writeInt(level.attackCooldownTicks());
            buf.writeFloat(level.fireballDamage());
        }

        buf.writeInt(value.elements.size());
        for (ElementEntry element : value.elements) {
            buf.writeString(element.id());
            buf.writeFloat(element.sameBiomeDamageBonus());
            buf.writeFloat(element.sameBiomeEffectBonus());
        }

        buf.writeBoolean(value.debugMode);
    }

    private static SyncGhastConfigPayload read(PacketByteBuf buf) {
        int levelCount = buf.readInt();
        List<LevelEntry> levels = new ArrayList<>();
        for (int i = 0; i < levelCount; i++) {
            int level = buf.readInt();
            int power = buf.readInt();
            int cooldown = buf.readInt();
            float damage = buf.readFloat();
            levels.add(new LevelEntry(level, power, cooldown, damage));
        }

        int elementCount = buf.readInt();
        List<ElementEntry> elements = new ArrayList<>();
        for (int i = 0; i < elementCount; i++) {
            String id = buf.readString();
            float dmgBonus = buf.readFloat();
            float effectBonus = buf.readFloat();
            elements.add(new ElementEntry(id, dmgBonus, effectBonus));
        }

        boolean debugMode = buf.readBoolean();
        return new SyncGhastConfigPayload(levels, elements, debugMode);
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public record LevelEntry(int level, int fireballPower, int attackCooldownTicks, float fireballDamage) {}

    public record ElementEntry(String id, float sameBiomeDamageBonus, float sameBiomeEffectBonus) {}
}
