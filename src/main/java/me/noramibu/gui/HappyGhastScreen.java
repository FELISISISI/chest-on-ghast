package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.HappyGhastEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 快乐恶魂GUI屏幕
 * 显示快乐恶魂的外观预览、血量、饱食度、经验和等级信息
 * 设计原则：美观、信息清晰、排版和谐
 */
public class HappyGhastScreen extends Screen {
    // GUI背景纹理（使用Minecraft原版纹理）
    private static final Identifier STATS_ICONS = Identifier.ofVanilla("textures/gui/icons.png");
    
    // 快乐恶魂数据
    private final int entityId;
    private final int level;
    private final int experience;
    private final float hunger;
    private final float maxHealth;
    private final float currentHealth;
    private final float maxHunger;
    private final int expToNext;
    
    // 实体引用（用于渲染外观预览）
    private Entity ghastEntity;
    
    // GUI尺寸
    private static final int GUI_WIDTH = 256;
    private static final int GUI_HEIGHT = 200;
    
    /**
     * 构造函数
     * @param payload 服务端发送的数据包
     */
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.entityId = payload.entityId();
        this.level = payload.level();
        this.experience = payload.experience();
        this.hunger = payload.hunger();
        this.maxHealth = payload.maxHealth();
        this.currentHealth = payload.currentHealth();
        this.maxHunger = payload.maxHunger();
        this.expToNext = payload.expToNext();
    }
    
    /**
     * 初始化GUI
     */
    @Override
    protected void init() {
        super.init();
        
        // 获取快乐恶魂实体引用
        if (this.client != null && this.client.world != null) {
            this.ghastEntity = this.client.world.getEntityById(entityId);
        }
    }
    
    /**
     * 渲染GUI
     * @param context 绘制上下文
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param delta 帧时间增量
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染简单的半透明背景（不使用模糊效果以避免冲突）
        // 在整个屏幕上绘制一个半透明的黑色背景
        context.fill(0, 0, this.width, this.height, 0x80000000);
        
        // 计算GUI中心位置
        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;
        
        // 绘制GUI背景面板
        renderBackgroundPanel(context, guiX, guiY);
        
        // 绘制标题
        renderTitle(context, guiX);
        
        // 绘制快乐恶魂3D模型预览
        renderGhastPreview(context, guiX + 40, guiY + 80);
        
        // 绘制等级信息
        renderLevelInfo(context, guiX + 150, guiY + 40);
        
        // 绘制血量条
        renderHealthBar(context, guiX + 150, guiY + 70);
        
        // 绘制饱食度条
        renderHungerBar(context, guiX + 150, guiY + 100);
        
        // 绘制经验条
        renderExpBar(context, guiX + 150, guiY + 130);
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * 渲染背景面板
     * 绘制一个带边框的深色半透明背景
     */
    private void renderBackgroundPanel(DrawContext context, int x, int y) {
        // 主背景 - 深灰色半透明
        context.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xD0101010);
        
        // 边框 - 亮灰色
        context.fill(x, y, x + GUI_WIDTH, y + 2, 0xFF8B8B8B); // 顶部
        context.fill(x, y + GUI_HEIGHT - 2, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B8B8B); // 底部
        context.fill(x, y, x + 2, y + GUI_HEIGHT, 0xFF8B8B8B); // 左侧
        context.fill(x + GUI_WIDTH - 2, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFF8B8B8B); // 右侧
        
        // 装饰线 - 金色
        context.fill(x + 10, y + 30, x + GUI_WIDTH - 10, y + 32, 0xFFFFD700);
    }
    
    /**
     * 渲染标题
     */
    private void renderTitle(DrawContext context, int x) {
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, 
            x + (GUI_WIDTH - titleWidth) / 2, 
            (this.height - GUI_HEIGHT) / 2 + 10, 
            0xFFD700, true);
    }
    
    /**
     * 渲染快乐恶魂外观预览
     * 使用简化的图形表示快乐恶魂
     */
    private void renderGhastPreview(DrawContext context, int x, int y) {
        // 绘制一个简化的快乐恶魂图标
        int size = 60;
        
        // 主体 - 白色方块
        context.fill(x - size/2, y - size/2, x + size/2, y + size/2, 0xFFFFFFFF);
        
        // 边框 - 浅灰色
        context.fill(x - size/2, y - size/2, x + size/2, y - size/2 + 2, 0xFFCCCCCC);
        context.fill(x - size/2, y + size/2 - 2, x + size/2, y + size/2, 0xFFCCCCCC);
        context.fill(x - size/2, y - size/2, x - size/2 + 2, y + size/2, 0xFFCCCCCC);
        context.fill(x + size/2 - 2, y - size/2, x + size/2, y + size/2, 0xFFCCCCCC);
        
        // 眼睛 - 黑色
        int eyeSize = 6;
        int eyeOffset = 12;
        context.fill(x - eyeOffset, y - 10, x - eyeOffset + eyeSize, y - 10 + eyeSize, 0xFF000000);
        context.fill(x + eyeOffset - eyeSize, y - 10, x + eyeOffset, y - 10 + eyeSize, 0xFF000000);
        
        // 微笑 - 黑色弧线（简化为矩形组合）
        int mouthY = y + 5;
        int mouthWidth = 20;
        context.fill(x - mouthWidth/2, mouthY, x + mouthWidth/2, mouthY + 2, 0xFF000000);
        context.fill(x - mouthWidth/2, mouthY, x - mouthWidth/2 + 2, mouthY + 5, 0xFF000000);
        context.fill(x + mouthWidth/2 - 2, mouthY, x + mouthWidth/2, mouthY + 5, 0xFF000000);
        
        // 等级徽章（在右上角）
        String levelBadge = "Lv." + level;
        int badgeX = x + size/2 - this.textRenderer.getWidth(levelBadge) - 5;
        int badgeY = y - size/2 + 5;
        context.fill(badgeX - 2, badgeY - 2, badgeX + this.textRenderer.getWidth(levelBadge) + 2, badgeY + 10, 0xD0FFD700);
        context.drawText(this.textRenderer, levelBadge, badgeX, badgeY, 0x000000, false);
    }
    
    /**
     * 渲染等级信息
     */
    private void renderLevelInfo(DrawContext context, int x, int y) {
        Text levelText = Text.translatable("gui.chest-on-ghast.level", level);
        context.drawText(this.textRenderer, levelText, x, y, 0xFFFFFF, true);
    }
    
    /**
     * 渲染血量条
     */
    private void renderHealthBar(DrawContext context, int x, int y) {
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.health");
        context.drawText(this.textRenderer, label, x, y - 10, 0xFFFFFF, false);
        
        // 血量条背景
        int barWidth = 80;
        int barHeight = 10;
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF555555);
        
        // 血量条前景（红色）
        float healthRatio = currentHealth / maxHealth;
        int healthBarWidth = (int)(barWidth * healthRatio);
        context.fill(x, y, x + healthBarWidth, y + barHeight, 0xFFFF0000);
        
        // 血量数值文本
        String healthText = String.format("%.1f / %.1f", currentHealth, maxHealth);
        int textX = x + (barWidth - this.textRenderer.getWidth(healthText)) / 2;
        context.drawText(this.textRenderer, healthText, textX, y + 1, 0xFFFFFF, true);
    }
    
    /**
     * 渲染饱食度条
     */
    private void renderHungerBar(DrawContext context, int x, int y) {
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.hunger");
        context.drawText(this.textRenderer, label, x, y - 10, 0xFFFFFF, false);
        
        // 饱食度条背景
        int barWidth = 80;
        int barHeight = 10;
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF555555);
        
        // 饱食度条前景（橙色）
        float hungerRatio = hunger / maxHunger;
        int hungerBarWidth = (int)(barWidth * hungerRatio);
        context.fill(x, y, x + hungerBarWidth, y + barHeight, 0xFFFF8C00);
        
        // 饱食度数值文本
        String hungerText = String.format("%.1f / %.1f", hunger, maxHunger);
        int textX = x + (barWidth - this.textRenderer.getWidth(hungerText)) / 2;
        context.drawText(this.textRenderer, hungerText, textX, y + 1, 0xFFFFFF, true);
    }
    
    /**
     * 渲染经验条
     */
    private void renderExpBar(DrawContext context, int x, int y) {
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.experience");
        context.drawText(this.textRenderer, label, x, y - 10, 0xFFFFFF, false);
        
        // 经验条背景
        int barWidth = 80;
        int barHeight = 10;
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF555555);
        
        // 经验条前景（绿色）
        if (level < 6) {
            float expRatio = (float)experience / expToNext;
            int expBarWidth = (int)(barWidth * expRatio);
            context.fill(x, y, x + expBarWidth, y + barHeight, 0xFF00FF00);
            
            // 经验数值文本
            String expText = experience + " / " + expToNext;
            int textX = x + (barWidth - this.textRenderer.getWidth(expText)) / 2;
            context.drawText(this.textRenderer, expText, textX, y + 1, 0xFFFFFF, true);
        } else {
            // 满级显示
            context.fill(x, y, x + barWidth, y + barHeight, 0xFF00FF00);
            String maxText = Text.translatable("gui.chest-on-ghast.max_level").getString();
            int textX = x + (barWidth - this.textRenderer.getWidth(maxText)) / 2;
            context.drawText(this.textRenderer, maxText, textX, y + 1, 0xFFFFFF, true);
        }
    }
    
    /**
     * 检查是否应该暂停游戏
     * GUI打开时不暂停游戏
     */
    @Override
    public boolean shouldPause() {
        return false;
    }
}
