package me.noramibu.gui;

import me.noramibu.Chestonghast;
import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 快乐恶魂GUI屏幕 - 多方法测试版本
 */
public class HappyGhastScreen extends Screen {
    private final int level;
    private final int experience;
    private final float hunger;
    private final float maxHealth;
    private final float currentHealth;
    private final float maxHunger;
    private final int expToNext;
    
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private int guiX;
    private int guiY;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.literal("Happy Ghast"));
        this.level = payload.level();
        this.experience = payload.experience();
        this.hunger = payload.hunger();
        this.maxHealth = payload.maxHealth();
        this.currentHealth = payload.currentHealth();
        this.maxHunger = payload.maxHunger();
        this.expToNext = payload.expToNext();
        
        Chestonghast.LOGGER.info("=== GUI Created ===");
    }
    
    @Override
    protected void init() {
        super.init();
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        Chestonghast.LOGGER.info("TextRenderer null? {}", this.textRenderer == null);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 白色背景便于观察
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        
        // 彩色边框
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + 3, 0xFFFF0000);
        context.fill(guiX, guiY + GUI_HEIGHT - 3, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFF0000FF);
        
        // 参照方块
        context.fill(guiX + 150, guiY + 10, guiX + 160, guiY + 20, 0xFFFF0000);
        context.fill(guiX + 150, guiY + 25, guiX + 160, guiY + 35, 0xFF00FF00);
        context.fill(guiX + 150, guiY + 40, guiX + 160, guiY + 50, 0xFF0000FF);
        
        super.render(context, mouseX, mouseY, delta);
        
        if (this.textRenderer == null) {
            Chestonghast.LOGGER.error("TextRenderer is NULL!");
            return;
        }
        
        int x = guiX + 10;
        int y = guiY + 10;
        
        // 方法1: String黑色
        Chestonghast.LOGGER.info("M1: String black");
        context.drawText(this.textRenderer, "M1-String-Black", x, y, 0xFF000000, false);
        y += 15;
        
        // 方法2: Text对象黑色
        Chestonghast.LOGGER.info("M2: Text black");
        context.drawText(this.textRenderer, Text.literal("M2-Text-Black"), x, y, 0xFF000000, false);
        y += 15;
        
        // 方法3: 白色带阴影
        Chestonghast.LOGGER.info("M3: White shadow");
        context.drawText(this.textRenderer, "M3-White-Shadow", x, y, 0xFFFFFFFF, true);
        y += 15;
        
        // 方法4: 红色
        Chestonghast.LOGGER.info("M4: Red");
        context.drawText(this.textRenderer, "M4-Red-Color", x, y, 0xFFFF0000, false);
        y += 15;
        
        // 方法5: 蓝色带阴影
        Chestonghast.LOGGER.info("M5: Blue shadow");
        context.drawText(this.textRenderer, "M5-Blue-Shadow", x, y, 0xFF0000FF, true);
        y += 15;
        
        // 方法6: 数据显示
        Chestonghast.LOGGER.info("M6: Data");
        String data = String.format("L%d HP%.0f/%.0f", level, currentHealth, maxHealth);
        context.drawText(this.textRenderer, data, x, y, 0xFF00FF00, false);
        y += 15;
        
        // 方法7: 超大X
        Chestonghast.LOGGER.info("M7: Large X");
        for (int i = 0; i < 5; i++) {
            context.drawText(this.textRenderer, "XXXXXXXXXX", x + i, y + i, 0xFFFF0000, false);
        }
        
        // 黄色确认标记
        context.fill(guiX + GUI_WIDTH - 30, guiY + GUI_HEIGHT - 30, 
                    guiX + GUI_WIDTH - 10, guiY + GUI_HEIGHT - 10, 0xFFFFFF00);
        
        Chestonghast.LOGGER.info("=== Render Done ===");
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
