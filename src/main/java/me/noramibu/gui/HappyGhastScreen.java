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
    
    private static final int GUI_WIDTH = 250;
    private static final int SPACING = 12;  // 缩小间距
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
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
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        super.render(context, mouseX, mouseY, delta);
        
        // 计算居中位置
        int centerX = this.width / 2;
        int startY = 50;
        int y = startY;
        
        // 标题（居中）
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, centerX - titleWidth / 2, y, 0xFFFFFFFF, false);
        y += SPACING * 2;
        
        // 等级（居中）
        Text levelText = Text.translatable("gui.chest-on-ghast.level", level);
        int levelWidth = this.textRenderer.getWidth(levelText);
        context.drawText(this.textRenderer, levelText, centerX - levelWidth / 2, y, 0xFFFFFFFF, false);
        y += SPACING * 2;
        
        // 血量标签（居中）
        Text healthLabel = Text.translatable("gui.chest-on-ghast.health");
        int healthLabelWidth = this.textRenderer.getWidth(healthLabel);
        context.drawText(this.textRenderer, healthLabel, centerX - healthLabelWidth / 2, y, 0xFFFFFFFF, false);
        y += SPACING;
        
        // 血量数值（居中）
        String healthValue = String.format("%.1f / %.1f", currentHealth, maxHealth);
        int healthValueWidth = this.textRenderer.getWidth(healthValue);
        context.drawText(this.textRenderer, healthValue, centerX - healthValueWidth / 2, y, 0xFFFF0000, false);
        y += SPACING;
        
        // 血量进度条（居中）
        int barWidth = 200;
        drawBar(context, centerX - barWidth / 2, y, barWidth, 8, currentHealth / maxHealth, 0xFFFF0000);
        y += SPACING * 2;
        
        // 饱食度标签（居中）
        Text hungerLabel = Text.translatable("gui.chest-on-ghast.hunger");
        int hungerLabelWidth = this.textRenderer.getWidth(hungerLabel);
        context.drawText(this.textRenderer, hungerLabel, centerX - hungerLabelWidth / 2, y, 0xFFFFFFFF, false);
        y += SPACING;
        
        // 饱食度数值（居中）
        String hungerValue = String.format("%.1f / %.1f", hunger, maxHunger);
        int hungerValueWidth = this.textRenderer.getWidth(hungerValue);
        context.drawText(this.textRenderer, hungerValue, centerX - hungerValueWidth / 2, y, 0xFFFF8800, false);
        y += SPACING;
        
        // 饱食度进度条（居中）
        drawBar(context, centerX - barWidth / 2, y, barWidth, 8, hunger / maxHunger, 0xFFFF8800);
        y += SPACING * 2;
        
        // 经验标签（居中）
        Text expLabel = Text.translatable("gui.chest-on-ghast.experience");
        int expLabelWidth = this.textRenderer.getWidth(expLabel);
        context.drawText(this.textRenderer, expLabel, centerX - expLabelWidth / 2, y, 0xFFFFFFFF, false);
        y += SPACING;
        
        if (level < 6) {
            // 经验数值（居中）
            String expValue = String.format("%d / %d", experience, expToNext);
            int expValueWidth = this.textRenderer.getWidth(expValue);
            context.drawText(this.textRenderer, expValue, centerX - expValueWidth / 2, y, 0xFF00FF00, false);
            y += SPACING;
            
            // 经验进度条（居中）
            drawBar(context, centerX - barWidth / 2, y, barWidth, 8, (float)experience / expToNext, 0xFF00FF00);
        } else {
            // 满级文字（居中）
            Text maxLevel = Text.translatable("gui.chest-on-ghast.max_level");
            int maxLevelWidth = this.textRenderer.getWidth(maxLevel);
            context.drawText(this.textRenderer, maxLevel, centerX - maxLevelWidth / 2, y, 0xFFFFD700, false);
            y += SPACING;
            
            // 满级进度条（居中）
            drawBar(context, centerX - barWidth / 2, y, barWidth, 8, 1.0f, 0xFFFFD700);
        }
        y += SPACING * 2;
        
        // 底部提示（居中）
        Text closeHint = Text.translatable("gui.chest-on-ghast.close_hint");
        int closeHintWidth = this.textRenderer.getWidth(closeHint);
        context.drawText(this.textRenderer, closeHint, centerX - closeHintWidth / 2, y, 0xFF888888, false);
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
