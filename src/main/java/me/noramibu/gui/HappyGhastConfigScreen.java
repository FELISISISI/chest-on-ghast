package me.noramibu.gui;

import me.noramibu.Chestonghast;
import me.noramibu.network.RequestGhastConfigPayload;
import me.noramibu.network.SyncGhastConfigPayload;
import me.noramibu.network.UpdateGhastConfigPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HappyGhastConfigScreen extends Screen {
    private static final Text OFFLINE_MESSAGE = Text.literal("请先进入世界后再调整配置");
    private final Screen parent;
    private boolean dataRequested = false;
    private boolean dataLoaded = false;
    private boolean debugMode = false;

    private final List<LevelRow> levelRows = new ArrayList<>();
    private final Map<String, ElementRow> elementRows = new LinkedHashMap<>();
    private final List<TextFieldWidget> textFields = new ArrayList<>();

    private CyclingButtonWidget<Boolean> debugToggle;
    private ButtonWidget saveButton;

    public HappyGhastConfigScreen(Screen parent) {
        super(Text.literal("快乐恶魂配置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        textFields.clear();
        levelRows.clear();
        elementRows.clear();

        int buttonY = this.height - 40;

        this.debugToggle = this.addDrawableChild(CyclingButtonWidget.onOffBuilder(debugMode)
            .omitKeyText()
            .build(this.width / 2 - 100, 32, 200, 20, Text.literal("调试模式"), (button, value) -> debugMode = value));

        this.saveButton = this.addDrawableChild(ButtonWidget.builder(Text.literal("保存"), btn -> saveConfig())
            .dimensions(this.width / 2 - 102, buttonY, 100, 20).build());
        this.saveButton.active = false;
        this.addDrawableChild(ButtonWidget.builder(Text.literal("返回"), btn -> close())
            .dimensions(this.width / 2 + 2, buttonY, 100, 20).build());

        if (!dataRequested && hasPlayConnection()) {
            dataRequested = true;
            ClientPlayNetworking.send(new RequestGhastConfigPayload());
        } else if (!hasPlayConnection()) {
            // 记录一次离线场景，方便后续排查
            Chestonghast.LOGGER.debug("HappyGhastConfigScreen opened without active connection; skipping config sync request.");
        }

        rebuildFields();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        if (!hasPlayConnection()) {
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, OFFLINE_MESSAGE, this.width / 2, this.height / 2, 0xFF6666);
            return;
        }
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);

        if (!dataLoaded) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("正在请求服务器配置..."), this.width / 2, this.height / 2, 0xAAAAAA);
            return;
        }

        int startY = 60;
        for (LevelRow row : levelRows) {
            row.render(context, this.textRenderer, this.width / 2 - 160, startY);
            startY += 26;
        }

        startY += 8;
        for (ElementRow row : elementRows.values()) {
            row.render(context, this.textRenderer, this.width / 2 - 160, startY);
            startY += 26;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0xC0101010);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    public void applyServerConfig(SyncGhastConfigPayload payload) {
        this.dataLoaded = true;
        this.debugMode = payload.debugMode();
        this.levelRows.clear();
        this.elementRows.clear();

        for (SyncGhastConfigPayload.LevelEntry entry : payload.levels()) {
            this.levelRows.add(new LevelRow(entry));
        }

        for (SyncGhastConfigPayload.ElementEntry entry : payload.elements()) {
            this.elementRows.put(entry.id(), new ElementRow(entry));
        }

        if (this.debugToggle != null) {
            this.debugToggle.setValue(this.debugMode);
        }
        if (this.saveButton != null) {
            this.saveButton.active = true;
        }

        rebuildFields();
    }

    private void rebuildFields() {
        textFields.forEach(this::remove);
        textFields.clear();
        if (!dataLoaded || this.client == null || this.textRenderer == null) {
            return;
        }

        for (LevelRow row : levelRows) {
            row.initFields(this.client, this.textRenderer);
            this.addDrawableChild(row.powerField);
            this.addDrawableChild(row.cooldownField);
            this.addDrawableChild(row.damageField);
            textFields.add(row.powerField);
            textFields.add(row.cooldownField);
            textFields.add(row.damageField);
        }

        for (ElementRow row : elementRows.values()) {
            row.initFields(this.client, this.textRenderer);
            this.addDrawableChild(row.damageBonusField);
            this.addDrawableChild(row.effectBonusField);
            textFields.add(row.damageBonusField);
            textFields.add(row.effectBonusField);
        }
    }

    private void saveConfig() {
        if (!dataLoaded || !hasPlayConnection()) {
            // 没有连接或数据尚未同步时直接返回，避免错误发送
            return;
        }

        List<SyncGhastConfigPayload.LevelEntry> levelEntries = new ArrayList<>();
        for (LevelRow row : levelRows) {
            levelEntries.add(row.toSnapshot());
        }

        List<SyncGhastConfigPayload.ElementEntry> elementEntries = new ArrayList<>();
        for (ElementRow row : elementRows.values()) {
            elementEntries.add(row.toSnapshot());
        }

        ClientPlayNetworking.send(new UpdateGhastConfigPayload(levelEntries, elementEntries, debugMode));
    }

    /**
     * 判断客户端是否与世界保持连接，避免在主菜单中发送无效数据包.
     */
    private boolean hasPlayConnection() {
        return this.client != null && this.client.getNetworkHandler() != null;
    }

    private static class LevelRow {
        private final SyncGhastConfigPayload.LevelEntry snapshot;
        private TextFieldWidget powerField;
        private TextFieldWidget cooldownField;
        private TextFieldWidget damageField;

        LevelRow(SyncGhastConfigPayload.LevelEntry snapshot) {
            this.snapshot = snapshot;
        }

        void initFields(MinecraftClient client, net.minecraft.client.font.TextRenderer textRenderer) {
            powerField = new TextFieldWidget(textRenderer, 0, 0, 40, 18, Text.literal(""));
            powerField.setText(Integer.toString(snapshot.fireballPower()));
            cooldownField = new TextFieldWidget(textRenderer, 0, 0, 60, 18, Text.literal(""));
            cooldownField.setText(Integer.toString(snapshot.attackCooldownTicks()));
            damageField = new TextFieldWidget(textRenderer, 0, 0, 60, 18, Text.literal(""));
            damageField.setText(String.format(Locale.ROOT, "%.1f", snapshot.fireballDamage()));
        }

        void render(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int x, int y) {
            Text label = Text.literal("等级 " + snapshot.level());
            context.drawText(textRenderer, label, x, y + 5, 0xFFFFFF, false);

            powerField.setPosition(x + 60, y);
            cooldownField.setPosition(x + 60 + 50, y);
            damageField.setPosition(x + 60 + 120, y);
        }

        SyncGhastConfigPayload.LevelEntry toSnapshot() {
            int power = parseInt(powerField.getText(), snapshot.fireballPower(), 1, 20);
            int cooldown = parseInt(cooldownField.getText(), snapshot.attackCooldownTicks(), 1, 200);
            float damage = parseFloat(damageField.getText(), snapshot.fireballDamage(), 1.0f, 100.0f);
            return new SyncGhastConfigPayload.LevelEntry(snapshot.level(), power, cooldown, damage);
        }
    }

    private static class ElementRow {
        private final SyncGhastConfigPayload.ElementEntry snapshot;
        private TextFieldWidget damageBonusField;
        private TextFieldWidget effectBonusField;

        ElementRow(SyncGhastConfigPayload.ElementEntry snapshot) {
            this.snapshot = snapshot;
        }

        void initFields(MinecraftClient client, net.minecraft.client.font.TextRenderer textRenderer) {
            damageBonusField = new TextFieldWidget(textRenderer, 0, 0, 60, 18, Text.literal(""));
            damageBonusField.setText(String.format(Locale.ROOT, "%.2f", snapshot.sameBiomeDamageBonus()));
            effectBonusField = new TextFieldWidget(textRenderer, 0, 0, 60, 18, Text.literal(""));
            effectBonusField.setText(String.format(Locale.ROOT, "%.2f", snapshot.sameBiomeEffectBonus()));
        }

        void render(DrawContext context, net.minecraft.client.font.TextRenderer textRenderer, int x, int y) {
            Text label = Text.literal("属性 " + snapshot.id().toUpperCase(Locale.ROOT));
            context.drawText(textRenderer, label, x, y + 5, 0xFFFFFF, false);

            damageBonusField.setPosition(x + 80, y);
            effectBonusField.setPosition(x + 80 + 70, y);
        }

        SyncGhastConfigPayload.ElementEntry toSnapshot() {
            float dmg = parseFloat(damageBonusField.getText(), snapshot.sameBiomeDamageBonus(), 0.0f, 5.0f);
            float eff = parseFloat(effectBonusField.getText(), snapshot.sameBiomeEffectBonus(), 0.0f, 5.0f);
            return new SyncGhastConfigPayload.ElementEntry(snapshot.id(), dmg, eff);
        }
    }

    private static int parseInt(String text, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static float parseFloat(String text, float fallback, float min, float max) {
        try {
            float value = Float.parseFloat(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public static void handleServerPayload(SyncGhastConfigPayload payload) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof HappyGhastConfigScreen screen) {
            screen.applyServerConfig(payload);
        }
    }
}
