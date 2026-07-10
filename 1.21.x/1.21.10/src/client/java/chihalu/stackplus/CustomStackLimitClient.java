package chihalu.stackplus;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomStackLimitClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(CustomStackLimit.MOD_ID);
	
	@Override
	public void onInitializeClient() {
		StackPlusUpdateNotifier.register();
		LOGGER.info("CustomStackLimit (クライアント専用) Mod が初期化されました！");
		LOGGER.info("CustomStackLimit: 通常アイテムのスタック数が{}個に設定されました", StackLimitConfig.getStackLimit());
	}
}
