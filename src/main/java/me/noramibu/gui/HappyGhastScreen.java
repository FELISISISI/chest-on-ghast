package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * 快乐恶魂GUI屏幕
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
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.level = payload.level();
        this.experience = payload.experience();
        this.hunger = payload.hunger();
        this.maxHealth = payload.maxHealth();
        this.currentHealth = payload.currentHealth();
        this.maxHunger = payload.maxHunger();
        this.expToNext = payload.expToNext();
    }
    
    @Override
    protected void init() {
        super.init();
        this.guiX = (this.width - GUI_WIDTH) / 2;
        this.guiY = (this.height - GUI_HEIGHT) / 2;
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
        // 暗淡背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        // GUI背景面板
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFC6C6C6);
        
        // 边框
        context.fill(guiX, guiY, guiX + GUI_WIDTH, guiY + 1, 0xFF373737);
        context.fill(guiX, guiY + 1, guiX + 1, guiY + GUI_HEIGHT, 0xFF373737);
        context.fill(guiX, guiY + GUI_HEIGHT - 1, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        context.fill(guiX + GUI_WIDTH - 1, guiY, guiX + GUI_WIDTH, guiY + GUI_HEIGHT, 0xFFFFFFFF);
        context.fill(guiX + 1, guiY + 1, guiX + GUI_WIDTH - 1, guiY + 2, 0xFF8B8B8B);
        context.fill(guiX + 1, guiY + 1, guiX + 2, guiY + GUI_HEIGHT - 1, 0xFF8B8B8B);
        
        // 父类render
        super.render(context, mouseX, mouseY, delta);
        
        // === 文字和进度条（关键：在super.render()之后）===
        int x = guiX + 10;
        int y = guiY + 8;
        
        // 标题
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        int titleWidth = this.textRenderer.getWidth(title);
        context.drawText(this.textRenderer, title, guiX + (GUI_WIDTH - titleWidth) / 2, y, 0x404040, false);
        y += 20;
        
        // 等级
        context.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.level", level), x, y, 0x404040, false);
        y += 18;
        
        // 血量标签
        context.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.health"), x, y, 0x404040, false);
        y += 12;
        
        // 血量进度条
        int barWidth = 156;
        int barHeight = 14;
        float healthRatio = currentHealth / maxHealth;
        
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF000000);
        context.fill(x + 1, y + 1, x + barWidth - 1, y + barHeight - 1, 0xFF8B8B8B);
        context.fill(x + 2, y + 2, x + barWidth - 2, y + barHeight - 2, 0xFF555555);
        
        int healthBarWidth = (int)((barWidth - 4) * healthRatio);
        if (healthBarWidth > 0) {
            context.fill(x + 2, y + 2, x + 2 + healthBarWidth, y + barHeight - 2, 0xFFCC0000);
            context.fill(x + 2, y + 2, x + 2 + healthBarWidth, y + 4, 0xFFFF0000);
        }
        
        String healthText = String.format("%.1f / %.1f", currentHealth, maxHealth);
        context.drawText(this.textRenderer, healthText, x + (barWidth - this.textRenderer.getWidth(healthText)) / 2, y + 3, 0xFFFFFF, true);
        y += 22;
        
        // 饱食度标签
        context.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.hunger"), x, y, 0x404040, false);
        y += 12;
        
        // 饱食度进度条
        float hungerRatio = hunger / maxHunger;
        
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF000000);
        context.fill(x + 1, y + 1, x + barWidth - 1, y + barHeight - 1, 0xFF8B8B8B);
        context.fill(x + 2, y + 2, x + barWidth - 2, y + barHeight - 2, 0xFF555555);
        
        int hungerBarWidth = (int)((barWidth - 4) * hungerRatio);
        if (hungerBarWidth > 0) {
            context.fill(x + 2, y + 2, x + 2 + hungerBarWidth, y + barHeight - 2, 0xFFCC6600);
            context.fill(x + 2, y + 2, x + 2 + hungerBarWidth, y + 4, 0xFFFF8C00);
        }
        
        String hungerText = String.format("%.1f / %.1f", hunger, maxHunger);
        context.drawText(this.textRenderer, hungerText, x + (barWidth - this.textRenderer.getWidth(hungerText)) / 2, y + 3, 0xFFFFFF, true);
        y += 22;
        
        // 经验标签
        context.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.experience"), x, y, 0x404040, false);
        y += 12;
        
        // 经验进度条
        context.fill(x, y, x + barWidth, y + barHeight, 0xFF000000);
        context.fill(x + 1, y + 1, x + barWidth - 1, y + barHeight - 1, 0xFF8B8B8B);
        context.fill(x + 2, y + 2, x + barWidth - 2, y + barHeight - 2, 0xFF555555);
        
        if (level < 6) {
            float expRatio = (float) experience / expToNext;
            int expBarWidth = (int)((barWidth - 4) * expRatio);
            if (expBarWidth > 0) {
                context.fill(x + 2, y + 2, x + 2 + expBarWidth, y + barHeight - 2, 0xFF00AA00);
                context.fill(x + 2, y + 2, x + 2 + expBarWidth, y + 4, 0xFF00FF00);
            }
            String expText = experience + " / " + expToNext;
            context.drawText(this.textRenderer, expText, x + (barWidth - this.textRenderer.getWidth(expText)) / 2, y + 3, 0xFFFFFF, true);
        } else {
            context.fill(x + 2, y + 2, x + barWidth - 2, y + barHeight - 2, 0xFF00AA00);
            context.fill(x + 2, y + 2, x + barWidth - 2, y + 4, 0xFF00FF00);
            Text maxText = Text.translatable("gui.chest-on-ghast.max_level");
            context.drawText(this.textRenderer, maxText, x + (barWidth - this.textRenderer.getWidth(maxText)) / 2, y + 3, 0xFFFFFF, true);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}

