package me.noramibu.item;

import me.noramibu.Chestonghast;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * 模组物品注册类
 * 负责注册所有自定义物品
 */
public class ModItems {
    // 快乐恶魂瞄准镜
    public static final Item GHAST_SCOPE = registerItem("ghast_scope",
        new GhastScopeItem(new Item.Settings().maxCount(1).registryKey(
            net.minecraft.registry.RegistryKey.of(
                net.minecraft.registry.RegistryKeys.ITEM,
                Identifier.of(Chestonghast.MOD_ID, "ghast_scope")
            )
        )));
    
    // 快乐恶魂专用附魔书
    public static final Item ENCHANTED_FIREBALL_BOOK = registerItem("enchanted_fireball_book",
        new EnchantedFireballBookItem(new Item.Settings().maxCount(1).registryKey(
            net.minecraft.registry.RegistryKey.of(
                net.minecraft.registry.RegistryKeys.ITEM,
                Identifier.of(Chestonghast.MOD_ID, "enchanted_fireball_book")
            )
        )));
    
    /**
     * 注册物品
     * @param name 物品ID
     * @param item 物品实例
     * @return 注册后的物品
     */
    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Chestonghast.MOD_ID, name), item);
    }
    
    /**
     * 初始化物品注册
     * 在模组主类中调用
     */
    public static void registerModItems() {
        Chestonghast.LOGGER.info("注册模组物品：{}", Chestonghast.MOD_ID);
    }
}
