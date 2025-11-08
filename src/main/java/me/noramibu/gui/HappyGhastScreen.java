package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class HappyGhastScreen extends Screen {
    private final int level;
    private final float currentHealth;
    private final float maxHealth;
    private final float hunger;
    private final float maxHunger;
    private final int experience;
    private final int expToNext;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.literal("Happy Ghast"));
        this.level = payload.level();
        this.currentHealth = payload.currentHealth();
        this.maxHealth = payload.maxHealth();
        this.hunger = payload.hunger();
        this.maxHunger = payload.maxHunger();
        this.experience = payload.experience();
        this.expToNext = payload.expToNext();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明暗色背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        super.render(context, mouseX, mouseY, delta);
        
        // 直接在屏幕上绘制文字 - 使用白色文字（在暗色背景上更清晰）
        int startX = 50;
        int startY = 50;
        int line = 0;
        
        // 每一行都是独立的drawText调用
        context.drawText(this.textRenderer, "=== Happy Ghast Info ===", startX, startY + (line++ * 15), 0xFFFFFFFF, false);
        line++;
        
        context.drawText(this.textRenderer, "Level: " + level, startX, startY + (line++ * 15), 0xFFFFFFFF, false);
        line++;
        
        context.drawText(this.textRenderer, "Health:", startX, startY + (line++ * 15), 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, String.format("  %.1f / %.1f", currentHealth, maxHealth), startX, startY + (line++ * 15), 0xFFFF0000, false);
        line++;
        
        context.drawText(this.textRenderer, "Hunger:", startX, startY + (line++ * 15), 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, String.format("  %.1f / %.1f", hunger, maxHunger), startX, startY + (line++ * 15), 0xFFFF8800, false);
        line++;
        
        if (level < 6) {
            context.drawText(this.textRenderer, "Experience:", startX, startY + (line++ * 15), 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, String.format("  %d / %d", experience, expToNext), startX, startY + (line++ * 15), 0xFF00FF00, false);
        } else {
            context.drawText(this.textRenderer, "Experience: MAX LEVEL", startX, startY + (line++ * 15), 0xFFFFD700, false);
        }
        
        // 底部提示
        line += 2;
        context.drawText(this.textRenderer, "Press ESC to close", startX, startY + (line * 15), 0xFFAAAAAA, false);
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
