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
        drawBar(context, startX, startY + (line++ * 15), 200, 10, currentHealth / maxHealth, 0xFFFF0000);
        line++;
        
        context.drawText(this.textRenderer, "Hunger:", startX, startY + (line++ * 15), 0xFFFFFFFF, false);
        context.drawText(this.textRenderer, String.format("  %.1f / %.1f", hunger, maxHunger), startX, startY + (line++ * 15), 0xFFFF8800, false);
        drawBar(context, startX, startY + (line++ * 15), 200, 10, hunger / maxHunger, 0xFFFF8800);
        line++;
        
        if (level < 6) {
            context.drawText(this.textRenderer, "Experience:", startX, startY + (line++ * 15), 0xFFFFFFFF, false);
            context.drawText(this.textRenderer, String.format("  %d / %d", experience, expToNext), startX, startY + (line++ * 15), 0xFF00FF00, false);
            drawBar(context, startX, startY + (line++ * 15), 200, 10, (float)experience / expToNext, 0xFF00FF00);
        } else {
            context.drawText(this.textRenderer, "Experience: MAX LEVEL", startX, startY + (line++ * 15), 0xFFFFD700, false);
            drawBar(context, startX, startY + (line++ * 15), 200, 10, 1.0f, 0xFFFFD700);
        }
        
        // 底部提示
        line += 2;
        context.drawText(this.textRenderer, "Press ESC to close", startX, startY + (line * 15), 0xFFAAAAAA, false);
    }
    
    private void drawBar(DrawContext context, int x, int y, int width, int height, float ratio, int color) {
        // 边框（黑色）
        context.fill(x, y, x + width, y + height, 0xFF000000);
        
        // 背景（深灰色）
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF333333);
        
        // 进度条
        int barWidth = (int)((width - 2) * Math.max(0, Math.min(1, ratio)));
        if (barWidth > 0) {
            context.fill(x + 1, y + 1, x + 1 + barWidth, y + height - 1, color);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
