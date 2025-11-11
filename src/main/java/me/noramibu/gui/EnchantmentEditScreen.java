package me.noramibu.gui;

import me.noramibu.enchantment.EnchantmentData;
import me.noramibu.item.EnchantedFireballBookItem;
import me.noramibu.network.UpdateEnchantmentPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 附魔编辑GUI
 * 显示3个附魔槽位和玩家物品栏
 * 玩家可以放置或移除附魔书
 */
public class EnchantmentEditScreen extends HandledScreen<EnchantmentEditScreen.EnchantmentEditScreenHandler> {
    private static final Identifier TEXTURE = Identifier.of("chest-on-ghast", "textures/gui/enchantment_edit.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;
    
    private final int ghastEntityId;
    
    public EnchantmentEditScreen(EnchantmentEditScreenHandler handler, PlayerInventory inventory, Text title, int ghastEntityId) {
        super(handler, inventory, title);
        this.ghastEntityId = ghastEntityId;
        this.backgroundHeight = TEXTURE_HEIGHT;
        this.backgroundWidth = TEXTURE_WIDTH;
    }
    
    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        
        // 添加完成按钮
        this.addDrawableChild(ButtonWidget.builder(
            Text.translatable("gui.chest-on-ghast.enchantment.done"),
            button -> this.close()
        ).dimensions(this.x + this.backgroundWidth / 2 - 40, this.y + this.backgroundHeight - 23, 80, 20).build());
    }
    
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // 绘制半透明背景
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.fill(x, y, x + this.backgroundWidth, y + this.backgroundHeight, 0xC0101010);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
    
    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 绘制标题
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        
        // 绘制物品栏标签
        context.drawText(this.textRenderer, this.playerInventoryTitle, 
            this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
        
        // 绘制槽位标签
        context.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.enchantment.slots"), 
            8, 20, 0x404040, false);
    }
    
    /**
     * 附魔编辑GUI的ScreenHandler
     */
    public static class EnchantmentEditScreenHandler extends ScreenHandler {
        private final PlayerInventory playerInventory;
        private final EnchantmentData enchantmentData;
        private final int ghastEntityId;
        
        // 3个附魔槽位
        private final EnchantmentSlot[] enchantmentSlots = new EnchantmentSlot[3];
        
        public EnchantmentEditScreenHandler(int syncId, PlayerInventory playerInventory, 
                                           EnchantmentData enchantmentData, int ghastEntityId) {
            super(ScreenHandlerType.GENERIC_3X3, syncId);
            this.playerInventory = playerInventory;
            this.enchantmentData = enchantmentData;
            this.ghastEntityId = ghastEntityId;
            
            // 添加3个附魔槽位（水平排列）
            for (int i = 0; i < 3; i++) {
                EnchantmentSlot slot = new EnchantmentSlot(i, 26 + i * 50, 35);
                this.enchantmentSlots[i] = slot;
                this.addSlot(slot);
                
                // 如果有现有附魔，加载到槽位
                EnchantmentData.EnchantmentSlot existingEnchant = enchantmentData.getEnchantment(i);
                if (existingEnchant != null) {
                    slot.setStack(existingEnchant.getBookStack().copy());
                }
            }
            
            // 添加玩家物品栏（主物品栏）
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 9; ++col) {
                    this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        8 + col * 18, 84 + row * 18));
                }
            }
            
            // 添加玩家快捷栏
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
            }
        }
        
        @Override
        public ItemStack quickMove(net.minecraft.entity.player.PlayerEntity player, int slot) {
            ItemStack newStack = ItemStack.EMPTY;
            Slot clickedSlot = this.slots.get(slot);
            
            if (clickedSlot.hasStack()) {
                ItemStack originalStack = clickedSlot.getStack();
                newStack = originalStack.copy();
                
                if (slot < 3) {
                    // 从附魔槽位移动到物品栏
                    if (!this.insertItem(originalStack, 3, this.slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    // 从物品栏移动到附魔槽位
                    if (originalStack.getItem() instanceof EnchantedFireballBookItem) {
                        // 尝试放入空的附魔槽位
                        boolean placed = false;
                        for (int i = 0; i < 3; i++) {
                            if (!this.enchantmentSlots[i].hasStack()) {
                                this.enchantmentSlots[i].setStack(originalStack.split(1));
                                placed = true;
                                break;
                            }
                        }
                        if (!placed) {
                            return ItemStack.EMPTY;
                        }
                    } else {
                        return ItemStack.EMPTY;
                    }
                }
                
                if (originalStack.isEmpty()) {
                    clickedSlot.setStack(ItemStack.EMPTY);
                } else {
                    clickedSlot.markDirty();
                }
            }
            
            return newStack;
        }
        
        @Override
        public boolean canUse(net.minecraft.entity.player.PlayerEntity player) {
            return true;
        }
        
        @Override
        public void onClosed(net.minecraft.entity.player.PlayerEntity player) {
            super.onClosed(player);
            
            // 保存附魔槽位的更改
            if (!player.getEntityWorld().isClient()) {
                return;
            }
            
            for (int i = 0; i < 3; i++) {
                ItemStack stack = this.enchantmentSlots[i].getStack();
                
                // 发送更新到服务端
                ClientPlayNetworking.send(new UpdateEnchantmentPayload(
                    this.ghastEntityId,
                    i,
                    stack.copy()
                ));
            }
        }
        
        /**
         * 附魔槽位类
         */
        private static class EnchantmentSlot extends Slot {
            private final int slotIndex;
            private ItemStack stack = ItemStack.EMPTY;
            
            public EnchantmentSlot(int slotIndex, int x, int y) {
                super(null, slotIndex, x, y);
                this.slotIndex = slotIndex;
            }
            
            @Override
            public boolean canInsert(ItemStack stack) {
                // 只接受快乐恶魂附魔书
                return stack.getItem() instanceof EnchantedFireballBookItem;
            }
            
            @Override
            public ItemStack getStack() {
                return this.stack;
            }
            
            @Override
            public void setStack(ItemStack stack) {
                this.stack = stack;
                this.markDirty();
            }
            
            @Override
            public int getMaxItemCount() {
                return 1; // 每个槽位只能放1本书
            }
            
            @Override
            public boolean canTakeItems(net.minecraft.entity.player.PlayerEntity player) {
                return true;
            }
        }
    }
}
