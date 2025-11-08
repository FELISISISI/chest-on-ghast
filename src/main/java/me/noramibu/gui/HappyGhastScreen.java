package me.noramibu.gui;

import me.noramibu.Chestonghast;
import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 快乐恶魂GUI屏幕
 * 简化版本 - 用于调试文字渲染问题
 */
public class HappyGhastScreen extends Screen {
    private final int level;
    private final int experience;
    private final float hunger;
    private final float maxHealth;
    private final float currentHealth;
    private final float maxHunger;
    private final int expToNext;
    
    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private int guiX;
    private int guiY;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.literal("Happy Ghast GUI")); // 使用 literal 而不是 translatable
        this.level = payload.level();
        this.experience = payload.experience();
        this.hunger = payload.hunger();
        this.maxHealth = payload.maxHealth();
        this.currentHealth = payload.currentHealth();
        this.maxHunger = payload.maxHunger();
        this.expToNext = payload.expToNext();
        
        Chestonghast.LOGGER.info("=== HappyGhastScreen 构造函数被调用 ===");
        Chestonghast.LOGGER.info("Level: {}, Health: {}", level, currentHealth);
    }
    
    @Override
    protected void init() {
        super.init();
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
    
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
        Chestonghast.LOGGER.info("=== HappyGhastScreen init() 被调用 ===");
        Chestonghast.LOGGER.info("Screen width: {}, height: {}", this.width, this.height);
        Chestonghast.LOGGER.info("GUI X: {}, Y: {}", this.guiX, this.guiY);
        Chestonghast.LOGGER.info("textRenderer: {}", this.textRenderer);
        Chestonghast.LOGGER.info("client: {}", this.client);
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Chestonghast.LOGGER.info("=== render() 开始 ===");
        
        // 先渲染默认背景
        this.renderBackground(context, mouseX, mouseY, delta);
        
        // 检查关键对象
        if (this.textRenderer == null) {
            Chestonghast.LOGGER.error("!!! textRenderer is NULL !!!");
            return;
        }
        
        if (context == null) {
            Chestonghast.LOGGER.error("!!! DrawContext is NULL !!!");
            return;
        }
        
        Chestonghast.LOGGER.info("textRenderer: {}, context: {}", this.textRenderer, context);
        
        // 尝试最简单的文字渲染 - 在固定位置
        try {
            // 使用固定坐标，确保文字在屏幕上可见
            int testX = 100;
            int testY = 100;
            
            Chestonghast.LOGGER.info("尝试在 ({}, {}) 绘制文字", testX, testY);
            
            // 方法1: 使用 String
            context.drawText(this.textRenderer, "=== TEST 1 ===", testX, testY, 0xFFFFFF, true);
            Chestonghast.LOGGER.info("成功绘制测试文字 1");
            
            // 方法2: 使用 Text.literal
            context.drawText(this.textRenderer, Text.literal("=== TEST 2 ==="), testX, testY + 20, 0xFF0000, true);
            Chestonghast.LOGGER.info("成功绘制测试文字 2");
            
            // 方法3: 不同颜色
            context.drawText(this.textRenderer, "Level: " + level, testX, testY + 40, 0x00FF00, true);
            Chestonghast.LOGGER.info("成功绘制测试文字 3");
            
            context.drawText(this.textRenderer, "Health: " + currentHealth, testX, testY + 60, 0x0000FF, true);
            Chestonghast.LOGGER.info("成功绘制测试文字 4");
            
            // 在屏幕中央
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            String centerText = "CENTER TEXT";
            int textWidth = this.textRenderer.getWidth(centerText);
            context.drawText(this.textRenderer, centerText, centerX - textWidth / 2, centerY, 0xFFFF00, true);
            Chestonghast.LOGGER.info("成功绘制中央文字");
            
        } catch (Exception e) {
            Chestonghast.LOGGER.error("绘制文字时发生异常: ", e);
            e.printStackTrace();
        }
        
        Chestonghast.LOGGER.info("=== render() 结束 ===");
    }
    
    @Override
    public boolean shouldPause() {
        return false; // 游戏不暂停
    }
    
    @Override
    public void close() {
        Chestonghast.LOGGER.info("=== HappyGhastScreen 关闭 ===");
        super.close();
    }
}
