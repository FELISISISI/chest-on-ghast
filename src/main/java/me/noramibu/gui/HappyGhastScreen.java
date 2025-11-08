package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.util.List;

public class HappyGhastScreen extends Screen {
    private final int level;
    private final float currentHealth;
    private final float maxHealth;
    private final float hunger;
    private final float maxHunger;
    private final int experience;
    private final int expToNext;
    private final boolean isCreative;
    private final List<String> favoriteFoods;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.level = payload.level();
        this.currentHealth = payload.currentHealth();
        this.maxHealth = payload.maxHealth();
        this.hunger = payload.hunger();
        this.maxHunger = payload.maxHunger();
        this.experience = payload.experience();
        this.expToNext = payload.expToNext();
        this.isCreative = payload.isCreative();
        this.favoriteFoods = payload.favoriteFoods();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        super.render(context, mouseX, mouseY, delta);
        
        // 计算中心位置
        int centerX = this.width / 2;
        int topY = 20;
        
        // 标题居中
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, centerX - titleWidth / 2, topY, 0xFFFFFFFF, false);
        
        // 等级居中
        Text levelText = Text.translatable("gui.chest-on-ghast.level", level);
        int levelWidth = this.textRenderer.getWidth(levelText);
        context.drawText(this.textRenderer, levelText, centerX - levelWidth / 2, topY + 15, 0xFFFFD700, false);
        
        // 三个数据块横向排列
        int dataY = topY + 35;
        int blockWidth = 100;
        int spacing = 20;
        int totalWidth = blockWidth * 3 + spacing * 2;
        int startX = centerX - totalWidth / 2;
        
        // 血量块（左）
        drawDataBlock(context, startX, dataY, blockWidth,
            Text.translatable("gui.chest-on-ghast.health"),
            String.format("%.1f/%.1f", currentHealth, maxHealth),
            currentHealth / maxHealth,
            0xFFFF0000);
        
        // 饱食度块（中）
        drawDataBlock(context, startX + blockWidth + spacing, dataY, blockWidth,
            Text.translatable("gui.chest-on-ghast.hunger"),
            String.format("%.1f/%.1f", hunger, maxHunger),
            hunger / maxHunger,
            0xFFFF8800);
        
        // 经验块（右）
        if (level < 6) {
            drawDataBlock(context, startX + (blockWidth + spacing) * 2, dataY, blockWidth,
                Text.translatable("gui.chest-on-ghast.experience"),
                String.format("%d/%d", experience, expToNext),
                (float)experience / expToNext,
                0xFF00FF00);
        } else {
            drawDataBlock(context, startX + (blockWidth + spacing) * 2, dataY, blockWidth,
                Text.translatable("gui.chest-on-ghast.experience"),
                Text.translatable("gui.chest-on-ghast.max_level").getString(),
                1.0f,
                0xFFFFD700);
        }
        
        // 底部提示（居中）
        int hintY = dataY + 70;
        Text closeHint = Text.translatable("gui.chest-on-ghast.close_hint");
        int closeHintWidth = this.textRenderer.getWidth(closeHint);
        context.drawText(this.textRenderer, closeHint, centerX - closeHintWidth / 2, hintY, 0xFF888888, false);
        
        // 如果是创造模式，显示最喜欢的食物
        if (isCreative && favoriteFoods != null && !favoriteFoods.isEmpty()) {
            int foodY = hintY + 25;
            
            // 标题
            Text favTitle = Text.translatable("gui.chest-on-ghast.favorite_foods");
            int favTitleWidth = this.textRenderer.getWidth(favTitle);
            context.drawText(this.textRenderer, favTitle, centerX - favTitleWidth / 2, foodY, 0xFFFFD700, false);
            
            // 列出三个最喜欢的食物
            for (int i = 0; i < favoriteFoods.size() && i < 3; i++) {
                String foodId = favoriteFoods.get(i);
                // 提取物品名称（去掉minecraft:前缀）
                String foodName = foodId.replace("minecraft:", "");
                Text foodText = Text.literal("❤ " + foodName);
                int foodWidth = this.textRenderer.getWidth(foodText);
                context.drawText(this.textRenderer, foodText, centerX - foodWidth / 2, foodY + 15 + i * 12, 0xFFFF69B4, false);
            }
        }
    }
    
    /**
     * 绘制单个数据块
     * @param context 绘制上下文
     * @param x 左上角X坐标
     * @param y 左上角Y坐标
     * @param width 宽度
     * @param label 标签文本
     * @param value 数值文本
     * @param ratio 进度比例
     * @param color 进度条颜色
     */
    private void drawDataBlock(DrawContext context, int x, int y, int width, 
                               Text label, String value, float ratio, int color) {
        int centerX = x + width / 2;
        
        // 标签（居中）
        int labelWidth = this.textRenderer.getWidth(label);
        context.drawText(this.textRenderer, label, centerX - labelWidth / 2, y, 0xFFFFFFFF, false);
        
        // 数值（居中）
        int valueWidth = this.textRenderer.getWidth(value);
        context.drawText(this.textRenderer, value, centerX - valueWidth / 2, y + 12, color, false);
        
        // 进度条（居中）
        int barWidth = width - 10;
        int barHeight = 6;
        int barX = x + (width - barWidth) / 2;
        int barY = y + 26;
        
        // 边框
        context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF000000);
        // 背景
        context.fill(barX + 1, barY + 1, barX + barWidth - 1, barY + barHeight - 1, 0xFF333333);
        // 进度
        int fillWidth = (int)((barWidth - 2) * Math.max(0, Math.min(1, ratio)));
        if (fillWidth > 0) {
            context.fill(barX + 1, barY + 1, barX + 1 + fillWidth, barY + barHeight - 1, color);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
