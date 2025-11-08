package me.noramibu;

import me.noramibu.network.GreetGhastPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端初始化类
 * 负责注册按键绑定和处理客户端逻辑
 */
public class ChestonghastClient implements ClientModInitializer {
    // 定义H键绑定，用于与快乐恶魂互动
    private static KeyBinding greetGhastKey;

    /**
     * 客户端初始化方法
     * 在客户端启动时调用，注册按键绑定和事件监听器
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
    }
}
