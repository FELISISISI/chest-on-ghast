package me.noramibu.enchantment;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

import java.util.HashMap;
import java.util.Map;

/**
 * 附魔数据类
 * 存储快乐恶魂的所有附魔信息
 */
public class EnchantmentData {
    // 最大附魔槽位数量
    public static final int MAX_ENCHANTMENT_SLOTS = 3;
    
    // 附魔槽位存储（槽位索引 -> 附魔类型和等级）
    private final Map<Integer, EnchantmentSlot> enchantmentSlots;
    
    /**
     * 构造函数
     */
    public EnchantmentData() {
        this.enchantmentSlots = new HashMap<>();
    }
    
    /**
     * 附魔槽位内部类
     */
    public static class EnchantmentSlot {
        private FireballEnchantment enchantment;
        private int level;
        private ItemStack bookStack; // 存储附魔书物品栈（用于显示）
        
        public EnchantmentSlot(FireballEnchantment enchantment, int level, ItemStack bookStack) {
            this.enchantment = enchantment;
            this.level = Math.min(level, enchantment.getMaxLevel());
            this.bookStack = bookStack.copy();
        }
        
        public FireballEnchantment getEnchantment() {
            return enchantment;
        }
        
        public int getLevel() {
            return level;
        }
        
        public ItemStack getBookStack() {
            return bookStack;
        }
    }
    
    /**
     * 设置指定槽位的附魔
     * @param slotIndex 槽位索引（0-2）
     * @param enchantment 附魔类型
     * @param level 附魔等级
     * @param bookStack 附魔书物品栈
     */
    public void setEnchantment(int slotIndex, FireballEnchantment enchantment, int level, ItemStack bookStack) {
        if (slotIndex < 0 || slotIndex >= MAX_ENCHANTMENT_SLOTS) {
            return;
        }
        
        if (enchantment == null) {
            enchantmentSlots.remove(slotIndex);
        } else {
            enchantmentSlots.put(slotIndex, new EnchantmentSlot(enchantment, level, bookStack));
        }
    }
    
    /**
     * 获取指定槽位的附魔
     */
    public EnchantmentSlot getEnchantment(int slotIndex) {
        return enchantmentSlots.get(slotIndex);
    }
    
    /**
     * 清除指定槽位
     */
    public void clearSlot(int slotIndex) {
        enchantmentSlots.remove(slotIndex);
    }
    
    /**
     * 清除所有附魔
     */
    public void clearAll() {
        enchantmentSlots.clear();
    }
    
    /**
     * 检查是否有指定附魔
     */
    public boolean hasEnchantment(FireballEnchantment enchantment) {
        for (EnchantmentSlot slot : enchantmentSlots.values()) {
            if (slot.getEnchantment() == enchantment) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取指定附魔的等级（如果没有返回0）
     */
    public int getEnchantmentLevel(FireballEnchantment enchantment) {
        for (EnchantmentSlot slot : enchantmentSlots.values()) {
            if (slot.getEnchantment() == enchantment) {
                return slot.getLevel();
            }
        }
        return 0;
    }
    
    /**
     * 获取所有激活的附魔
     */
    public Map<Integer, EnchantmentSlot> getAllEnchantments() {
        return new HashMap<>(enchantmentSlots);
    }
    
    /**
     * 保存到NBT
     */
    public NbtCompound writeToNbt() {
        NbtCompound nbt = new NbtCompound();
        NbtList enchantmentList = new NbtList();
        
        for (Map.Entry<Integer, EnchantmentSlot> entry : enchantmentSlots.entrySet()) {
            NbtCompound slotNbt = new NbtCompound();
            slotNbt.putInt("Slot", entry.getKey());
            slotNbt.putString("Enchantment", entry.getValue().getEnchantment().getId());
            slotNbt.putInt("Level", entry.getValue().getLevel());
            
            // 保存物品栈（简化存储，只存储附魔信息即可）
            // 不保存完整物品栈，节省存储空间
            
            enchantmentList.add(slotNbt);
        }
        
        nbt.put("Enchantments", enchantmentList);
        return nbt;
    }
    
    /**
     * 从NBT读取
     */
    public void readFromNbt(NbtCompound nbt) {
        enchantmentSlots.clear();
        
        if (nbt.contains("Enchantments")) {
            NbtList enchantmentList = nbt.getList("Enchantments").orElse(new NbtList());
            
            for (int i = 0; i < enchantmentList.size(); i++) {
                NbtCompound slotNbt = enchantmentList.getCompound(i).orElse(null);
                if (slotNbt == null) continue;
                
                int slot = slotNbt.getInt("Slot").orElse(0);
                String enchantmentId = slotNbt.getString("Enchantment").orElse("");
                int level = slotNbt.getInt("Level").orElse(1);
                
                FireballEnchantment enchantment = FireballEnchantment.fromId(enchantmentId);
                if (enchantment != null) {
                    // 重新创建附魔书物品栈
                    ItemStack bookStack = me.noramibu.item.EnchantedFireballBookItem.create(enchantment, level);
                    enchantmentSlots.put(slot, new EnchantmentSlot(enchantment, level, bookStack));
                }
            }
        }
    }
}
