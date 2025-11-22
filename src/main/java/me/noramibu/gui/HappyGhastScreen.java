package me.noramibu.gui;

import me.noramibu.network.RenameGhastPayload;
import me.noramibu.network.RequestGhastDataPayload;
import me.noramibu.network.SyncGhastDataPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.List;
import java.util.Locale;

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
    
    private int tickCounter = 0;  // 用于控制请求频率
    private TextFieldWidget nameField;  // 名字输入框
    private String customName;  // 自定义名字
    private String elementId;
    private float elementDamage;
    private int elementCooldownTicks;
    private float elementControlStrength;
    private int elementExplosionPower;
    private boolean elementHomeBonus;
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.entityId = payload.entityId();
        this.isCreative = payload.isCreative();
        this.favoriteFoods = payload.favoriteFoods();
        this.customName = payload.customName() != null ? payload.customName() : "";
        this.elementId = payload.elementId();
        this.elementDamage = payload.elementDamage();
        this.elementCooldownTicks = payload.elementCooldownTicks();
        this.elementControlStrength = payload.elementControlStrength();
        this.elementExplosionPower = payload.elementExplosionPower();
        this.elementHomeBonus = payload.elementHomeBoost();
        updateFromPayload(payload);
    }

    private void renderElementSection(DrawContext context, int centerX, int startY) {
        Text elementName = getElementDisplayName();
        String statsLine = String.format(Locale.ROOT, "伤害 %.1f | 冷却 %.2fs | 控制 %.2f", this.elementDamage, this.elementCooldownTicks / 20.0f, this.elementControlStrength);
        String miscLine = String.format(Locale.ROOT, "爆炸/推力 %d%s", this.elementExplosionPower, this.elementHomeBonus ? " · 地形加成" : "");
        
        int elementWidth = this.textRenderer.getWidth(elementName);
        context.drawText(this.textRenderer, elementName, centerX - elementWidth / 2, startY, 0xFFFFAA00, false);
        
        int statsWidth = this.textRenderer.getWidth(statsLine);
        context.drawText(this.textRenderer, statsLine, centerX - statsWidth / 2, startY + 12, 0xFFFFFFFF, false);
        
        int miscWidth = this.textRenderer.getWidth(miscLine);
        context.drawText(this.textRenderer, miscLine, centerX - miscWidth / 2, startY + 24, this.elementHomeBonus ? 0xFF00FF7F : 0xFFBBBBBB, false);
    }
    
    private Text getElementDisplayName() {
        String id = this.elementId == null ? "fire" : this.elementId.toLowerCase(Locale.ROOT);
        return switch (id) {
            case "ice" -> Text.literal("属性：冰");
            case "wind" -> Text.literal("属性：风");
            case "sand" -> Text.literal("属性：炽沙");
            default -> Text.literal("属性：火");
        };
    }
    
    /**
     * 初始化GUI组件
     */
    @Override
    protected void init() {
        super.init();
        
        // 创建名字输入框
        int centerX = this.width / 2;
        int topY = 18;
        int fieldWidth = 150;
        
        this.nameField = new TextFieldWidget(
            this.textRenderer,
            centerX - fieldWidth / 2,
            topY,
            fieldWidth,
            16,
            Text.literal("")
        );
        
        // 设置输入框属性
        this.nameField.setMaxLength(32);
        this.nameField.setPlaceholder(Text.translatable("gui.chest-on-ghast.name_placeholder"));
        this.nameField.setText(this.customName);
        
        // 当输入框内容改变时，存储临时名字
        this.nameField.setChangedListener(text -> {
            // 什么都不做，等待失去焦点时发送
        });
        
        this.addDrawableChild(this.nameField);
        
        var configButton = net.minecraft.client.gui.widget.ButtonWidget.builder(
            Text.literal("调整配置"),
            btn -> openConfigScreen()
        ).dimensions(centerX - fieldWidth / 2, topY + 24, fieldWidth, 20).build();
        this.addDrawableChild(configButton);
    }
    
    /**
     * 从payload更新数据
     */
    public void updateFromPayload(SyncGhastDataPayload payload) {
        this.level = payload.level();
        this.currentHealth = payload.currentHealth();
        this.maxHealth = payload.maxHealth();
        this.hunger = payload.hunger();
        this.maxHunger = payload.maxHunger();
        this.experience = payload.experience();
        this.expToNext = payload.expToNext();
        
        // 更新自定义名字
        String newCustomName = payload.customName();
        if (newCustomName == null) {
            newCustomName = "";
        }
        if (!newCustomName.equals(this.customName)) {
            updateCustomName(newCustomName);
        }
        this.elementId = payload.elementId();
        this.elementDamage = payload.elementDamage();
        this.elementCooldownTicks = payload.elementCooldownTicks();
        this.elementControlStrength = payload.elementControlStrength();
        this.elementExplosionPower = payload.elementExplosionPower();
        this.elementHomeBonus = payload.elementHomeBoost();
    }
    
    /**
     * 更新自定义名字
     */
    public void updateCustomName(String name) {
        this.customName = name == null ? "" : name;
        if (this.nameField != null) {
            this.nameField.setText(this.customName);
        }
    }
    
    /**
     * 处理失去焦点时的改名
     */
    private boolean wasFocused = false;
    
    /**
     * 每tick更新，定期向服务器请求最新数据
     */
    @Override
    public void tick() {
        super.tick();
        
        // 检测焦点状态变化，失去焦点时发送改名请求
        if (this.nameField != null) {
            boolean currentlyFocused = this.nameField.isFocused();
                if (wasFocused && !currentlyFocused) {
                    // 刚刚失去焦点
                    trySendRename();
            }
            wasFocused = currentlyFocused;
        }
        
        tickCounter++;
        // 每10 ticks（0.5秒）向服务器请求一次最新数据
        if (tickCounter >= 10) {
            ClientPlayNetworking.send(new RequestGhastDataPayload(this.entityId));
            tickCounter = 0;
        }
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, 0xC0101010);
        
        super.render(context, mouseX, mouseY, delta);
        
        // 计算中心位置
        int centerX = this.width / 2;
        int topY = 20;
        
        // 渲染名字输入框
        if (this.nameField != null) {
            this.nameField.render(context, mouseX, mouseY, delta);
        }
        
        // 等级居中（向下移动一点，给名字输入框腾出空间）
        Text levelText = Text.translatable("gui.chest-on-ghast.level", level);
        int levelWidth = this.textRenderer.getWidth(levelText);
        context.drawText(this.textRenderer, levelText, centerX - levelWidth / 2, topY + 22, 0xFFFFD700, false);
        
        // 三个数据块横向排列（向下移动）
        int dataY = topY + 42;
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
        
        int elementSectionY = dataY + 70;
        renderElementSection(context, centerX, elementSectionY);
        
        // 底部提示（居中）
        int hintY = elementSectionY + 60;
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
    
    private void openConfigScreen() {
        if (this.client != null) {
            this.client.setScreen(new HappyGhastConfigScreen(this));
        }
    }
    
    @Override
    public void close() {
        trySendRename();
        super.close();
    }
    
    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            trySendRename();
            return true;
        }
        return super.keyPressed(keyInput);
    }
    
    private void trySendRename() {
        if (this.nameField == null) return;
        String newName = this.nameField.getText();
        if (newName == null) return;
        newName = newName.trim();
        if (newName.isEmpty() || newName.equals(this.customName)) {
            return;
        }
        ClientPlayNetworking.send(new RenameGhastPayload(this.entityId, newName));
        this.customName = newName;
    }
}
