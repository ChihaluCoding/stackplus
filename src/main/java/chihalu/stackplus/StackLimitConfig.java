package chihalu.stackplus;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MinecartItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * スタック数設定を管理し、通常アイテムへ設定されたスタック制限を適用します。
 */
public class StackLimitConfig {
    public static final int DEFAULT_STACK_LIMIT = 1000;
    public static final int MIN_STACK_LIMIT = 1;
    public static final int MAX_STACK_LIMIT = 1_000_000_000;
    public static final int MAX_CUSTOM_STACK_LIMIT_PRESETS = 4;
    private static final String CONFIG_FILE_NAME = "stackplus.properties";
    private static final String STACK_LIMIT_KEY = "stackLimit";
    private static final String DISPLAY_MODE_KEY = "displayMode";
    private static final String SELECTED_ITEM_COUNT_MODE_KEY = "selectedItemCountMode";
    private static final String SELECTED_ITEM_COUNT_POSITION_KEY = "selectedItemCountPosition";
    private static final String SELECTED_ITEM_COUNT_ALWAYS_VISIBLE_KEY = "selectedItemCountAlwaysVisible";
    private static final String UPDATE_NOTIFICATIONS_ENABLED_KEY = "updateNotificationsEnabled";
    private static final String STACK_LIMIT_PRESETS_VISIBLE_KEY = "stackLimitPresetsVisible";
    private static final String CUSTOM_STACK_LIMIT_PRESETS_KEY = "customStackLimitPresets";
    private static final String LAST_NOTIFIED_RELEASE_KEY_PREFIX = "lastNotifiedReleaseVersion.";
    private static final String ITEM_LIMIT_KEY_PREFIX = "item.";
    private static final String STACK_LIMIT_KEY_PREFIX = "stack.";
    private static final String FORCED_ITEM_KEY_PREFIX = "force.";
    private static final String FORBIDDEN_ITEM_KEY_PREFIX = "forbid.item.";
    private static final String SELECTED_ITEM_COUNT_COLOR_KEY = "selectedItemCountColor";
    private static final String DURABILITY_WARNING_SUPPRESSED_KEY = "durabilityWarningSuppressed";
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final ConfigValues LOADED_CONFIG = loadConfig();
    private static int stackLimit = LOADED_CONFIG.stackLimit();
    private static DisplayMode displayMode = LOADED_CONFIG.displayMode();
    private static SelectedItemCountMode selectedItemCountMode = LOADED_CONFIG.selectedItemCountMode();
    private static SelectedItemCountPosition selectedItemCountPosition = LOADED_CONFIG.selectedItemCountPosition();
    private static int selectedItemCountColorRgb = LOADED_CONFIG.selectedItemCountColorRgb();
    private static boolean durabilityWarningSuppressed = LOADED_CONFIG.durabilityWarningSuppressed();
    private static boolean updateNotificationsEnabled = LOADED_CONFIG.updateNotificationsEnabled();
    private static boolean stackLimitPresetsVisible = LOADED_CONFIG.stackLimitPresetsVisible();
    private static final List<Integer> customStackLimitPresets = new ArrayList<>(LOADED_CONFIG.customStackLimitPresets());
    private static final Map<String, Integer> itemStackLimits = new LinkedHashMap<>(LOADED_CONFIG.itemStackLimits());
    private static final Map<String, Integer> stackVariantLimits = new LinkedHashMap<>(LOADED_CONFIG.stackVariantLimits());
    private static final Set<String> forcedStackableItems = new LinkedHashSet<>(LOADED_CONFIG.forcedStackableItems());
    private static final Set<String> forbiddenStackableItems = new LinkedHashSet<>(LOADED_CONFIG.forbiddenStackableItems());
    private static final Map<String, String> lastNotifiedReleaseVersions = new LinkedHashMap<>(LOADED_CONFIG.lastNotifiedReleaseVersions());
    private static volatile boolean serverRulesActive;
    private static volatile boolean remoteSession;
    private static String localStackRules;

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

    public enum SelectedItemCountMode {
        OFF("off"),
        ON_SWITCH("onSwitch"),
        ALWAYS("always");

        private final String serializedName;

        SelectedItemCountMode(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }

        public SelectedItemCountMode next() {
            return switch (this) {
                case OFF -> ON_SWITCH;
                case ON_SWITCH -> ALWAYS;
                case ALWAYS -> OFF;
            };
        }

        public boolean appendsCountToItemName() {
            return this != OFF;
        }

        public boolean keepsVisible() {
            return this == ALWAYS;
        }

        public static SelectedItemCountMode fromSerializedName(String value) {
            for (SelectedItemCountMode mode : values()) {
                if (mode.serializedName.equals(value)) {
                    return mode;
                }
            }
            return ON_SWITCH;
        }
    }

    public enum SelectedItemCountPosition {
        BESIDE("beside"),
        BELOW("below");

        private final String serializedName;

        SelectedItemCountPosition(String serializedName) {
            this.serializedName = serializedName;
        }

        public String getSerializedName() {
            return serializedName;
        }

        public SelectedItemCountPosition next() {
            return this == BESIDE ? BELOW : BESIDE;
        }

        public boolean isBelow() {
            return this == BELOW;
        }

        public static SelectedItemCountPosition fromSerializedName(String value) {
            for (SelectedItemCountPosition position : values()) {
                if (position.serializedName.equals(value)) {
                    return position;
                }
            }
            return BESIDE;
        }
    }

    public static int getStackLimit() {
        return stackLimit;
    }

    public static synchronized String exportStackRules() {
        Properties properties = new Properties();
        properties.setProperty(STACK_LIMIT_KEY, String.valueOf(stackLimit));
        itemStackLimits.forEach((itemId, itemLimit) -> properties.setProperty(ITEM_LIMIT_KEY_PREFIX + itemId, String.valueOf(itemLimit)));
        stackVariantLimits.forEach((key, limit) -> properties.setProperty(STACK_LIMIT_KEY_PREFIX + key, String.valueOf(limit)));
        forcedStackableItems.forEach(itemId -> properties.setProperty(FORCED_ITEM_KEY_PREFIX + itemId, "true"));
        forbiddenStackableItems.forEach(itemId -> properties.setProperty(FORBIDDEN_ITEM_KEY_PREFIX + itemId, "true"));
        StringWriter writer = new StringWriter();
        try {
            properties.store(writer, null);
        } catch (IOException exception) {
            throw new IllegalStateException("StackPlus rules could not be serialized", exception);
        }
        return writer.toString();
    }

    public static synchronized void applyServerStackRules(String serializedRules) {
        if (!serverRulesActive) {
            localStackRules = exportStackRules();
        }
        applyStackRules(serializedRules);
        serverRulesActive = true;
    }

    public static synchronized void restoreLocalStackRules() {
        if (!serverRulesActive) {
            return;
        }
        applyStackRules(localStackRules);
        localStackRules = null;
        serverRulesActive = false;
    }

    public static boolean areServerRulesActive() {
        return serverRulesActive;
    }

    public static void beginRemoteSession() {
        remoteSession = true;
    }

    public static synchronized void endRemoteSession() {
        remoteSession = false;
        restoreLocalStackRules();
    }

    public static boolean areStackRulesEnabled() {
        return !remoteSession || serverRulesActive;
    }

    private static void applyStackRules(String serializedRules) {
        Properties properties = readStackRules(serializedRules);
        try {
            stackLimit = clampStackLimit(Integer.parseInt(properties.getProperty(STACK_LIMIT_KEY, String.valueOf(DEFAULT_STACK_LIMIT))));
            itemStackLimits.clear();
            itemStackLimits.putAll(loadItemStackLimits(properties));
            stackVariantLimits.clear();
            stackVariantLimits.putAll(loadStackVariantLimits(properties));
            forcedStackableItems.clear();
            forcedStackableItems.addAll(loadForcedStackableItems(properties));
            forbiddenStackableItems.clear();
            forbiddenStackableItems.addAll(StackRuleConfigSupport.loadEnabledRuleIds(properties, FORBIDDEN_ITEM_KEY_PREFIX));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid StackPlus server rules", exception);
        }
    }

    private static Properties readStackRules(String serializedRules) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(serializedRules == null ? "" : serializedRules));
            return properties;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Invalid StackPlus server rules", exception);
        }
    }

    public static void setStackLimit(int value) {
        if (serverRulesActive) {
            return;
        }
        stackLimit = clampStackLimit(value);
    }

    public static DisplayMode getDisplayMode() {
        return displayMode;
    }

    public static void setDisplayMode(DisplayMode mode) {
        displayMode = mode;
    }

    public static SelectedItemCountMode getSelectedItemCountMode() {
        return selectedItemCountMode;
    }

    public static void setSelectedItemCountMode(SelectedItemCountMode mode) {
        selectedItemCountMode = mode;
    }

    public static SelectedItemCountPosition getSelectedItemCountPosition() {
        return selectedItemCountPosition;
    }

    public static void setSelectedItemCountPosition(SelectedItemCountPosition position) {
        selectedItemCountPosition = position;
    }

    public static int getSelectedItemCountColorRgb() {
        return selectedItemCountColorRgb;
    }

    public static void setSelectedItemCountColorRgb(int colorRgb) {
        selectedItemCountColorRgb = colorRgb & 0xFFFFFF;
    }

    public static boolean isDurabilityWarningSuppressed() {
        return durabilityWarningSuppressed;
    }

    public static void setDurabilityWarningSuppressed(boolean suppressed) {
        durabilityWarningSuppressed = suppressed;
        saveConfig();
    }


    public static boolean isUpdateNotificationsEnabled() {
        return updateNotificationsEnabled;
    }

    public static void setUpdateNotificationsEnabled(boolean enabled) {
        updateNotificationsEnabled = enabled;
    }

    public static boolean areStackLimitPresetsVisible() {
        return stackLimitPresetsVisible;
    }

    public static void setStackLimitPresetsVisible(boolean visible) {
        stackLimitPresetsVisible = visible;
    }

    public static List<Integer> getCustomStackLimitPresets() {
        return List.copyOf(customStackLimitPresets);
    }

    public static boolean addCustomStackLimitPreset(int value) {
        int clampedValue = clampStackLimit(value);
        if (customStackLimitPresets.contains(clampedValue)) {
            return false;
        }
        if (customStackLimitPresets.size() >= MAX_CUSTOM_STACK_LIMIT_PRESETS) {
            return false;
        }

        customStackLimitPresets.add(clampedValue);
        saveConfig();
        return true;
    }

    public static boolean removeCustomStackLimitPreset(int value) {
        int clampedValue = clampStackLimit(value);
        if (!customStackLimitPresets.remove(Integer.valueOf(clampedValue))) {
            return false;
        }

        saveConfig();
        return true;
    }

    public static String getLastNotifiedReleaseVersion(String gameVersion) {
        return lastNotifiedReleaseVersions.getOrDefault(gameVersion, "");
    }

    public static void setLastNotifiedReleaseVersion(String gameVersion, String releaseVersion) {
        if (gameVersion == null || gameVersion.isBlank()) {
            return;
        }

        if (releaseVersion == null || releaseVersion.isBlank()) {
            lastNotifiedReleaseVersions.remove(gameVersion);
        } else {
            lastNotifiedReleaseVersions.put(gameVersion, releaseVersion);
        }
        saveConfig();
    }

    public static void saveSettings(int stackLimitValue, DisplayMode displayModeValue, SelectedItemCountMode selectedItemCountModeValue,
                                    SelectedItemCountPosition selectedItemCountPositionValue,
                                    int selectedItemCountColorRgbValue,
                                    boolean updateNotificationsEnabledValue) {
        setStackLimit(stackLimitValue);
        setDisplayMode(displayModeValue);
        setSelectedItemCountMode(selectedItemCountModeValue);
        setSelectedItemCountPosition(selectedItemCountPositionValue);
        setSelectedItemCountColorRgb(selectedItemCountColorRgbValue);
        setUpdateNotificationsEnabled(updateNotificationsEnabledValue);
        saveConfig();
    }

    public static void saveStackLimitPresetsVisible(boolean visible) {
        setStackLimitPresetsVisible(visible);
        saveConfig();
    }

    public static void saveItemStackLimit(Item item, int value) {
        if (serverRulesActive) {
            return;
        }
        applyItemStackLimit(item, clampStackLimit(value));
        saveConfig();
    }

    public static void saveItemStackLimit(ItemStack stack, int value) {
        if (serverRulesActive) {
            return;
        }
        applyItemStackLimit(stack, clampStackLimit(value));
        saveConfig();
    }

    public static void saveItemStackLimits(Collection<Item> items, int value) {
        if (serverRulesActive) {
            return;
        }
        int clampedValue = clampStackLimit(value);
        for (Item item : items) {
            applyItemStackLimit(item, clampedValue);
        }
        saveConfig();
    }

    public static void saveStackVariantLimits(Collection<ItemStack> stacks, int value) {
        if (serverRulesActive) {
            return;
        }
        int clampedValue = clampStackLimit(value);
        for (ItemStack stack : stacks) {
            if (clampedValue == getDefaultStackLimit(stack, stackLimit)) {
                removeItemStackLimit(stack);
                continue;
            }
            applyItemStackLimit(stack, clampedValue);
        }
        saveConfig();
    }

    /** 設定を持たない状態での、このアイテムの既定スタック数を返します。 */
    public static int getDefaultStackLimit(ItemStack stack, int defaultStackLimit) {
        return stack.isDamageableItem() ? 1 : clampStackLimit(defaultStackLimit);
    }

    /** アイテム単位の明示的なスタック禁止を保存します。 */
    public static void setItemStackingForbidden(Item item, boolean forbidden) {
        if (serverRulesActive) {
            return;
        }
        String itemId = getItemId(item);
        if (forbidden) {
            forbiddenStackableItems.add(itemId);
        } else {
            forbiddenStackableItems.remove(itemId);
        }
        saveConfig();
    }

    public static void removeItemStackLimit(Item item) {
        if (serverRulesActive) {
            return;
        }
        String itemId = getItemId(item);
        itemStackLimits.remove(itemId);
        removeStackVariantLimits(itemId);
        forcedStackableItems.remove(itemId);
        forbiddenStackableItems.remove(itemId);
        saveConfig();
    }

    public static void removeItemStackLimit(ItemStack stack) {
        if (serverRulesActive) {
            return;
        }
        String stackVariantKey = getStackVariantKey(stack);
        if (stackVariantKey == null) {
            removeItemStackLimit(stack.getItem());
            return;
        }

        stackVariantLimits.remove(stackVariantKey);
        forbiddenStackableItems.remove(getItemId(stack.getItem()));
        saveConfig();
    }

    public static void removeItemStackLimits(Collection<Item> items) {
        if (serverRulesActive) {
            return;
        }
        for (Item item : items) {
            String itemId = getItemId(item);
            itemStackLimits.remove(itemId);
            removeStackVariantLimits(itemId);
            forcedStackableItems.remove(itemId);
            forbiddenStackableItems.remove(itemId);
        }
        saveConfig();
    }

    public static void removeStackVariantLimits(Collection<ItemStack> stacks) {
        if (serverRulesActive) {
            return;
        }
        for (ItemStack stack : stacks) {
            String stackVariantKey = getStackVariantKey(stack);
            if (stackVariantKey == null) {
                String itemId = getItemId(stack.getItem());
                itemStackLimits.remove(itemId);
                removeStackVariantLimits(itemId);
                forcedStackableItems.remove(itemId);
                forbiddenStackableItems.remove(itemId);
            } else {
                stackVariantLimits.remove(stackVariantKey);
            }
        }
        saveConfig();
    }

    /** 個別設定（アイテム・バリアント・禁止）の有無を一覧絞り込み用に返します。 */
    public static boolean hasConfiguredStackLimit(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String itemId = getItemId(stack.getItem());
        String stackVariantKey = getStackVariantKey(stack);
        return forbiddenStackableItems.contains(itemId)
                || (stackVariantKey != null && stackVariantLimits.containsKey(stackVariantKey))
                || itemStackLimits.containsKey(itemId);
    }

    public static boolean isItemStackingForbidden(Item item) {
        return forbiddenStackableItems.contains(getItemId(item));
    }

    private static void applyItemStackLimit(Item item, int clampedValue) {
        String itemId = getItemId(item);
        if (clampedValue > 1) {
            forcedStackableItems.add(itemId);
        } else {
            forcedStackableItems.remove(itemId);
        }

        itemStackLimits.put(itemId, clampedValue);
    }

    private static void applyItemStackLimit(ItemStack stack, int clampedValue) {
        if (stack.isEmpty()) {
            return;
        }

        String stackVariantKey = getStackVariantKey(stack);
        if (stackVariantKey == null) {
            applyItemStackLimit(stack.getItem(), clampedValue);
            return;
        }

        stackVariantLimits.put(stackVariantKey, clampedValue);
    }

    public static int clampStackLimit(int value) {
        return Math.max(MIN_STACK_LIMIT, Math.min(MAX_STACK_LIMIT, value));
    }

    public static String formatStackLimit(int value) {
        return NUMBER_FORMAT.format(value);
    }

    public static int getAdjustedStackLimit(int originalLimit) {
        if (!areStackRulesEnabled()) {
            return originalLimit;
        }
        if (originalLimit <= 1) {
            return originalLimit;
        }

        return stackLimit;
    }

    public static int getAdjustedStackLimit(Item item, int originalLimit) {
        if (!areStackRulesEnabled()) {
            return originalLimit;
        }
        if (isItemStackingForbidden(item)) {
            return 1;
        }
        if (!canApplyConfiguredStackLimit(item, originalLimit)) {
            return originalLimit;
        }

        Integer itemLimit = itemStackLimits.get(getItemId(item));
        if (itemLimit != null) {
            return itemLimit;
        }

        if (isForcedStackableItem(item)) {
            return stackLimit;
        }

        return originalLimit <= 1 && !new ItemStack(item).isDamageableItem()
                ? stackLimit
                : getAdjustedStackLimit(originalLimit);
    }

    private static boolean canApplyConfiguredStackLimit(Item item, int originalLimit) {
        return originalLimit > 1
                || !new ItemStack(item).isDamageableItem()
                || itemStackLimits.containsKey(getItemId(item))
                || isForcedStackableItem(item);
    }

    public static int getAdjustedStackLimit(ItemStack stack, int originalLimit) {
        int adjustedLimit = getSafeStackCountLimit(stack, originalLimit);
        return StackLimitMath.effectiveStackLimit(
                areStackRulesEnabled(), originalLimit, adjustedLimit, stack.getCount());
    }

    public static int getSafeStackCountLimit(ItemStack stack) {
        return getSafeStackCountLimit(stack, stack.getMaxStackSize());
    }

    public static int getSafeStackCountLimit(ItemStack stack, int originalLimit) {
        if (!areStackRulesEnabled()) {
            return originalLimit;
        }
        if (stack.isEmpty()) {
            return originalLimit;
        }

        String stackVariantKey = getStackVariantKey(stack);
        if (stackVariantKey != null) {
            Integer stackVariantLimit = stackVariantLimits.get(stackVariantKey);
            if (stackVariantLimit != null) {
                return clampStackLimit(stackVariantLimit);
            }

            Integer itemLimit = itemStackLimits.get(getItemId(stack.getItem()));
            if (itemLimit != null) {
                return clampStackLimit(itemLimit);
            }
            if (isItemStackingForbidden(stack.getItem())) {
                return 1;
            }
            return stack.isDamageableItem() ? originalLimit : clampStackLimit(stackLimit);
        }

        return clampStackLimit(getAdjustedStackLimit(stack.getItem(), originalLimit));
    }

    public static ItemStack clampStackCount(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        int safeLimit = getSafeStackCountLimit(stack);
        if (stack.getCount() <= safeLimit) {
            return stack;
        }

        ItemStack clampedStack = stack.copy();
        clampedStack.setCount(safeLimit);
        return clampedStack;
    }

    private static void saveConfig() {
        Properties properties = new Properties();
        Properties persistedStackRules = serverRulesActive ? readStackRules(localStackRules) : null;
        properties.setProperty(STACK_LIMIT_KEY, persistedStackRules == null
                ? String.valueOf(stackLimit)
                : persistedStackRules.getProperty(STACK_LIMIT_KEY, String.valueOf(DEFAULT_STACK_LIMIT)));
        properties.setProperty(DISPLAY_MODE_KEY, displayMode.getSerializedName());
        properties.setProperty(SELECTED_ITEM_COUNT_MODE_KEY, selectedItemCountMode.getSerializedName());
        properties.setProperty(SELECTED_ITEM_COUNT_POSITION_KEY, selectedItemCountPosition.getSerializedName());
        properties.setProperty(SELECTED_ITEM_COUNT_COLOR_KEY, String.format(Locale.ROOT, "%06X", selectedItemCountColorRgb));
        properties.setProperty(DURABILITY_WARNING_SUPPRESSED_KEY, String.valueOf(durabilityWarningSuppressed));
        properties.setProperty(UPDATE_NOTIFICATIONS_ENABLED_KEY, String.valueOf(updateNotificationsEnabled));
        properties.setProperty(STACK_LIMIT_PRESETS_VISIBLE_KEY, String.valueOf(stackLimitPresetsVisible));
        properties.setProperty(CUSTOM_STACK_LIMIT_PRESETS_KEY, serializeCustomStackLimitPresets());
        Map<String, Integer> persistedItemLimits = persistedStackRules == null ? itemStackLimits : loadItemStackLimits(persistedStackRules);
        Map<String, Integer> persistedVariantLimits = persistedStackRules == null ? stackVariantLimits : loadStackVariantLimits(persistedStackRules);
        Set<String> persistedForcedItems = persistedStackRules == null ? forcedStackableItems : loadForcedStackableItems(persistedStackRules);
        Set<String> persistedForbiddenItems = persistedStackRules == null ? forbiddenStackableItems : StackRuleConfigSupport.loadEnabledRuleIds(persistedStackRules, FORBIDDEN_ITEM_KEY_PREFIX);
        persistedItemLimits.forEach((itemId, itemLimit) -> properties.setProperty(ITEM_LIMIT_KEY_PREFIX + itemId, String.valueOf(itemLimit)));
        persistedVariantLimits.forEach((stackVariantKey, stackVariantLimit) -> properties.setProperty(STACK_LIMIT_KEY_PREFIX + stackVariantKey, String.valueOf(stackVariantLimit)));
        persistedForcedItems.forEach(itemId -> properties.setProperty(FORCED_ITEM_KEY_PREFIX + itemId, "true"));
        persistedForbiddenItems.forEach(itemId -> properties.setProperty(FORBIDDEN_ITEM_KEY_PREFIX + itemId, "true"));
        lastNotifiedReleaseVersions.forEach((gameVersion, releaseVersion) ->
                properties.setProperty(LAST_NOTIFIED_RELEASE_KEY_PREFIX + gameVersion, releaseVersion));

        Path configPath = getConfigPath();
        try {
            Files.createDirectories(configPath.getParent());
            try (OutputStream output = Files.newOutputStream(configPath)) {
                properties.store(output, "StackPlus configuration");
            }
        } catch (IOException exception) {
            StackPlus.LOGGER.warn("StackPlus 設定ファイルの保存に失敗しました: {}", configPath, exception);
        }
    }

    private static boolean isForcedStackableItem(Item item) {
        return forcedStackableItems.contains(getItemId(item));
    }

    /**
     * DataComponents と instanceof でバニラの非スタックアイテムを正確に判定します。
     * ユーザーが明示的に有効化したアイテム（forcedStackableItems）も含みます。
     * パス文字列の endsWith/startsWith による誤ヒットを防ぎます。
     */
    private static boolean isForcedStackableItem(ItemStack stack) {
        return forcedStackableItems.contains(getItemId(stack.getItem()))
                || isNaturallyUnstackable(stack);
    }

    /** 耐久品やバニラ固有ルールで自然にスタックできないアイテムを判定します。 */
    public static boolean isNaturallyUnstackable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();

        if (item instanceof BedItem
                || item instanceof BoatItem
                || item instanceof MinecartItem
                || item == Items.ENCHANTED_BOOK
                || item == Items.SADDLE
                || item == Items.TOTEM_OF_UNDYING
                || item == Items.GOAT_HORN) {
            return true;
        }

        DataComponentMap components = stack.getComponents();

        if (components.has(DataComponents.POTION_CONTENTS) && item != Items.TIPPED_ARROW) {
            return true;
        }

        if (components.has(DataComponents.BUCKET_ENTITY_DATA)
                || item == Items.BUCKET
                || item == Items.LAVA_BUCKET
                || item == Items.WATER_BUCKET
                || item == Items.MILK_BUCKET
                || item == Items.POWDER_SNOW_BUCKET) {
            return true;
        }

        if (components.has(DataComponents.JUKEBOX_PLAYABLE)
                || components.has(DataComponents.PROVIDES_BANNER_PATTERNS)) {
            return true;
        }

        var useRemainder = components.get(DataComponents.USE_REMAINDER);
        if (useRemainder != null && useRemainder.convertInto().getItem() == Items.BOWL) {
            return true;
        }

        return false;
    }

    private static String getItemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    private static String getStackVariantKey(ItemStack stack) {
        try {
            String itemId = getItemId(stack.getItem());
            String defaultComponents = new ItemStack(stack.getItem()).getComponents().toString();
            String stackComponents = stack.getComponents().toString();
            if (defaultComponents.equals(stackComponents)) {
                return null;
            }

            String encodedComponents = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(stackComponents.getBytes(StandardCharsets.UTF_8));
            return itemId + "#" + encodedComponents;
        } catch (RuntimeException exception) {
            // タイトル画面ではアイテムコンポーネントが未初期化のため、アイテム単位として扱います。
            return null;
        }
    }

    private static void removeStackVariantLimits(String itemId) {
        stackVariantLimits.keySet().removeIf(stackVariantKey -> stackVariantKey.startsWith(itemId + "#"));
    }

    private static ConfigValues loadConfig() {
        Path configPath = getConfigPath();
        if (!Files.exists(configPath)) {
            return defaultConfigValues();
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(configPath)) {
            properties.load(input);
            int loadedStackLimit = clampStackLimit(Integer.parseInt(properties.getProperty(STACK_LIMIT_KEY, String.valueOf(DEFAULT_STACK_LIMIT))));
            DisplayMode loadedDisplayMode = DisplayMode.fromSerializedName(properties.getProperty(DISPLAY_MODE_KEY, DisplayMode.COMPACT.getSerializedName()));
            SelectedItemCountMode loadedSelectedItemCountMode = loadSelectedItemCountMode(properties);
            SelectedItemCountPosition loadedSelectedItemCountPosition = SelectedItemCountPosition.fromSerializedName(properties.getProperty(SELECTED_ITEM_COUNT_POSITION_KEY, SelectedItemCountPosition.BESIDE.getSerializedName()));
            int loadedSelectedItemCountColorRgb = loadSelectedItemCountColorRgb(properties);
            boolean loadedDurabilityWarningSuppressed = Boolean.parseBoolean(properties.getProperty(DURABILITY_WARNING_SUPPRESSED_KEY, "false"));
            boolean loadedUpdateNotificationsEnabled = Boolean.parseBoolean(properties.getProperty(UPDATE_NOTIFICATIONS_ENABLED_KEY, "true"));
            boolean loadedStackLimitPresetsVisible = Boolean.parseBoolean(properties.getProperty(STACK_LIMIT_PRESETS_VISIBLE_KEY, "true"));
            return new ConfigValues(loadedStackLimit, loadedDisplayMode, loadedSelectedItemCountMode, loadedSelectedItemCountPosition, loadedSelectedItemCountColorRgb,
                    loadedDurabilityWarningSuppressed, loadedUpdateNotificationsEnabled, loadedStackLimitPresetsVisible, loadCustomStackLimitPresets(properties), loadItemStackLimits(properties),
                    loadStackVariantLimits(properties), loadForcedStackableItems(properties),
                    StackRuleConfigSupport.loadEnabledRuleIds(properties, FORBIDDEN_ITEM_KEY_PREFIX),
                    loadLastNotifiedReleaseVersions(properties));
        } catch (IOException | NumberFormatException exception) {
            StackPlus.LOGGER.warn("StackPlus 設定ファイルの読み込みに失敗しました。既定値を使用します: {}", configPath, exception);
            backupBrokenConfig(configPath);
            return defaultConfigValues();
        }
    }

    private static SelectedItemCountMode loadSelectedItemCountMode(Properties properties) {
        String savedMode = properties.getProperty(SELECTED_ITEM_COUNT_MODE_KEY);
        if (savedMode != null) {
            return SelectedItemCountMode.fromSerializedName(savedMode);
        }

        boolean legacyAlwaysVisible = Boolean.parseBoolean(properties.getProperty(SELECTED_ITEM_COUNT_ALWAYS_VISIBLE_KEY, "false"));
        return legacyAlwaysVisible ? SelectedItemCountMode.ALWAYS : SelectedItemCountMode.ON_SWITCH;
    }

    private static int loadSelectedItemCountColorRgb(Properties properties) {
        String storedColor = properties.getProperty(SELECTED_ITEM_COUNT_COLOR_KEY, "white").strip();
        return switch (storedColor) {
            case "white" -> 0xFFFFFF;
            case "yellow" -> 0xFFFF55;
            case "aqua" -> 0x55FFFF;
            case "green" -> 0x55FF55;
            case "light_purple" -> 0xFF55FF;
            default -> {
                try {
                    yield Integer.parseInt(storedColor.replace("#", ""), 16) & 0xFFFFFF;
                } catch (NumberFormatException exception) {
                    yield 0xFFFFFF;
                }
            }
        };
    }

    private static Map<String, Integer> loadItemStackLimits(Properties properties) {
        Map<String, Integer> loadedItemStackLimits = new LinkedHashMap<>();
        for (String propertyName : properties.stringPropertyNames()) {
            if (!propertyName.startsWith(ITEM_LIMIT_KEY_PREFIX)) {
                continue;
            }

            String itemId = propertyName.substring(ITEM_LIMIT_KEY_PREFIX.length());
            if (itemId.isBlank()) {
                continue;
            }

            try {
                int itemLimit = clampStackLimit(Integer.parseInt(properties.getProperty(propertyName)));
                loadedItemStackLimits.put(itemId, itemLimit);
            } catch (NumberFormatException exception) {
                StackPlus.LOGGER.warn("StackPlus 設定のアイテム別上限をスキップしました: {}={}", propertyName, properties.getProperty(propertyName));
            }
        }
        return loadedItemStackLimits;
    }

    private static List<Integer> loadCustomStackLimitPresets(Properties properties) {
        String savedPresets = properties.getProperty(CUSTOM_STACK_LIMIT_PRESETS_KEY, "");
        if (savedPresets.isBlank()) {
            return List.of();
        }

        List<Integer> loadedPresets = new ArrayList<>();
        for (String savedPreset : savedPresets.split(",")) {
            String trimmedPreset = savedPreset.trim();
            if (trimmedPreset.isEmpty()) {
                continue;
            }

            try {
                int preset = clampStackLimit(Integer.parseInt(trimmedPreset));
                if (!loadedPresets.contains(preset)) {
                    loadedPresets.add(preset);
                    if (loadedPresets.size() >= MAX_CUSTOM_STACK_LIMIT_PRESETS) {
                        break;
                    }
                }
            } catch (NumberFormatException exception) {
                StackPlus.LOGGER.warn("StackPlus 設定の自作プリセットをスキップしました: {}", trimmedPreset);
            }
        }
        return loadedPresets;
    }

    private static String serializeCustomStackLimitPresets() {
        return customStackLimitPresets.stream()
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private static Set<String> loadForcedStackableItems(Properties properties) {
        Set<String> loadedForcedItems = new LinkedHashSet<>();
        for (String propertyName : properties.stringPropertyNames()) {
            if (!propertyName.startsWith(FORCED_ITEM_KEY_PREFIX)) {
                continue;
            }

            String itemId = propertyName.substring(FORCED_ITEM_KEY_PREFIX.length());
            if (!itemId.isBlank() && Boolean.parseBoolean(properties.getProperty(propertyName))) {
                loadedForcedItems.add(itemId);
            }
        }
        return loadedForcedItems;
    }

    private static Map<String, Integer> loadStackVariantLimits(Properties properties) {
        Map<String, Integer> loadedStackVariantLimits = new LinkedHashMap<>();
        for (String propertyName : properties.stringPropertyNames()) {
            if (!propertyName.startsWith(STACK_LIMIT_KEY_PREFIX)) {
                continue;
            }

            String stackVariantKey = propertyName.substring(STACK_LIMIT_KEY_PREFIX.length());
            if (stackVariantKey.isBlank()) {
                continue;
            }

            try {
                int stackVariantLimit = clampStackLimit(Integer.parseInt(properties.getProperty(propertyName)));
                loadedStackVariantLimits.put(stackVariantKey, stackVariantLimit);
            } catch (NumberFormatException exception) {
                StackPlus.LOGGER.warn("StackPlus 設定の種類別上限をスキップしました: {}={}", propertyName, properties.getProperty(propertyName));
            }
        }
        return loadedStackVariantLimits;
    }

    private static Map<String, String> loadLastNotifiedReleaseVersions(Properties properties) {
        Map<String, String> loadedVersions = new LinkedHashMap<>();
        for (String propertyName : properties.stringPropertyNames()) {
            if (!propertyName.startsWith(LAST_NOTIFIED_RELEASE_KEY_PREFIX)) {
                continue;
            }

            String gameVersion = propertyName.substring(LAST_NOTIFIED_RELEASE_KEY_PREFIX.length());
            String releaseVersion = properties.getProperty(propertyName, "").strip();
            if (!gameVersion.isBlank() && !releaseVersion.isBlank()) {
                loadedVersions.put(gameVersion, releaseVersion);
            }
        }
        return loadedVersions;
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
    }

    private static ConfigValues defaultConfigValues() {
        return new ConfigValues(DEFAULT_STACK_LIMIT, DisplayMode.COMPACT, SelectedItemCountMode.ON_SWITCH,
                SelectedItemCountPosition.BESIDE, 0xFFFFFF, false, true, true, List.of(), Map.of(), Map.of(), Set.of(), Set.of(), Map.of());
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
            StackPlus.LOGGER.warn("StackPlus の壊れた設定ファイルを退避しました: {}", backupPath);
        } catch (IOException backupException) {
            StackPlus.LOGGER.warn("StackPlus の壊れた設定ファイル退避に失敗しました: {}", configPath, backupException);
        }
    }

    private record ConfigValues(int stackLimit, DisplayMode displayMode, SelectedItemCountMode selectedItemCountMode,
                                SelectedItemCountPosition selectedItemCountPosition, int selectedItemCountColorRgb,
                                boolean durabilityWarningSuppressed,
                                boolean updateNotificationsEnabled, boolean stackLimitPresetsVisible,
                                List<Integer> customStackLimitPresets,
                                Map<String, Integer> itemStackLimits, Map<String, Integer> stackVariantLimits,
                                Set<String> forcedStackableItems, Set<String> forbiddenStackableItems,
                                Map<String, String> lastNotifiedReleaseVersions) {
    }
}
