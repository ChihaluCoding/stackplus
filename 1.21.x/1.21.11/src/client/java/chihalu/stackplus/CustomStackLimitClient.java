package chihalu.stackplus;

import net.fabricmc.api.ClientModInitializer;

public class CustomStackLimitClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		StackPlusUpdateNotifier.register();
		StackPlusIssueReportCommand.register();
	}
}
