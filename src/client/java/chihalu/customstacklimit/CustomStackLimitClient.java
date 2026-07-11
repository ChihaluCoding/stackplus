package chihalu.customstacklimit;

import chihalu.customstacklimit.network.StackRulesClientNetworking;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomStackLimitClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(CustomStackLimit.MOD_ID);
	
	@Override
	public void onInitializeClient() {
		StackRulesClientNetworking.register();
		StackPlusIssueReportCommand.register();
		LOGGER.info("StackPlus クライアントが初期化されました");
		LOGGER.info("StackPlus: 通常アイテムのスタック数が{}個に設定されました", StackLimitConfig.getStackLimit());
	}
}
