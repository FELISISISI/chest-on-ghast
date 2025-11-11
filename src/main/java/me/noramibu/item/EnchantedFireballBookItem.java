package me.noramibu.item;

import me.noramibu.enchantment.FireballEnchantment;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 快乐恶魂专用附魔书
 * 只有这种附魔书才能添加到快乐恶魂的附魔槽位
 */
public class EnchantedFireballBookItem extends Item {
    
    public EnchantedFireballBookItem(Settings settings) {
        super(settings);
    }
    
    /**
     * 创建指定附魔和等级的附魔书
     */
    public static ItemStack create(FireballEnchantment enchantment, int level) {
        ItemStack stack = new ItemStack(ModItems.ENCHANTED_FIREBALL_BOOK);
        
        // 限制等级范围
        level = Math.max(1, Math.min(level, enchantment.getMaxLevel()));
        
        // 存储附魔信息到组件数据
        stack.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.ComponentMap.builder()
            .add(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(new NbtCompound()))
            .build().get(DataComponentTypes.CUSTOM_DATA));
        
        NbtCompound nbt = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, 
            net.minecraft.component.type.NbtComponent.DEFAULT).copyNbt();
        nbt.putString("FireballEnchantment", enchantment.getId());
        nbt.putInt("EnchantmentLevel", level);
        stack.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(nbt));
        
        // 设置自定义名称（带颜色）
        stack.set(DataComponentTypes.CUSTOM_NAME, 
            Text.translatable(enchantment.getTranslationKey())
                .append(" ")
                .append(getRomanNumeral(level))
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD));
        
        // 设置Lore（描述文本）
        List<Text> lore = List.of(
            Text.translatable(enchantment.getDescriptionKey()).formatted(Formatting.GRAY),
            Text.literal(""),
            Text.translatable("item.chest-on-ghast.fireball_book.usage").formatted(Formatting.DARK_GRAY, Formatting.ITALIC)
        );
        stack.set(DataComponentTypes.LORE, new LoreComponent(lore));
        
        // 添加附魔光效
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        
        return stack;
    }
    
    /**
     * 从物品栈获取附魔类型
     */
    public static FireballEnchantment getEnchantment(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EnchantedFireballBookItem)) {
            return null;
        }
        
        net.minecraft.component.type.NbtComponent customData = stack.getOrDefault(
            DataComponentTypes.CUSTOM_DATA, 
            net.minecraft.component.type.NbtComponent.DEFAULT
        );
        NbtCompound nbt = customData.copyNbt();
        
        if (nbt.contains("FireballEnchantment")) {
            String enchantmentId = nbt.getString("FireballEnchantment").orElse("");
            return FireballEnchantment.fromId(enchantmentId);
        }
        
        return null;
    }
    
    /**
     * 从物品栈获取附魔等级
     */
    public static int getEnchantmentLevel(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EnchantedFireballBookItem)) {
            return 0;
        }
        
        net.minecraft.component.type.NbtComponent customData = stack.getOrDefault(
            DataComponentTypes.CUSTOM_DATA, 
            net.minecraft.component.type.NbtComponent.DEFAULT
        );
        NbtCompound nbt = customData.copyNbt();
        
        if (nbt.contains("EnchantmentLevel")) {
            return nbt.getInt("EnchantmentLevel").orElse(1);
        }
        
        return 1; // 默认等级1
    }
    
    // Tooltip将通过Lore组件显示，不需要重写appendTooltip方法
    
    /**
     * 转换数字为罗马数字
     */
    private static String getRomanNumeral(int number) {
        switch (number) {
            case 1: return "I";
            case 2: return "II";
            case 3: return "III";
            case 4: return "IV";
            case 5: return "V";
            default: return String.valueOf(number);
        }
    }
}
