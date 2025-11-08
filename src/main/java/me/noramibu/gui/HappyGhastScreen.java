package me.noramibu.gui;

import me.noramibu.Chestonghast;
import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 快乐恶魂GUI屏幕
 * 显示快乐恶魂的血量、饱食度、经验和等级信息
 * 使用Minecraft官方GUI样式，简洁美观
 */
public class HappyGhastScreen extends Screen {
    // 快乐恶魂数据
    private final int entityId;
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
        this.entityId = payload.entityId();
        this.level = payload.level();
        this.experience = payload.experience();
        this.hunger = payload.hunger();
        this.maxHealth = payload.maxHealth();
        this.currentHealth = payload.currentHealth();
        this.maxHunger = payload.maxHunger();
        this.expToNext = payload.expToNext();
        
        Chestonghast.LOGGER.info("HappyGhastScreen created with level: {}, health: {}/{}", level, currentHealth, maxHealth);
    }
    
    /**
     * 初始化GUI
     */
    @Override
    protected void init() {
        super.init();
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
        
        Chestonghast.LOGGER.info("HappyGhastScreen initialized at position: ({}, {}), screen size: {}x{}", 
            guiX, guiY, this.width, this.height);
        Chestonghast.LOGGER.info("TextRenderer: {}", this.textRenderer);
    }
    
    /**
     * 渲染GUI
     */
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Chestonghast.LOGGER.info("Render called - Drawing background and text");
        
        // 绘制暗淡背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        Chestonghast.LOGGER.info("Background drawn");
        
        // 绘制GUI背景面板
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFC6C6C6);
        Chestonghast.LOGGER.info("GUI panel drawn at ({}, {})", guiX, guiY);
        
        // 绘制边框
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + 1, 0xFF373737); // 顶部
        context.fill(guiX, guiY + 1, guiX + 1, guiY + GUI_HEIGHT, 0xFF373737); // 左侧
        context.fill(guiX, guiY + GUI_HEIGHT - 1, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF); // 底部
        context.fill(guiX + GUI_WIDTH - 1, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF); // 右侧
        
        // 尝试绘制文字 - 使用多种方法
        int textY = guiY + 10;
        
        // 方法1: 使用Text对象
        Text title = Text.literal("快乐恶魂");
        Chestonghast.LOGGER.info("Drawing title text: {}", title.getString());
        context.drawText(this.textRenderer, title, guiX + 10, textY, 0x404040, false);
        
        // 方法2: 使用String直接绘制
        textY += 15;
        String levelText = "等级: " + level;
        Chestonghast.LOGGER.info("Drawing level text: {}", levelText);
        context.drawText(this.textRenderer, levelText, guiX + 10, textY, 0xFF0000, false);
        
        // 方法3: 使用Text.literal
        textY += 15;
        Text healthText = Text.literal("血量: " + String.format("%.1f / %.1f", currentHealth, maxHealth));
        Chestonghast.LOGGER.info("Drawing health text: {}", healthText.getString());
        context.drawText(this.textRenderer, healthText, guiX + 10, textY, 0xFFFFFF, true);
        
        // 方法4: 绘制一些彩色方块作为参照
        context.fill(guiX + 150, guiY + 10, guiX + 160, guiY + 20, 0xFFFF0000); // 红色方块
        context.fill(guiX + 150, guiY + 25, guiX + 160, guiY + 35, 0xFF00FF00); // 绿色方块
        context.fill(guiX + 150, guiY + 40, guiX + 160, guiY + 50, 0xFF0000FF); // 蓝色方块
        
        Chestonghast.LOGGER.info("All rendering completed");
        
        // 调用父类render方法
        super.render(context, mouseX, mouseY, delta);
    }
    
    /**
     * 检查是否应该暂停游戏
     */
    @Override
    public boolean shouldPause() {
        return false;
    }
}
