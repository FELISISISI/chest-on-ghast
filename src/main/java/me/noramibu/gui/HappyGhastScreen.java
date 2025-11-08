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
 * 显示快乐恶魂的血量、饱食度、经验和等级信息
 * 使用Minecraft官方GUI样式，简洁美观
 */
public class HappyGhastScreen extends Screen {
    // GUI背景纹理（使用Minecraft原版容器纹理）
    private static final Identifier BACKGROUND_TEXTURE = Identifier.ofVanilla("textures/gui/demo_background.png");
    
    // 快乐恶魂数据
    private final int entityId;
    private final int level;
    private final int experience;
    private final float hunger;
    private final float maxHealth;
    private final float currentHealth;
    private final float maxHunger;
    private final int expToNext;
    
    // GUI尺寸（使用更紧凑的布局）
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    
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
        // 渲染暗淡的背景（使用Minecraft风格）
        super.render(context, mouseX, mouseY, delta);
        
        // 计算GUI中心位置
        int guiX = (this.width - GUI_WIDTH) / 2;
        int guiY = (this.height - GUI_HEIGHT) / 2;
        
        // 绘制Minecraft官方风格的GUI背景
        renderMinecraftStyleBackground(context, guiX, guiY);
        
        // 绘制标题
        renderTitle(context, guiX, guiY);
        
        // 绘制等级信息（显眼位置）
        renderLevelInfo(context, guiX + 10, guiY + 20);
        
        // 绘制血量条
        renderHealthBar(context, guiX + 10, guiY + 50);
        
        // 绘制饱食度条
        renderHungerBar(context, guiX + 10, guiY + 85);
        
        // 绘制经验条
        renderExpBar(context, guiX + 10, guiY + 120);
    }
    
    /**
     * 渲染Minecraft官方风格的背景
     * 使用九宫格纹理绘制标准容器背景
     */
    private void renderMinecraftStyleBackground(DrawContext context, int x, int y) {
        // 使用Minecraft的标准方法绘制九宫格背景
        // 绘制背景纹理（使用渐变效果模拟官方样式）
        
        // 主背景 - 使用Minecraft标准灰色
        context.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFC6C6C6);
        
        // 绘制顶部边框（深色）
        context.fill(x, y, x + GUI_WIDTH, y + 1, 0xFF373737);
        context.fill(x, y + 1, x + 1, y + GUI_HEIGHT, 0xFF373737);
        
        // 绘制底部和右侧边框（亮色）
        context.fill(x, y + GUI_HEIGHT - 1, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFFFFFFF);
        context.fill(x + GUI_WIDTH - 1, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFFFFFFF);
        
        // 内部阴影效果
        context.fill(x + 1, y + 1, x + GUI_WIDTH - 1, y + 2, 0xFF8B8B8B);
        context.fill(x + 1, y + 1, x + 2, y + GUI_HEIGHT - 1, 0xFF8B8B8B);
    }
    
    /**
     * 渲染标题
     */
    private void renderTitle(DrawContext context, int x, int y) {
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        // 标题居中显示，使用深色文字
        context.drawText(this.textRenderer, title, 
            x + (GUI_WIDTH - titleWidth) / 2, 
            y + 6, 
            0x404040, false);
    }
    
    /**
     * 渲染等级信息
     */
    private void renderLevelInfo(DrawContext context, int x, int y) {
        // 等级标题
        Text levelLabel = Text.literal("等级: ");
        context.drawText(this.textRenderer, levelLabel, x, y, 0x404040, false);
        
        // 等级数值（使用金色突出显示）
        String levelValue = String.valueOf(level);
        int labelWidth = this.textRenderer.getWidth(levelLabel);
        context.drawText(this.textRenderer, levelValue, x + labelWidth, y, 0xFFAA00, false);
    }
    
    /**
     * 渲染血量条
     */
    private void renderHealthBar(DrawContext context, int x, int y) {
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.health");
        context.drawText(this.textRenderer, label, x, y, 0x404040, false);
        
        // 血量条位置调整
        int barY = y + 12;
        int barWidth = 156;
        int barHeight = 14;
        
        // 血量条外边框（深色）
        context.fill(x, barY, x + barWidth, barY + barHeight, 0xFF000000);
        
        // 血量条内边框（浅色）
        context.fill(x + 1, barY + 1, x + barWidth - 1, barY + barHeight - 1, 0xFF8B8B8B);
        
        // 血量条背景（深灰色）
        context.fill(x + 2, barY + 2, x + barWidth - 2, barY + barHeight - 2, 0xFF555555);
        
        // 血量条前景（红色渐变效果）
        float healthRatio = currentHealth / maxHealth;
        int healthBarWidth = (int)((barWidth - 4) * healthRatio);
        if (healthBarWidth > 0) {
            // 深红色底
            context.fill(x + 2, barY + 2, x + 2 + healthBarWidth, barY + barHeight - 2, 0xFFCC0000);
            // 亮红色高光
            context.fill(x + 2, barY + 2, x + 2 + healthBarWidth, barY + 4, 0xFFFF0000);
        }
        
        // 血量数值文本（居中显示）
        String healthText = String.format("%.1f / %.1f", currentHealth, maxHealth);
        int textX = x + (barWidth - this.textRenderer.getWidth(healthText)) / 2;
        context.drawText(this.textRenderer, healthText, textX, barY + 3, 0xFFFFFF, true);
    }
    
    /**
     * 渲染饱食度条
     */
    private void renderHungerBar(DrawContext context, int x, int y) {
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.hunger");
        context.drawText(this.textRenderer, label, x, y, 0x404040, false);
        
        // 饱食度条位置调整
        int barY = y + 12;
        int barWidth = 156;
        int barHeight = 14;
        
        // 饱食度条外边框（深色）
        context.fill(x, barY, x + barWidth, barY + barHeight, 0xFF000000);
        
        // 饱食度条内边框（浅色）
        context.fill(x + 1, barY + 1, x + barWidth - 1, barY + barHeight - 1, 0xFF8B8B8B);
        
        // 饱食度条背景（深灰色）
        context.fill(x + 2, barY + 2, x + barWidth - 2, barY + barHeight - 2, 0xFF555555);
        
        // 饱食度条前景（橙色渐变效果）
        float hungerRatio = hunger / maxHunger;
        int hungerBarWidth = (int)((barWidth - 4) * hungerRatio);
        if (hungerBarWidth > 0) {
            // 深橙色底
            context.fill(x + 2, barY + 2, x + 2 + hungerBarWidth, barY + barHeight - 2, 0xFFCC6600);
            // 亮橙色高光
            context.fill(x + 2, barY + 2, x + 2 + hungerBarWidth, barY + 4, 0xFFFF8C00);
        }
        
        // 饱食度数值文本（居中显示）
        String hungerText = String.format("%.1f / %.1f", hunger, maxHunger);
        int textX = x + (barWidth - this.textRenderer.getWidth(hungerText)) / 2;
        context.drawText(this.textRenderer, hungerText, textX, barY + 3, 0xFFFFFF, true);
    }
    
    /**
     * 渲染经验条
     */
    private void renderExpBar(DrawContext context, int x, int y) {
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.experience");
        context.drawText(this.textRenderer, label, x, y, 0x404040, false);
        
        // 经验条位置调整
        int barY = y + 12;
        int barWidth = 156;
        int barHeight = 14;
        
        // 经验条外边框（深色）
        context.fill(x, barY, x + barWidth, barY + barHeight, 0xFF000000);
        
        // 经验条内边框（浅色）
        context.fill(x + 1, barY + 1, x + barWidth - 1, barY + barHeight - 1, 0xFF8B8B8B);
        
        // 经验条背景（深灰色）
        context.fill(x + 2, barY + 2, x + barWidth - 2, barY + barHeight - 2, 0xFF555555);
        
        // 经验条前景（绿色渐变效果）
        if (level < 6) {
            float expRatio = (float)experience / expToNext;
            int expBarWidth = (int)((barWidth - 4) * expRatio);
            if (expBarWidth > 0) {
                // 深绿色底
                context.fill(x + 2, barY + 2, x + 2 + expBarWidth, barY + barHeight - 2, 0xFF00AA00);
                // 亮绿色高光
                context.fill(x + 2, barY + 2, x + 2 + expBarWidth, barY + 4, 0xFF00FF00);
            }
            
            // 经验数值文本（居中显示）
            String expText = experience + " / " + expToNext;
            int textX = x + (barWidth - this.textRenderer.getWidth(expText)) / 2;
            context.drawText(this.textRenderer, expText, textX, barY + 3, 0xFFFFFF, true);
        } else {
            // 满级显示（充满绿色）
            context.fill(x + 2, barY + 2, x + barWidth - 2, barY + barHeight - 2, 0xFF00AA00);
            context.fill(x + 2, barY + 2, x + barWidth - 2, barY + 4, 0xFF00FF00);
            
            String maxText = Text.translatable("gui.chest-on-ghast.max_level").getString();
            int textX = x + (barWidth - this.textRenderer.getWidth(maxText)) / 2;
            context.drawText(this.textRenderer, maxText, textX, barY + 3, 0xFFFFFF, true);
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
