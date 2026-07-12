package chihalu.stackplus.modmenu;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/** ワールド内で取得したクリエイティブ順をタイトル画面でも再利用します。 */
final class StackPlusCreativeOrderCache {
    private static final String FILE_NAME = "stackplus-creative-order.properties";
    private static final String ITEM_PREFIX = "item.";

    static List<String> load() {
        Path path = getPath();
        if (!Files.exists(path)) {
            return List.of();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            properties.load(input);
        } catch (IOException exception) {
            return List.of();
        }

        List<String> order = new ArrayList<>();
        for (int index = 0; ; index++) {
            String key = properties.getProperty(ITEM_PREFIX + index);
            if (key == null) {
                break;
            }
            order.add(key);
        }
        return order;
    }

    static void save(List<String> order) {
        Properties properties = new Properties();
        for (int index = 0; index < order.size(); index++) {
            properties.setProperty(ITEM_PREFIX + index, order.get(index));
        }
        Path path = getPath();
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream output = Files.newOutputStream(path)) {
                properties.store(output, "StackPlus creative order");
            }
        } catch (IOException ignored) {
            // 並び順キャッシュは補助機能のため、保存失敗時も設定画面は開けるようにします。
        }
    }

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private StackPlusCreativeOrderCache() {
    }
}


