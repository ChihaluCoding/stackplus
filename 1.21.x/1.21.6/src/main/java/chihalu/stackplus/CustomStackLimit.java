package chihalu.stackplus;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomStackLimit implements ModInitializer {
	public static final String MOD_ID = "stackplus";

	// このロガーはコンソールとログファイルにテキストを書き込むために使用されます。
	// ロガーの名前にMod IDを使用することがベストプラクティスとされています。
	// こうすることで、どのModが情報、警告、エラーを出力したかが明確になります。
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// このコードはMinecraftがMod読み込み準備完了状態になると直ちに実行されます。
		// ただし、一部のもの（リソースなど）はまだ初期化されていない可能性があります。
		// 注意深く進めてください。

		LOGGER.info("CustomStackLimit Mod が初期化されました！");
		LOGGER.info("CustomStackLimit: 通常アイテムのスタック数が{}個に設定されました", StackLimitConfig.getStackLimit());
	}
}
