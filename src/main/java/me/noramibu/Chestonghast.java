package me.noramibu;

import me.noramibu.config.GhastConfig;
import me.noramibu.item.ModItems;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 模组主类
 * 负责初始化服务端逻辑和网络处理器
 */
public class Chestonghast implements ModInitializer {
	// 模组ID，用于标识这个模组
	public static final String MOD_ID = "chest-on-ghast";
	
	// 日志记录器，用于输出调试和运行信息
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/**
	 * 模组初始化方法
	 * 在服务端启动时调用，注册网络包处理器
	 */
	@Override
	public void onInitialize() {
		// 加载配置文件
		GhastConfig.getInstance();
		
		// 注册模组物品
		ModItems.registerModItems();
		
		// 注册服务端网络包接收器
		// 用于处理客户端发送的按键事件
		NetworkHandler.registerServerReceivers();
		
		LOGGER.info("Chest on Ghast mod initialized!");
	}
} 