package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class StackPlusItemSelection {
    private static final int LEFT_MOUSE_BUTTON = 0;

    public static void start(Screen parent, int itemStackLimit) {
        Minecraft client = Minecraft.getInstance();
        client.setScreen(new ItemSelectionScreen(parent, itemStackLimit));
    }

    private StackPlusItemSelection() {
    }

    private static final class ItemSelectionScreen extends Screen {
        private static final int SLOT_SIZE = 18;
        private static final int SLOT_GAP = 2;
        private static final int SLOT_PITCH = SLOT_SIZE + SLOT_GAP;
        private static final int MAX_GRID_COLUMNS = 16;
        private static final int ITEM_OFFSET = 1;
        private static final int PANEL_MARGIN = 18;
        private static final int GRID_PADDING = 10;
        private static final int LEFT_PANEL_WIDTH = 220;
        private static final int PANEL_GAP = 12;
        private static final int SEARCH_HEIGHT = 20;
        private static final int SORT_BUTTON_WIDTH = 56;
        private static final int SEARCH_SORT_GAP = 6;
        private static final int HEADER_HEIGHT = 54;
        private static final int FOOTER_HEIGHT = 14;
        private static final int BUTTON_HEIGHT = 20;
        private static final int PANEL_COLOR = 0x58000000;
        private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;
        private static final int SLOT_HOVER_COLOR = 0x66FFFFFF;
        private static final int SLOT_SELECTED_COLOR = 0x8800A8FF;
        private static final int SLOT_BACKGROUND_COLOR = 0x30000000;
        private static final long SELECTED_ITEM_DISPLAY_INTERVAL_MS = 3_000L;
        private static final int[] LIMIT_PRESETS = {64, 999, 1_000, 10_000, 32_767, 1_000_000};

        private final Screen parent;
        private final int initialStackLimit;
        private final boolean stackLimitPresetsVisible;
        private final List<Entry> allItems;
        private final List<Entry> filteredItems = new ArrayList<>();
        private final Set<Entry> selectedEntries = new LinkedHashSet<>();
        private EditBox searchBox;
        private EditBox limitInput;
        private SortMode sortMode = SortMode.CREATIVE;
        private String searchText = "";
        private int pendingLimit;
        private int scrollRows;
        private boolean updatingLimitInput;

        private ItemSelectionScreen(Screen parent, int itemStackLimit) {
            super(Component.translatable("screen.stackplus.item_selection.title"));
            this.parent = parent;
            this.initialStackLimit = StackLimitConfig.clampStackLimit(itemStackLimit);
            this.stackLimitPresetsVisible = StackLimitConfig.areStackLimitPresetsVisible();
            this.pendingLimit = this.initialStackLimit;
            this.allItems = createEntries();
            filterItems();
        }

        @Override
        protected void init() {
            int gridLeft = getGridLeft();
            int gridWidth = getGridWidth();
            int searchWidth = gridWidth - SORT_BUTTON_WIDTH - SEARCH_SORT_GAP;
            this.searchBox = new EditBox(this.font, gridLeft, 28, searchWidth, SEARCH_HEIGHT,
                    Component.translatable("screen.stackplus.item_selection.search"));
            this.searchBox.setMaxLength(80);
            this.searchBox.setValue(searchText);
            updateSearchHint();
            this.searchBox.setResponder(value -> {
                searchText = value;
                updateSearchHint();
                scrollRows = 0;
                filterItems();
            });
            addRenderableWidget(searchBox);
            addRenderableWidget(Button.builder(Component.literal(sortMode.label()), button -> {
                sortMode = sortMode.next();
                button.setMessage(Component.literal(sortMode.label()));
                scrollRows = 0;
                filterItems();
            }).bounds(gridLeft + searchWidth + SEARCH_SORT_GAP, 28, SORT_BUTTON_WIDTH, SEARCH_HEIGHT).build());

            int left = getLeftPanelLeft() + GRID_PADDING;
            int contentWidth = getLeftPanelWidth() - GRID_PADDING * 2;
            this.limitInput = new EditBox(this.font, left, 118, contentWidth, 20,
                    Component.translatable("screen.stackplus.item_selection.limit_input"));
            this.limitInput.setMaxLength(14);
            this.limitInput.setResponder(this::onLimitInputChanged);
            addRenderableWidget(limitInput);

            if (stackLimitPresetsVisible) {
                addPresetButtons(left, 142);
            }
            int backButtonY = this.height - 42;
            int halfWidth = (contentWidth - 6) / 2;
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.save"), button -> saveSelectedItem())
                    .bounds(left, backButtonY - BUTTON_HEIGHT - 6, halfWidth, BUTTON_HEIGHT)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.reset"), button -> resetSelectedItem())
                    .bounds(left + halfWidth + 6, backButtonY - BUTTON_HEIGHT - 6, halfWidth, BUTTON_HEIGHT)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.back"), button -> onClose())
                    .bounds(left, backButtonY, contentWidth, BUTTON_HEIGHT)
                    .build());

            setLimitInputText(pendingLimit);
            setInitialFocus(searchBox);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.render(graphics, mouseX, mouseY, partialTick);
            renderSelectedItem(graphics);
            renderItems(graphics, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (super.mouseClicked(event, doubleClick)) {
                return true;
            }
            if (event.button() != LEFT_MOUSE_BUTTON) {
                return false;
            }

            Entry entry = getEntryAt(event.x(), event.y());
            if (entry == null) {
                return false;
            }

            selectEntry(entry);
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!isMouseInRightPanel(mouseX, mouseY)) {
                return false;
            }

            int maxScrollRows = Math.max(0, getTotalRows() - getVisibleRows());
            scrollRows = Math.max(0, Math.min(maxScrollRows, scrollRows - (int) Math.signum(scrollY)));
            return true;
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // 標準背景の後、ウィジェットより前にパネルを描き、後段背景による上書きを防ぐ。
            renderStackPlusBackground(graphics, partialTick);
            renderPanels(graphics);
        }

        private void renderStackPlusBackground(GuiGraphics graphics, float partialTick) {
            if (this.minecraft.level == null) {
                this.renderPanorama(graphics, partialTick);
            }
            this.renderBlurredBackground(graphics);
            this.renderTransparentBackground(graphics);
        }

        private void renderPanels(GuiGraphics graphics) {
            graphics.fill(getLeftPanelLeft(), 6, getLeftPanelRight(), this.height - 6, PANEL_COLOR);
            graphics.fill(getLeftPanelLeft(), 6, getLeftPanelLeft() + getLeftPanelWidth(), 7, PANEL_BORDER_COLOR);
        graphics.fill(getLeftPanelLeft(), this.height - 7, getLeftPanelLeft() + getLeftPanelWidth(), this.height - 6, PANEL_BORDER_COLOR);
        graphics.fill(getLeftPanelLeft(), 6, getLeftPanelLeft() + 1, this.height - 6, PANEL_BORDER_COLOR);
        graphics.fill(getLeftPanelLeft() + getLeftPanelWidth() - 1, 6, getLeftPanelLeft() + getLeftPanelWidth(), this.height - 6, PANEL_BORDER_COLOR);
            graphics.fill(getRightLeft(), 6, getRightRight(), this.height - 6, PANEL_COLOR);
            graphics.fill(getRightLeft(), 6, getRightLeft() + getRightWidth(), 7, PANEL_BORDER_COLOR);
        graphics.fill(getRightLeft(), this.height - 7, getRightLeft() + getRightWidth(), this.height - 6, PANEL_BORDER_COLOR);
        graphics.fill(getRightLeft(), 6, getRightLeft() + 1, this.height - 6, PANEL_BORDER_COLOR);
        graphics.fill(getRightLeft() + getRightWidth() - 1, 6, getRightLeft() + getRightWidth(), this.height - 6, PANEL_BORDER_COLOR);
            drawCenteredText(graphics, Component.translatable("screen.stackplus.item_selection.selected_title"), getLeftPanelLeft() + getLeftPanelWidth() / 2, 18, 0xFFFFFFFF);
            drawCenteredText(graphics, this.title, getRightLeft() + getRightWidth() / 2, 10, 0xFFFFFFFF);
        }

        private void renderSelectedItem(GuiGraphics graphics) {
            int centerX = getLeftPanelLeft() + getLeftPanelWidth() / 2;
            if (selectedEntries.isEmpty()) {
                drawCenteredText(graphics, Component.translatable("screen.stackplus.item_selection.no_selection"), centerX, 74, 0xFFE0E0E0);
                return;
            }

            Entry displayedEntry = getDisplayedSelectedEntry();
            graphics.renderItem(displayedEntry.stack(), centerX - 8, 48);
            drawCenteredText(graphics, displayedEntry.stack().getDisplayName(), centerX, 72, 0xFFFFFFFF);
            drawCenteredText(graphics, Component.literal(displayedEntry.id()), centerX, 88, 0xFFB8B8B8);
            graphics.drawString(this.font, Component.translatable("screen.stackplus.item_selection.limit_label"), getLeftPanelLeft() + GRID_PADDING, 104, 0xFFE0E0E0, false);
        }

        private void drawCenteredText(GuiGraphics graphics, Component text, int centerX, int y, int color) {
            graphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
        }

        private Entry getDisplayedSelectedEntry() {
            if (selectedEntries.size() <= 1) {
                return selectedEntries.iterator().next();
            }

            int displayIndex = (int) ((System.currentTimeMillis() / SELECTED_ITEM_DISPLAY_INTERVAL_MS) % selectedEntries.size());
            int index = 0;
            for (Entry entry : selectedEntries) {
                if (index == displayIndex) {
                    return entry;
                }
                index++;
            }
            return selectedEntries.iterator().next();
        }

        private void renderItems(GuiGraphics graphics, int mouseX, int mouseY) {
            int columns = getColumns();
            int visibleRows = getVisibleRows();
            int startIndex = scrollRows * columns;
            int endIndex = Math.min(filteredItems.size(), startIndex + columns * visibleRows);
            int gridLeft = getGridLeft();
            int gridTop = getGridTop();

            for (int index = startIndex; index < endIndex; index++) {
                int visibleIndex = index - startIndex;
                int column = visibleIndex % columns;
                int row = visibleIndex / columns;
                int x = gridLeft + column * SLOT_PITCH;
                int y = gridTop + row * SLOT_PITCH;
                Entry entry = filteredItems.get(index);

                graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND_COLOR);
                if (selectedEntries.contains(entry)) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_SELECTED_COLOR);
                } else if (isMouseInSlot(mouseX, mouseY, x, y)) {
                    graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_HOVER_COLOR);
                    graphics.setTooltipForNextFrame(this.font, entry.stack(), mouseX, mouseY);
                }
                graphics.renderItem(entry.stack(), x + ITEM_OFFSET, y + ITEM_OFFSET);
            }
        }

        private void addPresetButtons(int left, int top) {
            int gap = 6;
            int columns = 2;
            int buttonWidth = (getLeftPanelWidth() - GRID_PADDING * 2 - gap) / columns;
            for (int index = 0; index < LIMIT_PRESETS.length; index++) {
                int value = LIMIT_PRESETS[index];
                int x = left + index % columns * (buttonWidth + gap);
                int y = top + index / columns * 22;
                addRenderableWidget(Button.builder(Component.literal(formatPreset(value)), button -> setPendingLimit(value, true))
                        .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                        .build());
            }
        }

        private void selectEntry(Entry entry) {
            if (hasShiftDown()) {
                toggleSelectedEntry(entry);
            } else {
                selectedEntries.clear();
                selectedEntries.add(entry);
            }
            setPendingLimit(StackLimitConfig.getAdjustedStackLimit(entry.stack(), initialStackLimit), true);
        }

        private void toggleSelectedEntry(Entry entry) {
            if (!selectedEntries.add(entry)) {
                selectedEntries.remove(entry);
            }
        }

        private void saveSelectedItem() {
            if (selectedEntries.isEmpty()) {
                return;
            }

            syncPendingLimitFromInput();
            StackLimitConfig.saveStackVariantLimits(selectedEntries.stream().map(Entry::stack).toList(), pendingLimit);
        }

        private void resetSelectedItem() {
            if (selectedEntries.isEmpty()) {
                return;
            }

            StackLimitConfig.removeStackVariantLimits(selectedEntries.stream().map(Entry::stack).toList());
            setPendingLimit(getResetStackLimit(getDisplayedSelectedEntry()), true);
        }

        private int getResetStackLimit(Entry entry) {
            return StackLimitConfig.getSafeStackCountLimit(entry.stack(), initialStackLimit);
        }

        private boolean hasShiftDown() {
            long window = Minecraft.getInstance().getWindow().handle();
            return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                    || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        }

        private void onLimitInputChanged(String input) {
            if (updatingLimitInput || !isValidLimitInput(input)) {
                return;
            }

            Integer parsedValue = parseLimitInput(input);
            if (parsedValue != null) {
                setPendingLimit(parsedValue, false);
            }
        }

        private void syncPendingLimitFromInput() {
            Integer parsedValue = parseLimitInput(limitInput.getValue());
            if (parsedValue == null) {
                setLimitInputText(pendingLimit);
                return;
            }

            setPendingLimit(parsedValue, true);
        }

        private void setPendingLimit(int value, boolean updateInput) {
            pendingLimit = StackLimitConfig.clampStackLimit(value);
            if (updateInput && limitInput != null) {
                setLimitInputText(pendingLimit);
            }
        }

        private void setLimitInputText(int value) {
            updatingLimitInput = true;
            limitInput.setValue(StackLimitConfig.formatStackLimit(value));
            updatingLimitInput = false;
        }

        private Entry getEntryAt(double mouseX, double mouseY) {
            int columns = getColumns();
            int gridLeft = getGridLeft();
            int gridTop = getGridTop();
            int column = (int) ((mouseX - gridLeft) / SLOT_PITCH);
            int row = (int) ((mouseY - gridTop) / SLOT_PITCH);
            if (column < 0 || column >= columns || row < 0 || row >= getVisibleRows()) {
                return null;
            }

            int slotX = gridLeft + column * SLOT_PITCH;
            int slotY = gridTop + row * SLOT_PITCH;
            if (!isMouseInSlot(mouseX, mouseY, slotX, slotY)) {
                return null;
            }

            int index = (scrollRows + row) * columns + column;
            if (index < 0 || index >= filteredItems.size()) {
                return null;
            }
            return filteredItems.get(index);
        }

        private void filterItems() {
            String query = searchText.trim().toLowerCase(Locale.ROOT);
            filteredItems.clear();
            for (Entry entry : allItems) {
                if (query.isEmpty() || entry.id().contains(query) || entry.name().contains(query)) {
                    filteredItems.add(entry);
                }
            }
            filteredItems.sort(sortMode.comparator());
        }

        private void updateSearchHint() {
            if (searchBox == null) {
                return;
            }
            searchBox.setHint(searchText.isEmpty()
                    ? Component.translatable("screen.stackplus.item_selection.search_hint")
                    : Component.empty());
        }

        private int getLeftPanelLeft() {
            return PANEL_MARGIN;
        }

        private int getLeftPanelRight() {
            return getLeftPanelLeft() + getLeftPanelWidth();
        }

        private int getLeftPanelWidth() {
            return Math.min(LEFT_PANEL_WIDTH, Math.max(160, this.width / 3));
        }

        private int getRightLeft() {
            return getLeftPanelRight() + PANEL_GAP;
        }

        private int getRightRight() {
            return this.width - PANEL_MARGIN;
        }

        private int getRightWidth() {
            return Math.max(120, getRightRight() - getRightLeft());
        }

        private int getGridLeft() {
            return getRightLeft() + (getRightWidth() - getGridWidth()) / 2;
        }

        private int getGridWidth() {
            return getColumns() * SLOT_PITCH - SLOT_GAP;
        }

        private int getGridTop() {
            return HEADER_HEIGHT;
        }

        private int getColumns() {
            int availableWidth = getRightWidth() - GRID_PADDING * 2;
            return Math.max(1, Math.min(MAX_GRID_COLUMNS, (availableWidth + SLOT_GAP) / SLOT_PITCH));
        }

        private int getVisibleRows() {
            return Math.max(1, (this.height - HEADER_HEIGHT - FOOTER_HEIGHT + SLOT_GAP) / SLOT_PITCH);
        }

        private int getTotalRows() {
            return (filteredItems.size() + getColumns() - 1) / getColumns();
        }

        private boolean isMouseInRightPanel(double mouseX, double mouseY) {
            return mouseX >= getRightLeft() && mouseX < getRightRight() && mouseY >= 6 && mouseY < this.height - 6;
        }

        private static boolean isMouseInSlot(double mouseX, double mouseY, int x, int y) {
            return mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
        }

        private static boolean isValidLimitInput(String input) {
            if (input.length() > 14) {
                return false;
            }

            for (int index = 0; index < input.length(); index++) {
                char character = input.charAt(index);
                if (isLimitSuffix(character)) {
                    return index == input.length() - 1;
                }
                if (!Character.isDigit(character) && character != ',') {
                    return false;
                }
            }
            return true;
        }

        private static Integer parseLimitInput(String input) {
            String normalizedInput = input.trim().replace(",", "");
            if (normalizedInput.isEmpty()) {
                return null;
            }

            long multiplier = getLimitInputMultiplier(normalizedInput.charAt(normalizedInput.length() - 1));
            String digitsOnly = multiplier == 1
                    ? normalizedInput
                    : normalizedInput.substring(0, normalizedInput.length() - 1);
            if (digitsOnly.isEmpty()) {
                return null;
            }

            try {
                long value = Long.parseLong(digitsOnly) * multiplier;
                if (value > StackLimitConfig.MAX_STACK_LIMIT) {
                    return StackLimitConfig.MAX_STACK_LIMIT;
                }
                if (value < StackLimitConfig.MIN_STACK_LIMIT) {
                    return StackLimitConfig.MIN_STACK_LIMIT;
                }
                return (int) value;
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        private static boolean isLimitSuffix(char character) {
            return character == 'k' || character == 'K'
                    || character == 'm' || character == 'M'
                    || character == 'b' || character == 'B';
        }

        private static long getLimitInputMultiplier(char suffix) {
            return switch (suffix) {
                case 'k', 'K' -> 1_000L;
                case 'm', 'M' -> 1_000_000L;
                case 'b', 'B' -> 1_000_000_000L;
                default -> 1L;
            };
        }

        private static String formatPreset(int value) {
            return switch (value) {
                case 1_000 -> "1K";
                case 10_000 -> "10K";
                case 32_767 -> "32K";
                case 1_000_000 -> "1M";
                default -> String.valueOf(value);
            };
        }

        private static List<Entry> createEntries() {
            Map<String, Entry> entries = new LinkedHashMap<>();
            rebuildCreativeTabs();
            for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                addCreativeTabEntries(entries, tab);
            }

            BuiltInRegistries.ITEM.stream()
                    .forEach(item -> addRegistryEntry(entries, item));

            return new ArrayList<>(entries.values());
        }

        private static void rebuildCreativeTabs() {
            try {
                CreativeModeTabs.tryRebuildTabContents(FeatureFlags.DEFAULT_FLAGS, true, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));
            } catch (RuntimeException exception) {
                // Some title-screen paths still lack full registry context; fall back to safe entries below.
            }
        }

        private static void addCreativeTabEntries(Map<String, Entry> entries, CreativeModeTab tab) {
            try {
                addEntries(entries, tab.getDisplayItems());
                addEntries(entries, tab.getSearchTabDisplayItems());
            } catch (IllegalStateException exception) {
                // The title screen can open Mod Menu before creative tab contents are built.
            }
        }

        private static void addRegistryEntry(Map<String, Entry> entries, Item item) {
            try {
                Entry entry = Entry.fromItem(item);
                if (entry.isConfigurable()) {
                    entries.putIfAbsent(entry.key(), entry);
                }
            } catch (RuntimeException exception) {
                // Some item components are not bound yet when opened from the title screen.
            }
        }

        private static void addEntries(Map<String, Entry> entries, Iterable<ItemStack> stacks) {
            for (ItemStack stack : stacks) {
                Entry entry = new Entry(stack.copyWithCount(1));
                if (entry.isConfigurable()) {
                    entries.putIfAbsent(entry.key(), entry);
                }
            }
        }

        // 検索結果へ適用する表示順。名前が同じ場合はIDで順序を安定させる。
        private enum SortMode {
            CREATIVE,
            ID_ASCENDING,
            ID_DESCENDING,
            NAME_ASCENDING,
            NAME_DESCENDING;

            private SortMode next() {
                SortMode[] modes = values();
                return modes[(ordinal() + 1) % modes.length];
            }

            private String label() {
                return switch (this) {
                    case CREATIVE -> "Creative";
                    case ID_ASCENDING -> "ID ↑";
                    case ID_DESCENDING -> "ID ↓";
                    case NAME_ASCENDING -> "A-Z";
                    case NAME_DESCENDING -> "Z-A";
                };
            }

            private Comparator<Entry> comparator() {
                return switch (this) {
                    case CREATIVE -> (left, right) -> 0;
                    case ID_ASCENDING -> Comparator.comparing(Entry::id);
                    case ID_DESCENDING -> Comparator.comparing(Entry::id).reversed();
                    case NAME_ASCENDING -> Comparator.comparing(Entry::name).thenComparing(Entry::id);
                    case NAME_DESCENDING -> Comparator.comparing(Entry::name).reversed().thenComparing(Entry::id);
                };
            }
        }

        private record Entry(Item item, ItemStack stack, String id, String name, String key) {
            private static Entry fromItem(Item item) {
                ItemStack stack = createItemStack(item);
                String id = getItemId(item);
                return new Entry(item, stack, id, getSafeItemName(item, stack, id), id);
            }

            private Entry(Item item, ItemStack stack) {
                this(item, stack, getItemId(item), stack.getDisplayName().getString().toLowerCase(Locale.ROOT), getEntryKey(item, stack));
            }

            private Entry(ItemStack stack) {
                this(stack.getItem(), stack);
            }

            private static String getItemId(Item item) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
            }

            private static String getEntryKey(Item item, ItemStack stack) {
                String itemId = getItemId(item);
                if (!hasVariantComponents(item, stack)) {
                    return itemId;
                }
                return itemId + "#" + ItemStack.hashItemAndComponents(stack);
            }

            private static ItemStack createItemStack(Item item) {
                try {
                    return new ItemStack(item);
                } catch (RuntimeException exception) {
                    return new ItemStack(item);
                }
            }

            private static String getSafeItemName(Item item, ItemStack stack, String id) {
                try {
                    return stack.getDisplayName().getString().toLowerCase(Locale.ROOT);
                } catch (RuntimeException exception) {
                    return Component.translatable(item.getDescriptionId()).getString().toLowerCase(Locale.ROOT) + " " + id;
                }
            }

            private static DataComponentMap createFallbackComponents(Item item) {
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                DataComponentMap.Builder builder = DataComponentMap.builder()
                        .addAll(DataComponents.COMMON_ITEM_COMPONENTS)
                        .set(DataComponents.ITEM_NAME, Component.translatable(item.getDescriptionId()));
                if (id != null) {
                    builder.set(DataComponents.ITEM_MODEL, id);
                }
                return builder.build();
            }

            private static boolean hasVariantComponents(Item item, ItemStack stack) {
                try {
                    return !new ItemStack(item).getComponents().toString().equals(stack.getComponents().toString());
                } catch (RuntimeException exception) {
                    return false;
                }
            }

            private boolean isConfigurable() {
                return !stack.isEmpty() && !isDamageable();
            }

            private boolean isDamageable() {
                try {
                    return stack.isDamageableItem() || isKnownDamageableItemId(id);
                } catch (RuntimeException exception) {
                    try {
                        return item.components().has(DataComponents.MAX_DAMAGE) || isKnownDamageableItemId(id);
                    } catch (RuntimeException ignored) {
                        return isKnownDamageableItemId(id);
                    }
                }
            }

            private static boolean isKnownDamageableItemId(String id) {
                return id.endsWith("_sword")
                        || id.endsWith("_pickaxe")
                        || id.endsWith("_axe")
                        || id.endsWith("_shovel")
                        || id.endsWith("_hoe")
                        || id.endsWith("_helmet")
                        || id.endsWith("_chestplate")
                        || id.endsWith("_leggings")
                        || id.endsWith("_boots")
                        || id.equals("minecraft:bow")
                        || id.equals("minecraft:crossbow")
                        || id.equals("minecraft:shield")
                        || id.equals("minecraft:fishing_rod")
                        || id.equals("minecraft:carrot_on_a_stick")
                        || id.equals("minecraft:warped_fungus_on_a_stick")
                        || id.equals("minecraft:flint_and_steel")
                        || id.equals("minecraft:shears")
                        || id.equals("minecraft:brush")
                        || id.equals("minecraft:trident")
                        || id.equals("minecraft:mace")
                        || id.equals("minecraft:elytra")
                        || id.equals("minecraft:wolf_armor");
            }
        }
    }
}
