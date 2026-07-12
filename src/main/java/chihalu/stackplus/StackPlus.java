package chihalu.stackplus;

import chihalu.stackplus.network.StackRulesNetworking;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StackPlus implements ModInitializer {
	public static final String MOD_ID = "stackplus";

	// このロガーはコンソールとログファイルにテキストを書き込むために使用されます。
	// ロガーの名前にMod IDを使用することがベストプラクティスとされています。
	// こうすることで、どのModが情報、警告、エラーを出力したかが明確になります。
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		StackRulesNetworking.registerServer();
	}
}
