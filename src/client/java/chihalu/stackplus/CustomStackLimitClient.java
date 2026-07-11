package chihalu.stackplus;

import chihalu.stackplus.network.StackRulesClientNetworking;

import net.fabricmc.api.ClientModInitializer;

public class CustomStackLimitClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StackRulesClientNetworking.register();
		StackPlusUpdateNotifier.register();
		StackPlusIssueReportCommand.register();
	}
}
