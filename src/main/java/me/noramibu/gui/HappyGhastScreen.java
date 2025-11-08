package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
    
    // GUI位置（计算后的中心位置）
    private int guiX;
    private int guiY;
    
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
     * 计算GUI中心位置
     */
    @Override
    protected void init() {
        super.init();
        // 计算GUI中心位置
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
    }
    
    /**
     * 渲染GUI背景层
     * 在render之前调用，渲染暗淡的背景
     */
    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染暗淡的背景层（手动绘制半透明黑色，避免模糊冲突）
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }
    
    /**
     * 渲染GUI主体
     * @param context 绘制上下文
     * @param mouseX 鼠标X坐标
     * @param mouseY 鼠标Y坐标
     * @param delta 帧时间增量
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 先渲染背景
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // 绘制Minecraft官方风格的GUI背景面板
        renderMinecraftStyleBackground(context);
        
        // 绘制标题
        renderTitle(context);
        
        // 绘制等级信息
        renderLevelInfo(context);
        
        // 绘制血量条
        renderHealthBar(context);
        
        // 绘制饱食度条
        renderHungerBar(context);
        
        // 绘制经验条
        renderExpBar(context);
        
        // 最后调用父类的render方法（渲染子组件和鼠标悬浮提示）
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * 渲染Minecraft官方风格的背景
     * 使用标准的灰色背景和3D边框
     */
    private void renderMinecraftStyleBackground(DrawContext context) {
        // 主背景 - 使用Minecraft标准灰色
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFC6C6C6);
        
        // 绘制顶部和左侧边框（深色阴影）
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + 1, 0xFF373737);
        context.fill(guiX, guiY + 1, guiX + 1, guiY + GUI_HEIGHT, 0xFF373737);
        
        // 绘制底部和右侧边框（亮色高光）
        context.fill(guiX, guiY + GUI_HEIGHT - 1, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        context.fill(guiX + GUI_WIDTH - 1, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        
        // 内部阴影效果
        context.fill(guiX + 1, guiY + 1, guiX + GUI_WIDTH - 1, guiY + 2, 0xFF8B8B8B);
        context.fill(guiX + 1, guiY + 1, guiX + 2, guiY + GUI_HEIGHT - 1, 0xFF8B8B8B);
    }
    
    /**
     * 渲染标题
     */
    private void renderTitle(DrawContext context) {
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        // 标题居中显示，使用深色文字
        int titleX = guiX + (GUI_WIDTH - titleWidth) / 2;
        int titleY = guiY + 6;
        context.drawText(this.textRenderer, title, titleX, titleY, 0x404040, false);
    }
    
    /**
     * 渲染等级信息
     */
    private void renderLevelInfo(DrawContext context) {
        int x = guiX + 10;
        int y = guiY + 20;
        
        // 等级文本
        Text levelText = Text.literal("等级: " + level);
        context.drawText(this.textRenderer, levelText, x, y, 0x404040, false);
        
        // 等级数值用金色突出显示
        String levelStr = String.valueOf(level);
        int labelWidth = this.textRenderer.getWidth("等级: ");
        context.drawText(this.textRenderer, levelStr, x + labelWidth, y, 0xFFAA00, false);
    }
    
    /**
     * 渲染血量条
     */
    private void renderHealthBar(DrawContext context) {
        int x = guiX + 10;
        int y = guiY + 50;
        
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.health");
        context.drawText(this.textRenderer, label, x, y, 0x404040, false);
        
        // 进度条
        int barY = y + 12;
        renderProgressBar(context, x, barY, 156, 14, currentHealth / maxHealth, 0xFFCC0000, 0xFFFF0000);
        
        // 数值文本
        String healthText = String.format("%.1f / %.1f", currentHealth, maxHealth);
        int textX = x + (156 - this.textRenderer.getWidth(healthText)) / 2;
        context.drawText(this.textRenderer, healthText, textX, barY + 3, 0xFFFFFF, true);
    }
    
    /**
     * 渲染饱食度条
     */
    private void renderHungerBar(DrawContext context) {
        int x = guiX + 10;
        int y = guiY + 85;
        
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.hunger");
        context.drawText(this.textRenderer, label, x, y, 0x404040, false);
        
        // 进度条
        int barY = y + 12;
        renderProgressBar(context, x, barY, 156, 14, hunger / maxHunger, 0xFFCC6600, 0xFFFF8C00);
        
        // 数值文本
        String hungerText = String.format("%.1f / %.1f", hunger, maxHunger);
        int textX = x + (156 - this.textRenderer.getWidth(hungerText)) / 2;
        context.drawText(this.textRenderer, hungerText, textX, barY + 3, 0xFFFFFF, true);
    }
    
    /**
     * 渲染经验条
     */
    private void renderExpBar(DrawContext context) {
        int x = guiX + 10;
        int y = guiY + 120;
        
        // 标签
        Text label = Text.translatable("gui.chest-on-ghast.experience");
        context.drawText(this.textRenderer, label, x, y, 0x404040, false);
        
        // 进度条
        int barY = y + 12;
        if (level < 6) {
            float expRatio = (float)experience / expToNext;
            renderProgressBar(context, x, barY, 156, 14, expRatio, 0xFF00AA00, 0xFF00FF00);
            
            // 数值文本
            String expText = experience + " / " + expToNext;
            int textX = x + (156 - this.textRenderer.getWidth(expText)) / 2;
            context.drawText(this.textRenderer, expText, textX, barY + 3, 0xFFFFFF, true);
        } else {
            // 满级显示
            renderProgressBar(context, x, barY, 156, 14, 1.0f, 0xFF00AA00, 0xFF00FF00);
            
            String maxText = Text.translatable("gui.chest-on-ghast.max_level").getString();
            int textX = x + (156 - this.textRenderer.getWidth(maxText)) / 2;
            context.drawText(this.textRenderer, maxText, textX, barY + 3, 0xFFFFFF, true);
        }
    }
    
    /**
     * 渲染通用进度条
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
