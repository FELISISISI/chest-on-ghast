package me.noramibu.gui;

import me.noramibu.Chestonghast;
import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 快乐恶魂GUI屏幕
 * 显示快乐恶魂的血量、饱食度、经验和等级信息
 */
public class HappyGhastScreen extends Screen {
    // 快乐恶魂数据
    private final int level;
    private final int experience;
    private final float hunger;
    private final float maxHealth;
    private final float currentHealth;
    private final float maxHunger;
    private final int expToNext;
    
    // GUI尺寸
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    
    // GUI位置
    private int guiX;
    private int guiY;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.level = payload.level();
        this.experience = payload.experience();
        this.hunger = payload.hunger();
        this.maxHealth = payload.maxHealth();
        this.currentHealth = payload.currentHealth();
        this.maxHunger = payload.maxHunger();
        this.expToNext = payload.expToNext();
        
        Chestonghast.LOGGER.info("GUI created - Level: {}, HP: {}/{}", level, currentHealth, maxHealth);
    }
    
    @Override
    protected void init() {
        super.init();
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. 暗淡背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // 2. GUI面板
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFC6C6C6);
        
        // 3. 边框
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + 1, 0xFF373737);
        context.fill(guiX, guiY + 1, guiX + 1, guiY + GUI_HEIGHT, 0xFF373737);
        context.fill(guiX, guiY + GUI_HEIGHT - 1, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        context.fill(guiX + GUI_WIDTH - 1, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        
        // 4. 父类render
        super.render(context, mouseX, mouseY, delta);
        
        // 5. 文字（在最上层）
        int textX = guiX + 10;
        int textY = guiY + 10;
        
        // 红色标题
        context.drawText(this.textRenderer, Text.literal("Happy Ghast"), textX, textY, 0xFF0000, false);
        textY += 15;
        
        // 绿色等级
        context.drawText(this.textRenderer, "Level: " + level, textX, textY, 0x00FF00, false);
        textY += 15;
        
        // 白色血量（带阴影）
        context.drawText(this.textRenderer, String.format("HP: %.1f/%.1f", currentHealth, maxHealth), textX, textY, 0xFFFFFF, true);
        textY += 15;
        
        // 橙色饱食度
        context.drawText(this.textRenderer, String.format("Food: %.1f/%.1f", hunger, maxHunger), textX, textY, 0xFFAA00, false);
        textY += 15;
        
        // 蓝色经验
        if (level < 6) {
            context.drawText(this.textRenderer, String.format("EXP: %d/%d", experience, expToNext), textX, textY, 0x00AAFF, false);
        } else {
            context.drawText(this.textRenderer, "MAX LEVEL", textX, textY, 0xFFD700, false);
        }
        
        // 彩色参照方块
        context.fill(guiX + 150, guiY + 10, guiX + 160, guiY + 20, 0xFFFF0000);
        context.fill(guiX + 150, guiY + 25, guiX + 160, guiY + 35, 0xFF00FF00);
        context.fill(guiX + 150, guiY + 40, guiX + 160, guiY + 50, 0xFF0000FF);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
