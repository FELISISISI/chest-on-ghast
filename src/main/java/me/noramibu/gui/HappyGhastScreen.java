package me.noramibu.gui;

import me.noramibu.accessor.HappyGhastDataAccessor;
import me.noramibu.data.HappyGhastData;
import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.text.Text;
import java.util.List;

public class HappyGhastScreen extends Screen {
    private final int entityId;
    private int level;
    private float currentHealth;
    private float maxHealth;
    private float hunger;
    private float maxHunger;
    private int experience;
    private int expToNext;
    private final boolean isCreative;
    private final List<String> favoriteFoods;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.entityId = payload.entityId();
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
    
    /**
     * 每tick更新实时数据
     */
    @Override
    public void tick() {
        super.tick();
        updateData();
    }
    
    /**
     * 从实体更新数据
     */
    private void updateData() {
        if (this.client == null || this.client.world == null) {
            return;
        }
        
        Entity entity = this.client.world.getEntityById(this.entityId);
        if (entity instanceof HappyGhastEntity ghast) {
            // 更新血量
            this.currentHealth = ghast.getHealth();
            
            // 从accessor获取数据
            if (ghast instanceof HappyGhastDataAccessor accessor) {
                HappyGhastData data = accessor.getGhastData();
                if (data != null) {
                    this.level = data.getLevel();
                    this.hunger = data.getHunger();
                    this.maxHunger = data.getMaxHunger();
                    this.experience = data.getExperience();
                    this.expToNext = data.getExpToNextLevel();
                    this.maxHealth = data.getMaxHealth();
                }
            }
        }
    }
    
    /**
     * 覆盖默认的背景渲染方法
     * 我们在render方法中自定义了背景，所以这里不需要默认的背景渲染
     * 阻止super.render()调用默认的背景模糊效果，避免"Can only blur once per frame"错误
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 不做任何事情，我们在render方法中已经绘制了自定义背景
        // 这样可以防止Minecraft尝试应用默认的背景模糊效果
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
        
        // 如果是创造模式，在左下角低调显示最喜欢的食物
        if (isCreative && favoriteFoods != null && !favoriteFoods.isEmpty()) {
            int leftMargin = 10;
            int bottomMargin = this.height - 60;
            
            // 标题（小字体，半透明）
            Text favTitle = Text.translatable("gui.chest-on-ghast.favorite_foods_hint");
            context.drawText(this.textRenderer, favTitle, leftMargin, bottomMargin, 0x88FFFFFF, false);
            
            // 列出三个最喜欢的食物（使用物品的翻译名称）
            for (int i = 0; i < favoriteFoods.size() && i < 3; i++) {
                String foodId = favoriteFoods.get(i);
                // 使用Minecraft的翻译系统获取物品名称
                Text foodText = Text.translatable("item." + foodId.replace(":", "."));
                context.drawText(this.textRenderer, foodText, leftMargin + 5, bottomMargin + 12 + i * 10, 0x88FFAA88, false);
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
