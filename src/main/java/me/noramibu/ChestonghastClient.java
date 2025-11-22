package me.noramibu;

import me.noramibu.gui.HappyGhastConfigScreen;
import me.noramibu.gui.HappyGhastScreen;
import me.noramibu.network.GreetGhastPayload;
import me.noramibu.network.RequestGhastConfigPayload;
import me.noramibu.network.SyncGhastConfigPayload;
import me.noramibu.network.SyncGhastDataPayload;
import me.noramibu.network.UpdateGhastConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化类
 * 负责注册按键绑定、处理客户端逻辑和GUI显示
 */
public class ChestonghastClient implements ClientModInitializer {
    // 定义H键绑定，用于与快乐恶魂互动
    private static KeyBinding greetGhastKey;

    /**
     * 客户端初始化方法
     * 在客户端启动时调用，注册按键绑定、事件监听器和网络包接收器
     */
    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(RequestGhastConfigPayload.ID, RequestGhastConfigPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(UpdateGhastConfigPayload.ID, UpdateGhastConfigPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGhastConfigPayload.ID, SyncGhastConfigPayload.CODEC);
        
        // 注册H键绑定
        // 创建一个简单的KeyBinding对象，使用MISC类别
        greetGhastKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.chest-on-ghast.greet", // 按键的翻译键
            InputUtil.Type.KEYSYM, // 按键类型
            GLFW.GLFW_KEY_H, // 默认绑定到H键
            KeyBinding.Category.MISC // 按键类别，使用MISC枚举
        ));

        // 注册客户端tick事件监听器
        // 每个游戏tick都会检查按键是否被按下
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 检查H键是否被按下
            while (greetGhastKey.wasPressed()) {
                // 发送网络包到服务端，告知服务端玩家按下了H键
                // 服务端会处理这个包并检测玩家是否在看着快乐恶魂
                ClientPlayNetworking.send(new GreetGhastPayload());
            }
        });
        
        // 注册客户端网络包接收器
        // 接收服务端发送的快乐恶魂数据
        ClientPlayNetworking.registerGlobalReceiver(
            SyncGhastDataPayload.ID,
            (payload, context) -> {
                // 在客户端主线程中执行，确保线程安全
                context.client().execute(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    
                    // 如果当前屏幕是HappyGhastScreen，更新数据
                    if (client.currentScreen instanceof HappyGhastScreen screen) {
                        screen.updateFromPayload(payload);
                    } else {
                        // 否则打开新的GUI
                        client.setScreen(new HappyGhastScreen(payload));
                    }
                });
            }
        );
        
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof OptionsScreen options) {
                Screens.getButtons(options).add(ButtonWidget.builder(Text.literal("快乐恶魂配置"), btn -> {
                    client.setScreen(new HappyGhastConfigScreen(screen));
                }).dimensions(options.width / 2 - 102, options.height / 6 + 180, 204, 20).build());
            }
        });
        
        ClientPlayNetworking.registerGlobalReceiver(
            SyncGhastConfigPayload.ID,
            (payload, context) -> context.client().execute(() -> HappyGhastConfigScreen.handleServerPayload(payload))
        );
    }
}
