package chihalu.customstacklimit;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Properties;

/**
 * スタック数設定を管理し、通常アイテムへ設定されたスタック制限を適用します。
 */
public class StackLimitConfig {
    public static final int DEFAULT_STACK_LIMIT = 1000;
    public static final int MIN_STACK_LIMIT = 1;
    public static final int MAX_STACK_LIMIT = 1_000_000_000;
    public static final int WARNING_STACK_LIMIT = 32_767;

    private static final String CONFIG_FILE_NAME = "stackplus.properties";
    private static final String STACK_LIMIT_KEY = "stackLimit";
    private static final String DISPLAY_MODE_KEY = "displayMode";
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final ConfigValues LOADED_CONFIG = loadConfig();
    private static int stackLimit = LOADED_CONFIG.stackLimit();
    private static DisplayMode displayMode = LOADED_CONFIG.displayMode();

    public enum DisplayMode {
        COMPACT("compact"),
        PLUS_99("99plus");

        private final String serializedName;

        DisplayMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }

        public DisplayMode next() {
            return this == COMPACT ? PLUS_99 : COMPACT;
        }

        public static DisplayMode fromSerializedName(String value) {
            for (DisplayMode mode : values()) {
                if (mode.serializedName.equals(value)) {
                    return mode;
                }
            }
            return COMPACT;
        }
    }

    public static int getStackLimit() {
        return stackLimit;
    }

    public static void setStackLimit(int value) {
        stackLimit = clampStackLimit(value);
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
    }

    public static void saveSettings(int stackLimitValue, DisplayMode displayModeValue) {
        setStackLimit(stackLimitValue);
        setDisplayMode(displayModeValue);
        saveConfig();
    }

    public static int clampStackLimit(int value) {
        return Math.max(MIN_STACK_LIMIT, Math.min(MAX_STACK_LIMIT, value));
    }

    public static String formatStackLimit(int value) {
        return NUMBER_FORMAT.format(value);
    }

    public static int getAdjustedStackLimit(int originalLimit) {
        if (originalLimit <= 1) {
            return originalLimit;
        }

        return stackLimit;
    }

    public static int getAdjustedStackLimit(Item item, int originalLimit) {
        if (isForcedStackableItem(item)) {
            return stackLimit;
        }

        return getAdjustedStackLimit(originalLimit);
    }

    public static int getAdjustedStackLimit(ItemStack stack, int originalLimit) {
        if (stack.isDamageableItem()) {
            return originalLimit;
        }

        int adjustedLimit = getAdjustedStackLimit(stack.getItem(), originalLimit);
        return Math.max(adjustedLimit, stack.getCount());
    }

    private static void saveConfig() {
        Properties properties = new Properties();
        properties.setProperty(STACK_LIMIT_KEY, String.valueOf(stackLimit));
        properties.setProperty(DISPLAY_MODE_KEY, displayMode.getSerializedName());

        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, "StackPlus configuration");
            }
        } catch (IOException exception) {
            CustomStackLimit.LOGGER.warn("StackPlus 設定ファイルの保存に失敗しました: {}", configPath, exception);
        }
    }

    private static boolean isForcedStackableItem(Item item) {
        return item instanceof BedItem || isRequestedSingleStackItem(item);
    }

    private static boolean isRequestedSingleStackItem(Item item) {
        String path = getItemPath(item);

        return path.endsWith("_banner_pattern")
                || path.equals("enchanted_book")
                || path.equals("mushroom_stew")
                || path.equals("rabbit_stew")
                || path.equals("suspicious_stew")
                || path.equals("potion")
                || path.equals("splash_potion")
                || path.equals("lingering_potion")
                || path.equals("bucket")
                || path.endsWith("_bucket")
                || path.equals("minecart")
                || path.endsWith("_minecart")
                || path.equals("saddle")
                || path.equals("totem_of_undying")
                || path.endsWith("_boat")
                || path.endsWith("_chest_boat")
                || path.equals("bamboo_raft")
                || path.equals("bamboo_chest_raft")
                || path.equals("goat_horn")
                || path.startsWith("music_disc_")
                || path.endsWith("_harness");
    }

    private static String getItemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    private static ConfigValues loadConfig() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            return new ConfigValues(DEFAULT_STACK_LIMIT, DisplayMode.COMPACT);
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
            int loadedStackLimit = clampStackLimit(Integer.parseInt(properties.getProperty(STACK_LIMIT_KEY, String.valueOf(DEFAULT_STACK_LIMIT))));
            DisplayMode loadedDisplayMode = DisplayMode.fromSerializedName(properties.getProperty(DISPLAY_MODE_KEY, DisplayMode.COMPACT.getSerializedName()));
            return new ConfigValues(loadedStackLimit, loadedDisplayMode);
        } catch (IOException | NumberFormatException exception) {
            CustomStackLimit.LOGGER.warn("StackPlus 設定ファイルの読み込みに失敗しました。既定値を使用します: {}", configPath, exception);
            backupBrokenConfig(configPath);
            return new ConfigValues(DEFAULT_STACK_LIMIT, DisplayMode.COMPACT);
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    private static void backupBrokenConfig(Path configPath) {
        if (!Files.exists(configPath)) {
            return;
        }

        Path backupPath = configPath.resolveSibling(CONFIG_FILE_NAME + ".broken");
        if (Files.exists(backupPath)) {
            backupPath = configPath.resolveSibling(CONFIG_FILE_NAME + "." + System.currentTimeMillis() + ".broken");
        }

        try {
            Files.move(configPath, backupPath);
            CustomStackLimit.LOGGER.warn("StackPlus の壊れた設定ファイルを退避しました: {}", backupPath);
        } catch (IOException backupException) {
            CustomStackLimit.LOGGER.warn("StackPlus の壊れた設定ファイル退避に失敗しました: {}", configPath, backupException);
        }
    }

    private record ConfigValues(int stackLimit, DisplayMode displayMode) {
    }
}
