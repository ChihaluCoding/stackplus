package chihalu.stackplus;

import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

/** 設定ファイル内の有効なルールIDの読込をまとめます。 */
final class StackRuleConfigSupport {
    static Set<String> loadEnabledRuleIds(Properties properties, String keyPrefix) {
        Set<String> loadedRuleIds = new LinkedHashSet<>();
        for (String propertyName : properties.stringPropertyNames()) {
            if (!propertyName.startsWith(keyPrefix)) {
                continue;
            }
            String id = propertyName.substring(keyPrefix.length());
            if (!id.isBlank() && Boolean.parseBoolean(properties.getProperty(propertyName))) {
                loadedRuleIds.add(id);
            }
        }
        return loadedRuleIds;
    }

    private StackRuleConfigSupport() {
    }
}


