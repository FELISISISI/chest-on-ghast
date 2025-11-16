package me.noramibu.network;

import me.noramibu.Chestonghast;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 同步快乐恶魂数据的网络包
 * 服务端发送到客户端，用于同步恶魂的数据并打开GUI
 */
public record SyncGhastDataPayload(
    int entityId,          // 实体ID
    int level,             // 等级
    int experience,        // 经验值
    float hunger,          // 饱食度
    float maxHealth,       // 最大血量
    float currentHealth,   // 当前血量
    float maxHunger,       // 最大饱食度
    int expToNext,         // 升级所需经验
    boolean isCreative,    // 玩家是否为创造模式
    List<String> favoriteFoods,  // 最喜欢的食物（创造模式下显示）
    String customName      // 自定义名字
) implements CustomPayload {
    
    // 网络包标识符
    public static final CustomPayload.Id<SyncGhastDataPayload> ID = 
        new CustomPayload.Id<>(Identifier.of(Chestonghast.MOD_ID, "sync_ghast_data"));
    
    // 编解码器，用于序列化和反序列化网络包
    public static final PacketCodec<PacketByteBuf, SyncGhastDataPayload> CODEC = 
        PacketCodec.of(
            (value, buf) -> {
                // 编码器：按顺序写入所有数据
                buf.writeInt(value.entityId);
                buf.writeInt(value.level);
                buf.writeInt(value.experience);
                buf.writeFloat(value.hunger);
                buf.writeFloat(value.maxHealth);
                buf.writeFloat(value.currentHealth);
                buf.writeFloat(value.maxHunger);
                buf.writeInt(value.expToNext);
                buf.writeBoolean(value.isCreative);
                
                // 写入最喜欢的食物列表
                buf.writeInt(value.favoriteFoods.size());
                for (String food : value.favoriteFoods) {
                    buf.writeString(food);
                }
                
                // 写入自定义名字
                buf.writeString(value.customName != null ? value.customName : "");
            },
            buf -> {
                // 解码器：按顺序读取所有数据
                int entityId = buf.readInt();
                int level = buf.readInt();
                int experience = buf.readInt();
                float hunger = buf.readFloat();
                float maxHealth = buf.readFloat();
                float currentHealth = buf.readFloat();
                float maxHunger = buf.readFloat();
                int expToNext = buf.readInt();
                boolean isCreative = buf.readBoolean();
                
                // 读取最喜欢的食物列表
                int foodCount = buf.readInt();
                List<String> favoriteFoods = new ArrayList<>();
                for (int i = 0; i < foodCount; i++) {
                    favoriteFoods.add(buf.readString());
                }
                
                // 读取自定义名字
                String customName = buf.readString();
                
                return new SyncGhastDataPayload(
                    entityId, level, experience, hunger,
                    maxHealth, currentHealth, maxHunger, expToNext,
                    isCreative, favoriteFoods, customName
                );
            }
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
