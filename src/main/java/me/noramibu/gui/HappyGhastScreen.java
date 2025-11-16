package me.noramibu.gui;

import me.noramibu.enchant.GhastEnchantment;
import me.noramibu.enchant.GhastEnchantmentType;
import me.noramibu.network.RenameGhastPayload;
import me.noramibu.network.RequestGhastDataPayload;
import me.noramibu.network.SyncGhastDataPayload;
import me.noramibu.network.UpdateGhastEnchantmentsPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.List;
import java.util.ArrayList;

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
    private final List<GhastEnchantment> enchantments;
    private final List<ButtonWidget> enchantmentTypeButtons = new ArrayList<>();
    private final List<ButtonWidget> enchantmentLevelButtons = new ArrayList<>();
    
    private int tickCounter = 0;  // 用于控制请求频率
    private int enchantmentBaseY; // 附魔按钮区域的基准Y
    private TextFieldWidget nameField;  // 名字输入框
    private String customName;  // 自定义名字
    
    public HappyGhastScreen(SyncGhastDataPayload payload) {
        super(Text.translatable("gui.chest-on-ghast.happy_ghast"));
        this.entityId = payload.entityId();
        this.isCreative = payload.isCreative();
        this.favoriteFoods = payload.favoriteFoods();
        this.enchantments = new ArrayList<>();
        this.customName = "";
        updateFromPayload(payload);
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
        this.nameField.setText(this.customName.isEmpty() ? 
            Text.translatable("gui.chest-on-ghast.happy_ghast").getString() : this.customName);
        this.nameField.setPlaceholder(Text.translatable("gui.chest-on-ghast.name_placeholder"));
        
        // 当输入框内容改变时，存储临时名字
        this.nameField.setChangedListener(text -> {
            // 什么都不做，等待失去焦点时发送
        });
        
        this.addDrawableChild(this.nameField);
        
        // 计算附魔区域基准Y并构建按钮
        this.enchantmentBaseY = this.height / 2 + 40;
        buildEnchantmentControls(centerX);
        syncEnchantmentButtons();
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
        ingestEnchantments(payload.enchantments());
        
        // 更新自定义名字
        String newCustomName = payload.customName();
        if (newCustomName != null && !newCustomName.equals(this.customName)) {
            updateCustomName(newCustomName);
        }
    }
    
    /**
     * 更新自定义名字
     */
    public void updateCustomName(String name) {
        this.customName = name;
        if (this.nameField != null) {
            this.nameField.setText(name.isEmpty() ? 
                Text.translatable("gui.chest-on-ghast.happy_ghast").getString() : name);
        }
    }
    
    /**
     * 替换本地附魔数据并刷新按钮
     */
    private void ingestEnchantments(List<GhastEnchantment> remote) {
        this.enchantments.clear();
        if (remote != null) {
            this.enchantments.addAll(remote);
        }
        while (this.enchantments.size() < GhastEnchantment.MAX_SLOTS) {
            this.enchantments.add(GhastEnchantment.EMPTY);
        }
        syncEnchantmentButtons();
    }
    
    /**
     * 创建附魔类型与等级按钮
     */
    private void buildEnchantmentControls(int centerX) {
        this.enchantmentTypeButtons.clear();
        this.enchantmentLevelButtons.clear();
        
        int slotWidth = 90;
        int spacing = 12;
        int totalWidth = GhastEnchantment.MAX_SLOTS * slotWidth + (GhastEnchantment.MAX_SLOTS - 1) * spacing;
        int startX = centerX - totalWidth / 2;
        
        for (int slot = 0; slot < GhastEnchantment.MAX_SLOTS; slot++) {
            final int slotIndex = slot;
            int x = startX + slot * (slotWidth + spacing);
            
            ButtonWidget typeButton = ButtonWidget.builder(
                Text.literal(""),
                button -> cycleEnchantmentType(slotIndex)
            ).dimensions(x, this.enchantmentBaseY, slotWidth, 20).build();
            typeButton.setTooltip(Tooltip.of(Text.empty()));
            this.enchantmentTypeButtons.add(this.addDrawableChild(typeButton));
            
            ButtonWidget levelButton = ButtonWidget.builder(
                Text.literal(""),
                button -> cycleEnchantmentLevel(slotIndex)
            ).dimensions(x, this.enchantmentBaseY + 24, slotWidth, 20).build();
            levelButton.setTooltip(Tooltip.of(Text.empty()));
            this.enchantmentLevelButtons.add(this.addDrawableChild(levelButton));
        }
    }
    
    /**
     * 根据当前附魔刷新按钮的文字、可用状态和提示
     */
    private void syncEnchantmentButtons() {
        for (int slot = 0; slot < this.enchantmentTypeButtons.size(); slot++) {
            GhastEnchantment enchantment = getEnchantment(slot);
            
            ButtonWidget typeButton = this.enchantmentTypeButtons.get(slot);
            if (typeButton != null) {
                typeButton.setMessage(buildTypeLabel(slot, enchantment));
                typeButton.setTooltip(Tooltip.of(buildTypeTooltip(enchantment)));
            }
            
            if (slot < this.enchantmentLevelButtons.size()) {
                ButtonWidget levelButton = this.enchantmentLevelButtons.get(slot);
                if (levelButton != null) {
                    levelButton.setMessage(buildLevelLabel(enchantment));
                    levelButton.active = !enchantment.isEmpty();
                    levelButton.setTooltip(Tooltip.of(buildLevelTooltip(enchantment)));
                }
            }
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
                String newName = this.nameField.getText();
                if (newName != null && !newName.isEmpty() && !newName.equals(this.customName)) {
                    ClientPlayNetworking.send(new RenameGhastPayload(this.entityId, newName));
                    this.customName = newName;
                }
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
    
    /**
     * 获取指定槽位的附魔
     */
    private GhastEnchantment getEnchantment(int slot) {
        if (slot < 0 || slot >= this.enchantments.size()) {
            return GhastEnchantment.EMPTY;
        }
        return this.enchantments.get(slot);
    }
    
    /**
     * 构建附魔类型按钮的标签
     */
    private Text buildTypeLabel(int slot, GhastEnchantment enchantment) {
        Text typeText = enchantment.isEmpty()
            ? Text.translatable("gui.chest-on-ghast.enchantment.none")
            : enchantment.type().getDisplayText();
        return Text.translatable("gui.chest-on-ghast.enchantment_slot_label", slot + 1, typeText);
    }
    
    /**
     * 构建附魔等级按钮的标签
     */
    private Text buildLevelLabel(GhastEnchantment enchantment) {
        if (enchantment.isEmpty()) {
            return Text.translatable("gui.chest-on-ghast.enchantment_level_empty");
        }
        return Text.translatable("gui.chest-on-ghast.enchantment_level", romanNumeral(enchantment.level()));
    }
    
    /**
     * 构建类型提示文本
     */
    private Text buildTypeTooltip(GhastEnchantment enchantment) {
        GhastEnchantmentType type = enchantment.type();
        if (type == GhastEnchantmentType.NONE) {
            return Text.translatable("gui.chest-on-ghast.enchantment.none_desc");
        }
        return Text.translatable(
            "gui.chest-on-ghast.enchantment.tooltip",
            type.getDisplayText(),
            type.getRequiredLevel(),
            type.getDescriptionText()
        );
    }
    
    /**
     * 构建等级提示文本
     */
    private Text buildLevelTooltip(GhastEnchantment enchantment) {
        if (enchantment.isEmpty()) {
            return Text.translatable("gui.chest-on-ghast.enchantment.level_hint_empty");
        }
        return Text.translatable("gui.chest-on-ghast.enchantment.level_hint");
    }
    
    /**
     * 循环切换附魔类型（根据当前等级过滤）
     */
    private void cycleEnchantmentType(int slot) {
        GhastEnchantment current = getEnchantment(slot);
        GhastEnchantmentType nextType = current.type().next();
        
        int attempts = 0;
        while (attempts < GhastEnchantmentType.SELECTABLE_TYPES.size()) {
            if (nextType == GhastEnchantmentType.NONE || this.level >= nextType.getRequiredLevel()) {
                break;
            }
            nextType = nextType.next();
            attempts++;
        }
        
        int nextLevel = nextType == GhastEnchantmentType.NONE
            ? 0
            : Math.max(1, Math.min(current.level(), nextType.getMaxLevel()));
        
        sendEnchantmentUpdate(slot, new GhastEnchantment(nextType, nextLevel));
    }
    
    /**
     * 循环切换附魔等级
     */
    private void cycleEnchantmentLevel(int slot) {
        GhastEnchantment current = getEnchantment(slot);
        if (current.isEmpty()) {
            return;
        }
        
        int nextLevel = current.level() + 1;
        if (nextLevel > current.type().getMaxLevel()) {
            nextLevel = 1;
        }
        sendEnchantmentUpdate(slot, new GhastEnchantment(current.type(), nextLevel));
    }
    
    /**
     * 本地更新并向服务端发送附魔改动
     */
    private void sendEnchantmentUpdate(int slot, GhastEnchantment enchantment) {
        if (slot < 0 || slot >= this.enchantments.size()) {
            return;
        }
        
        this.enchantments.set(slot, enchantment);
        syncEnchantmentButtons();
        
        ClientPlayNetworking.send(
            new UpdateGhastEnchantmentsPayload(
                this.entityId,
                slot,
                enchantment.type().getId().toString(),
                enchantment.level()
            )
        );
    }
    
    /**
     * 将数字转换为罗马数字
     */
    private String romanNumeral(int value) {
        return switch (Math.max(0, value)) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            default -> value <= 0 ? "-" : String.valueOf(value);
        };
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

        // 附魔标题
        int enchantHeaderY = (this.enchantmentBaseY == 0 ? dataY + 90 : this.enchantmentBaseY - 18);
        Text enchantHeader = Text.translatable("gui.chest-on-ghast.enchantments");
        int enchantHeaderWidth = this.textRenderer.getWidth(enchantHeader);
        context.drawText(this.textRenderer, enchantHeader, centerX - enchantHeaderWidth / 2, enchantHeaderY, 0xFF55FFFF, false);

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
