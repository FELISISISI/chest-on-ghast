package me.noramibu;

import me.noramibu.gui.HappyGhastScreen;
import me.noramibu.network.GreetGhastPayload;
import me.noramibu.network.SyncGhastDataPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
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
        // 接收服务端发送的快乐恶魂数据并打开GUI
        ClientPlayNetworking.registerGlobalReceiver(
            SyncGhastDataPayload.ID,
            (payload, context) -> {
                // 调试日志 - 确认收到了网络包
                Chestonghast.LOGGER.info("=== 收到 SyncGhastDataPayload ===");
                Chestonghast.LOGGER.info("Level: {}, Health: {}/{}", payload.level(), payload.currentHealth(), payload.maxHealth());
                
                // 在客户端主线程中执行，确保线程安全
                context.client().execute(() -> {
                    // 打开快乐恶魂GUI屏幕
                    HappyGhastScreen screen = new HappyGhastScreen(payload);
                    Chestonghast.LOGGER.info("创建了 HappyGhastScreen 实例: {}", screen);
                    
                    MinecraftClient.getInstance().setScreen(screen);
                    Chestonghast.LOGGER.info("调用了 setScreen，当前屏幕: {}", MinecraftClient.getInstance().currentScreen);
                });
            }
        );
    }
}
