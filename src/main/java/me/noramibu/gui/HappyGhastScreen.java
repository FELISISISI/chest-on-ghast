package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 快乐恶魂GUI屏幕
 * 显示快乐恶魂的血量、饱食度、经验和等级信息
 * 使用Minecraft官方GUI样式
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
    
    /**
     * 构造函数
     * @param payload 服务端发送的数据包
     */
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
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
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
    }
    
    /**
     * 渲染GUI
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 1. 渲染暗淡的背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // 2. 渲染GUI背景面板（使用Minecraft标准灰色）
        renderBackground(context);
        
        // 3. 调用父类render
        super.render(context, mouseX, mouseY, delta);
        
        // 4. 渲染文字内容（在最上层）
        renderContent(context);
    }
    
    /**
     * 渲染GUI背景
     */
    private void renderBackground(DrawContext context) {
        // 主背景 - Minecraft标准灰色
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFC6C6C6);
        
        // 3D边框效果 - 深色（顶部和左侧）
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + 1, 0xFF373737);
        context.fill(guiX, guiY + 1, guiX + 1, guiY + GUI_HEIGHT, 0xFF373737);
        
        // 3D边框效果 - 亮色（底部和右侧）
        context.fill(guiX, guiY + GUI_HEIGHT - 1, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        context.fill(guiX + GUI_WIDTH - 1, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        
        // 内部阴影
        context.fill(guiX + 1, guiY + 1, guiX + GUI_WIDTH - 1, guiY + 2, 0xFF8B8B8B);
        context.fill(guiX + 1, guiY + 1, guiX + 2, guiY + GUI_HEIGHT - 1, 0xFF8B8B8B);
    }
    
    /**
     * 渲染GUI内容
     */
    private void renderContent(DrawContext context) {
        int x = guiX + 10;
        int y = guiY + 8;
        
        // 标题
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, 
            guiX + (GUI_WIDTH - titleWidth) / 2, y, 0x404040, false);
        y += 20;
        
        // 等级信息
        Text levelLabel = Text.translatable("gui.chest-on-ghast.level", level);
        context.drawText(this.textRenderer, levelLabel, x, y, 0x404040, false);
        y += 18;
        
        // 血量条
        Text healthLabel = Text.translatable("gui.chest-on-ghast.health");
        context.drawText(this.textRenderer, healthLabel, x, y, 0x404040, false);
        y += 12;
        renderProgressBar(context, x, y, 156, 14, currentHealth / maxHealth, 0xFFCC0000, 0xFFFF0000);
        String healthText = String.format("%.1f / %.1f", currentHealth, maxHealth);
        int healthTextX = x + (156 - this.textRenderer.getWidth(healthText)) / 2;
        context.drawText(this.textRenderer, healthText, healthTextX, y + 3, 0xFFFFFF, true);
        y += 22;
        
        // 饱食度条
        Text hungerLabel = Text.translatable("gui.chest-on-ghast.hunger");
        context.drawText(this.textRenderer, hungerLabel, x, y, 0x404040, false);
        y += 12;
        renderProgressBar(context, x, y, 156, 14, hunger / maxHunger, 0xFFCC6600, 0xFFFF8C00);
        String hungerText = String.format("%.1f / %.1f", hunger, maxHunger);
        int hungerTextX = x + (156 - this.textRenderer.getWidth(hungerText)) / 2;
        context.drawText(this.textRenderer, hungerText, hungerTextX, y + 3, 0xFFFFFF, true);
        y += 22;
        
        // 经验条
        Text expLabel = Text.translatable("gui.chest-on-ghast.experience");
        context.drawText(this.textRenderer, expLabel, x, y, 0x404040, false);
        y += 12;
        
        if (level < 6) {
            // 未满级 - 显示经验进度
            float expRatio = (float) experience / expToNext;
            renderProgressBar(context, x, y, 156, 14, expRatio, 0xFF00AA00, 0xFF00FF00);
            String expText = experience + " / " + expToNext;
            int expTextX = x + (156 - this.textRenderer.getWidth(expText)) / 2;
            context.drawText(this.textRenderer, expText, expTextX, y + 3, 0xFFFFFF, true);
        } else {
            // 满级 - 显示满级文字
            renderProgressBar(context, x, y, 156, 14, 1.0f, 0xFF00AA00, 0xFF00FF00);
            Text maxLevelText = Text.translatable("gui.chest-on-ghast.max_level");
            int maxTextX = x + (156 - this.textRenderer.getWidth(maxLevelText)) / 2;
            context.drawText(this.textRenderer, maxLevelText, maxTextX, y + 3, 0xFFFFFF, true);
        }
    }
    
    /**
     * 渲染进度条
     * @param context 绘制上下文
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param ratio 进度比例（0.0-1.0）
     * @param darkColor 深色（底色）
     * @param lightColor 亮色（高光）
     */
    private void renderProgressBar(DrawContext context, int x, int y, int width, int height, 
                                   float ratio, int darkColor, int lightColor) {
        // 外边框（黑色）
        context.fill(x, y, x + width, y + height, 0xFF000000);
        
        // 内边框（灰色）
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF8B8B8B);
        
        // 背景（深灰色）
        context.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xFF555555);
        
        // 进度条前景（渐变效果）
        int barWidth = (int)((width - 4) * Math.max(0, Math.min(1, ratio)));
        if (barWidth > 0) {
            // 深色底
            context.fill(x + 2, y + 2, x + 2 + barWidth, y + height - 2, darkColor);
            // 亮色高光
            context.fill(x + 2, y + 2, x + 2 + barWidth, y + 4, lightColor);
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
