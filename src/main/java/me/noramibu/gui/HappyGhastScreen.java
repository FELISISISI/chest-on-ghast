package me.noramibu.gui;

import me.noramibu.network.SyncGhastDataPayload;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
    private int x, y;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.literal("Happy Ghast"));
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
        this.x = (this.width - GUI_WIDTH) / 2;
        this.y = (this.height - GUI_HEIGHT) / 2;
    }
    
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // 背景
        ctx.fill(0, 0, this.width, this.height, 0xC0101010);
        ctx.fill(x, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFC6C6C6);
        
        // 边框
        ctx.fill(x, y, x + GUI_WIDTH, y + 1, 0xFF373737);
        ctx.fill(x, y + 1, x + 1, y + GUI_HEIGHT, 0xFF373737);
        ctx.fill(x, y + GUI_HEIGHT - 1, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFFFFFFF);
        ctx.fill(x + GUI_WIDTH - 1, y, x + GUI_WIDTH, y + GUI_HEIGHT, 0xFFFFFFFF);
        
        super.render(ctx, mouseX, mouseY, delta);
        
        // 文字内容
        int tx = x + 10;
        int ty = y + 10;
        
        // 标题
        Text title = Text.translatable("gui.chest-on-ghast.happy_ghast");
        ctx.drawText(this.textRenderer, title, x + (GUI_WIDTH - this.textRenderer.getWidth(title)) / 2, ty, 0x404040, false);
        ty += 18;
        
        // 等级
        ctx.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.level", level), tx, ty, 0x404040, false);
        ty += 18;
        
        // 血量
        ctx.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.health"), tx, ty, 0x404040, false);
        ty += 10;
        drawBar(ctx, tx, ty, 156, 14, currentHealth / maxHealth, 0xFFCC0000, 0xFFFF0000);
        ctx.drawText(this.textRenderer, String.format("%.1f / %.1f", currentHealth, maxHealth), 
            tx + (156 - this.textRenderer.getWidth(String.format("%.1f / %.1f", currentHealth, maxHealth))) / 2, ty + 3, 0xFFFFFF, true);
        ty += 20;
        
        // 饱食度
        ctx.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.hunger"), tx, ty, 0x404040, false);
        ty += 10;
        drawBar(ctx, tx, ty, 156, 14, hunger / maxHunger, 0xFFCC6600, 0xFFFF8C00);
        ctx.drawText(this.textRenderer, String.format("%.1f / %.1f", hunger, maxHunger),
            tx + (156 - this.textRenderer.getWidth(String.format("%.1f / %.1f", hunger, maxHunger))) / 2, ty + 3, 0xFFFFFF, true);
        ty += 20;
        
        // 经验
        ctx.drawText(this.textRenderer, Text.translatable("gui.chest-on-ghast.experience"), tx, ty, 0x404040, false);
        ty += 10;
        if (level < 6) {
            drawBar(ctx, tx, ty, 156, 14, (float)experience / expToNext, 0xFF00AA00, 0xFF00FF00);
            String exp = experience + " / " + expToNext;
            ctx.drawText(this.textRenderer, exp, tx + (156 - this.textRenderer.getWidth(exp)) / 2, ty + 3, 0xFFFFFF, true);
        } else {
            drawBar(ctx, tx, ty, 156, 14, 1.0f, 0xFF00AA00, 0xFF00FF00);
            Text max = Text.translatable("gui.chest-on-ghast.max_level");
            ctx.drawText(this.textRenderer, max, tx + (156 - this.textRenderer.getWidth(max)) / 2, ty + 3, 0xFFFFFF, true);
        }
    }
    
    private void drawBar(DrawContext ctx, int x, int y, int w, int h, float ratio, int dark, int light) {
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF8B8B8B);
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF555555);
        int bw = (int)((w - 4) * Math.max(0, Math.min(1, ratio)));
        if (bw > 0) {
            ctx.fill(x + 2, y + 2, x + 2 + bw, y + h - 2, dark);
            ctx.fill(x + 2, y + 2, x + 2 + bw, y + 4, light);
        }
    }
    
    @Override
    public boolean shouldPause() {
        return false;
    }
}
