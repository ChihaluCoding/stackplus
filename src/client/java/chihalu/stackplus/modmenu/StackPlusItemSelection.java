package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null && StackLimitConfig.areServerRulesActive()) {
            StackLimitConfig.endRemoteSession();
        }
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
        private static final int CONFIGURED_FILTER_BUTTON_WIDTH = 86;
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
        private final Map<Entry, Integer> pendingLimits = new LinkedHashMap<>();
        private final Set<Entry> pendingAllowedEntries = new LinkedHashSet<>();
        private final Set<Entry> pendingForbiddenEntries = new LinkedHashSet<>();
        private TextFieldWidget searchBox;
        private TextFieldWidget limitInput;
        private ButtonWidget stackingForbiddenButton;
        private ButtonWidget previousSelectedButton;
        private ButtonWidget nextSelectedButton;
        private SortMode sortMode = SortMode.CREATIVE;
        private Entry activeEntry;
        private String searchText = "";
        private boolean configuredOnly;
        private int pendingLimit;
        private int scrollRows;
        private boolean updatingLimitInput;

        private ItemSelectionScreen(Screen parent, int itemStackLimit) {
            super(Text.translatable("screen.stackplus.item_selection.title"));
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
            int searchWidth = gridWidth - SORT_BUTTON_WIDTH - CONFIGURED_FILTER_BUTTON_WIDTH - SEARCH_SORT_GAP * 2;
            this.searchBox = new TextFieldWidget(this.textRenderer, gridLeft, 28, searchWidth, SEARCH_HEIGHT,
                    Text.translatable("screen.stackplus.item_selection.search"));
            this.searchBox.setTooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.search")));
            this.searchBox.setMaxLength(80);
            this.searchBox.setText(searchText);
            updateSearchSuggestion();
            this.searchBox.setChangedListener(value -> {
                searchText = value;
                updateSearchSuggestion();
                scrollRows = 0;
                filterItems();
            });
            addDrawableChild(searchBox);
            addDrawableChild(ButtonWidget.builder(Text.literal(sortMode.label()), button -> {
                sortMode = sortMode.next();
                button.setMessage(Text.literal(sortMode.label()));
                scrollRows = 0;
                filterItems();
            }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.sort")))
                    .dimensions(gridLeft + searchWidth + SEARCH_SORT_GAP, 28, SORT_BUTTON_WIDTH, SEARCH_HEIGHT).build());
            addDrawableChild(ButtonWidget.builder(configuredOnlyText(), button -> {
                configuredOnly = !configuredOnly;
                button.setMessage(configuredOnlyText());
                scrollRows = 0;
                filterItems();
            }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.configured_only")))
                    .dimensions(gridLeft + searchWidth + SEARCH_SORT_GAP + SORT_BUTTON_WIDTH + SEARCH_SORT_GAP, 28,
                    CONFIGURED_FILTER_BUTTON_WIDTH, SEARCH_HEIGHT).build());

            int left = getLeftPanelLeft() + GRID_PADDING;
            int contentWidth = getLeftPanelWidth() - GRID_PADDING * 2;
            this.stackingForbiddenButton = ButtonWidget.builder(stackingForbiddenText(false), button -> toggleStackingForbidden())
                    .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.stacking")))
                    .dimensions(left, 104, contentWidth, BUTTON_HEIGHT).build();
            this.stackingForbiddenButton.active = false;
            addDrawableChild(this.stackingForbiddenButton);

            this.previousSelectedButton = ButtonWidget.builder(Text.literal("<"), button -> cycleActiveEntry(-1))
                    .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.previous")))
                    .dimensions(left, 48, 20, BUTTON_HEIGHT).build();
            this.nextSelectedButton = ButtonWidget.builder(Text.literal(">"), button -> cycleActiveEntry(1))
                    .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.next")))
                    .dimensions(left + contentWidth - 20, 48, 20, BUTTON_HEIGHT).build();
            addDrawableChild(this.previousSelectedButton);
            addDrawableChild(this.nextSelectedButton);

            this.limitInput = new TextFieldWidget(this.textRenderer, left, 142, contentWidth, 20,
                    Text.translatable("screen.stackplus.item_selection.limit_input"));
            this.limitInput.setTooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.limit")));
            this.limitInput.setMaxLength(14);
            this.limitInput.setTextPredicate(ItemSelectionScreen::isValidLimitInput);
            this.limitInput.setText(String.valueOf(pendingLimit));
            this.limitInput.setChangedListener(this::onLimitInputChanged);
            addDrawableChild(limitInput);

            if (stackLimitPresetsVisible) {
                addPresetButtons(left, 166);
            }
            int backButtonY = this.height - 42;
            int halfWidth = (contentWidth - 6) / 2;
            addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.save"), button -> saveSelectedItem())
                    .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.save")))
                    .dimensions(left, backButtonY - BUTTON_HEIGHT - 6, halfWidth, BUTTON_HEIGHT)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.reset"), button -> resetSelectedItem())
                    .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.reset")))
                    .dimensions(left + halfWidth + 6, backButtonY - BUTTON_HEIGHT - 6, halfWidth, BUTTON_HEIGHT)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.back"), button -> close())
                    .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.settings.back")))
                    .dimensions(left, backButtonY, contentWidth, BUTTON_HEIGHT)
                    .build());

            setLimitInputText(pendingLimit);
            setInitialFocus(searchBox);
            updateStackingForbiddenButton();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            renderSelectedItem(context);
            renderItems(context, mouseX, mouseY);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (super.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            if (button != LEFT_MOUSE_BUTTON) {
                return false;
            }

            Entry entry = getEntryAt(mouseX, mouseY);
            if (entry == null) {
                return false;
            }

            selectEntry(entry);
            return true;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (!isMouseInRightPanel(mouseX, mouseY)) {
                return false;
            }

            int maxScrollRows = Math.max(0, getTotalRows() - getVisibleRows());
            scrollRows = Math.max(0, Math.min(maxScrollRows, scrollRows - (int) Math.signum(verticalAmount)));
            return true;
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }

        @Override
        public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
            // 標準背景の後、ウィジェットより前にパネルを描き、後段背景による上書きを防ぐ。
            renderStackPlusBackground(context, delta);
            renderPanels(context);
        }

        private void renderStackPlusBackground(DrawContext context, float delta) {
            if (this.client.world == null) {
                this.renderPanoramaBackground(context, delta);
            }
            this.applyBlur(delta);
            this.renderInGameBackground(context);
        }

        private void renderPanels(DrawContext context) {
            context.fill(getLeftPanelLeft(), 6, getLeftPanelRight(), this.height - 6, PANEL_COLOR);
            drawPanelBorder(context, getLeftPanelLeft(), 6, getLeftPanelWidth(), this.height - 12);
            context.fill(getRightLeft(), 6, getRightRight(), this.height - 6, PANEL_COLOR);
            drawPanelBorder(context, getRightLeft(), 6, getRightWidth(), this.height - 12);
            drawCenteredText(context, Text.translatable("screen.stackplus.item_selection.selected_title"), getLeftPanelLeft() + getLeftPanelWidth() / 2, 18, 0xFFFFFFFF);
            drawCenteredText(context, this.title, getRightLeft() + getRightWidth() / 2, 10, 0xFFFFFFFF);
        }

        private void renderSelectedItem(DrawContext context) {
            int centerX = getLeftPanelLeft() + getLeftPanelWidth() / 2;
            if (selectedEntries.isEmpty()) {
                drawCenteredText(context, Text.translatable("screen.stackplus.item_selection.no_selection"), centerX, 74, 0xFFE0E0E0);
                return;
            }

            Entry displayedEntry = getDisplayedSelectedEntry();
            context.drawItem(displayedEntry.stack(), centerX - 8, 48);
            drawCenteredText(context, displayedEntry.stack().getName(), centerX, 72, 0xFFFFFFFF);
            drawCenteredText(context, Text.literal(displayedEntry.id()), centerX, 88, 0xFFB8B8B8);
            context.drawText(this.textRenderer, Text.translatable("screen.stackplus.item_selection.limit_label"), getLeftPanelLeft() + GRID_PADDING, 128, 0xFFE0E0E0, false);
        }

        private Entry getDisplayedSelectedEntry() {
            if (activeEntry != null && selectedEntries.contains(activeEntry)) {
                return activeEntry;
            }
            return selectedEntries.iterator().next();
        }

        private void renderItems(DrawContext context, int mouseX, int mouseY) {
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

                context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_BACKGROUND_COLOR);
                if (selectedEntries.contains(entry)) {
                    context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_SELECTED_COLOR);
                } else if (isMouseInSlot(mouseX, mouseY, x, y)) {
                    context.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, SLOT_HOVER_COLOR);
                    context.drawTooltip(this.textRenderer, entry.stack().getName(), mouseX, mouseY);
                }
                context.drawItem(entry.stack(), x + ITEM_OFFSET, y + ITEM_OFFSET);
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
                addDrawableChild(ButtonWidget.builder(Text.literal(formatPreset(value)), button -> setPendingLimit(value, true))
                        .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.item_selection.preset")))
                        .dimensions(x, y, buttonWidth, BUTTON_HEIGHT)
                        .build());
            }
        }

        private void selectEntry(Entry entry) {
            if (!Screen.hasShiftDown() && selectedEntries.size() > 1 && selectedEntries.contains(entry)) {
                activeEntry = entry;
                setPendingLimit(getPendingLimit(entry), true);
                updateStackingForbiddenButton();
                return;
            }
            if (Screen.hasShiftDown()) {
                toggleSelectedEntry(entry);
            } else {
                selectedEntries.clear();
                pendingLimits.clear();
                pendingAllowedEntries.clear();
                pendingForbiddenEntries.clear();
                selectedEntries.add(entry);
            }
            if (!selectedEntries.contains(entry)) {
                activeEntry = selectedEntries.isEmpty() ? null : selectedEntries.iterator().next();
                updateStackingForbiddenButton();
                return;
            }
            activeEntry = entry;
            setPendingLimit(getPendingLimit(entry), true);
            updateStackingForbiddenButton();
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
            if (selectedEntries.stream().anyMatch(entry -> getPendingLimit(entry) > 1 && isPotentiallyDamageable(entry))
                    && !StackLimitConfig.isDurabilityWarningSuppressed()) {
                StackPlusDurabilityWarningScreen.open(this, this::saveSelectedItemNow);
                return;
            }
            saveSelectedItemNow();
        }

        private static boolean isPotentiallyDamageable(Entry entry) {
            if (entry.stack().isDamageable()) {
                return true;
            }
            String id = entry.id();
            return id.contains("sword") || id.contains("pickaxe") || id.contains("axe")
                    || id.contains("shovel") || id.contains("hoe") || id.contains("bow")
                    || id.contains("crossbow") || id.contains("trident") || id.contains("mace")
                    || id.contains("shears") || id.contains("fishing_rod") || id.contains("flint_and_steel")
                    || id.contains("brush") || id.contains("helmet") || id.contains("chestplate")
                    || id.contains("leggings") || id.contains("boots") || id.contains("elytra");
        }

        private void saveSelectedItemNow() {
            selectedEntries.forEach(entry -> {
                int limit = getPendingLimit(entry);
                boolean forbidden = isStackingForbidden(entry);
                if (forbidden) {
                    if (isPotentiallyDamageable(entry)
                            && limit == StackLimitConfig.getDefaultStackLimit(entry.stack(), initialStackLimit)) {
                        StackLimitConfig.removeItemStackLimit(entry.item());
                    } else {
                        StackLimitConfig.setItemStackingForbidden(entry.item(), true);
                    }
                } else {
                    StackLimitConfig.setItemStackingForbidden(entry.item(), false);
                    if (isPotentiallyDamageable(entry)) {
                        StackLimitConfig.saveItemStackLimit(entry.item(), limit);
                    } else if (limit == StackLimitConfig.getDefaultStackLimit(entry.stack(), initialStackLimit)) {
                        StackLimitConfig.removeItemStackLimit(entry.item());
                    } else {
                        StackLimitConfig.saveStackVariantLimits(List.of(entry.stack()), limit);
                    }
                }
            });
            pendingLimits.clear();
            pendingAllowedEntries.removeAll(selectedEntries);
            pendingForbiddenEntries.removeAll(selectedEntries);
            if (configuredOnly) {
                selectedEntries.removeIf(entry -> !isConfiguredForFilter(entry));
                if (activeEntry == null || !selectedEntries.contains(activeEntry)) {
                    activeEntry = selectedEntries.stream().findFirst().orElse(null);
                }
                if (activeEntry == null) {
                    updatingLimitInput = true;
                    limitInput.setText("");
                    updatingLimitInput = false;
                } else {
                    setPendingLimit(getPendingLimit(activeEntry), true);
                }
            }
            if (configuredOnly) {
                filterItems();
            }
            updateStackingForbiddenButton();
        }

        private void resetSelectedItem() {
            if (selectedEntries.isEmpty()) {
                return;
            }
            StackLimitConfig.removeItemStackLimits(selectedEntries.stream().map(Entry::item).toList());
            pendingLimits.clear();
            pendingAllowedEntries.removeAll(selectedEntries);
            pendingForbiddenEntries.removeAll(selectedEntries);
            if (configuredOnly) {
                selectedEntries.clear();
                activeEntry = null;
                updatingLimitInput = true;
                limitInput.setText("");
                updatingLimitInput = false;
                updateStackingForbiddenButton();
                filterItems();
                return;
            }
            setPendingLimit(getResetStackLimit(getDisplayedSelectedEntry()), true);
            updateStackingForbiddenButton();
        }

        private void toggleStackingForbidden() {
            if (selectedEntries.isEmpty()) {
                return;
            }
            boolean forbid = selectedEntries.stream().anyMatch(entry -> !isStackingForbidden(entry));
            if (forbid) {
                pendingAllowedEntries.removeAll(selectedEntries);
                pendingForbiddenEntries.addAll(selectedEntries);
                selectedEntries.forEach(entry -> pendingLimits.put(entry, 1));
                setPendingLimitForActiveEntry(1);
            } else {
                pendingForbiddenEntries.removeAll(selectedEntries);
                pendingAllowedEntries.addAll(selectedEntries);
                selectedEntries.forEach(entry -> pendingLimits.put(entry, Math.max(initialStackLimit, 2)));
                setPendingLimitForActiveEntry(Math.max(initialStackLimit, 2));
            }
            updateStackingForbiddenButton();
        }

        private void updateStackingForbiddenButton() {
            if (stackingForbiddenButton == null) {
                return;
            }
            boolean forbidden = !selectedEntries.isEmpty() && selectedEntries.stream().allMatch(this::isStackingForbidden);
            stackingForbiddenButton.active = !selectedEntries.isEmpty();
            stackingForbiddenButton.setMessage(stackingForbiddenText(forbidden));
            if (limitInput != null) {
                limitInput.active = !selectedEntries.isEmpty() && !forbidden;
            }
            boolean canCycle = selectedEntries.size() > 1;
            previousSelectedButton.active = canCycle;
            nextSelectedButton.active = canCycle;
        }

        private boolean isStackingForbidden(Entry entry) {
            if (pendingForbiddenEntries.contains(entry)) {
                return true;
            }
            if (pendingAllowedEntries.contains(entry)) {
                return false;
            }
            if (isPotentiallyDamageable(entry)
                    && !StackLimitConfig.hasConfiguredStackLimit(entry.stack())
                    ) {
                return true;
            }
            return StackLimitConfig.isItemStackingForbidden(entry.item());
        }

        private int getPendingLimit(Entry entry) {
            Integer pending = pendingLimits.get(entry);
            if (pending != null) {
                return pending;
            }
            if (isStackingForbidden(entry)) {
                return 1;
            }
            return StackLimitConfig.getAdjustedStackLimit(entry.stack(), initialStackLimit);
        }

        private int getResetStackLimit(Entry entry) {
            return StackLimitConfig.getSafeStackCountLimit(entry.stack(), initialStackLimit);
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
            Integer parsedValue = parseLimitInput(limitInput.getText());
            if (parsedValue == null) {
                setLimitInputText(pendingLimit);
                return;
            }

            setPendingLimit(parsedValue, true);
        }

        private void cycleActiveEntry(int direction) {
            if (selectedEntries.isEmpty()) {
                return;
            }
            syncPendingLimitFromInput();
            List<Entry> entries = new ArrayList<>(selectedEntries);
            int currentIndex = activeEntry == null ? -1 : entries.indexOf(activeEntry);
            activeEntry = entries.get((currentIndex + direction + entries.size()) % entries.size());
            setPendingLimit(getPendingLimit(activeEntry), true);
            updateStackingForbiddenButton();
        }

        private void setPendingLimit(int value, boolean updateInput) {
            pendingLimit = StackLimitConfig.clampStackLimit(value);
            if (activeEntry != null && selectedEntries.contains(activeEntry)) {
                pendingLimits.put(activeEntry, pendingLimit);
            }
            if (updateInput && limitInput != null) {
                setLimitInputText(pendingLimit);
            }
        }

        private void setPendingLimitForActiveEntry(int value) {
            setPendingLimit(value, true);
        }

        private void setLimitInputText(int value) {
            updatingLimitInput = true;
            limitInput.setText(StackLimitConfig.formatStackLimit(value));
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
                if ((query.isEmpty() || entry.id().contains(query) || entry.name().contains(query))
                        && (!configuredOnly || isConfiguredForFilter(entry))) {
                    filteredItems.add(entry);
                }
            }
            filteredItems.sort(sortMode.comparator());
        }

        private boolean isConfiguredForFilter(Entry entry) {
            return StackLimitConfig.hasConfiguredStackLimit(entry.stack())
                    && (!isPotentiallyDamageable(entry) || !isStackingForbidden(entry));
        }

        private Text configuredOnlyText() {
            return Text.translatable("screen.stackplus.item_selection.configured_only",
                    Text.translatable(configuredOnly ? "screen.stackplus.item_selection.configured_only.on" : "screen.stackplus.item_selection.configured_only.off"));
        }

        private static Text stackingForbiddenText(boolean forbidden) {
            return Text.translatable("screen.stackplus.item_selection.stacking_forbidden",
                    Text.translatable(forbidden ? "screen.stackplus.item_selection.stacking_forbidden.on" : "screen.stackplus.item_selection.stacking_forbidden.off"));
        }

        private void updateSearchSuggestion() {
            if (searchBox == null) {
                return;
            }
            searchBox.setSuggestion(searchText.isEmpty()
                    ? Text.translatable("screen.stackplus.item_selection.search_hint").getString()
                    : "");
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

        private void drawPanelBorder(DrawContext context, int x, int y, int width, int height) {
            context.fill(x, y, x + width, y + 1, PANEL_BORDER_COLOR);
            context.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER_COLOR);
            context.fill(x, y, x + 1, y + height, PANEL_BORDER_COLOR);
            context.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER_COLOR);
        }

        private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
            int x = centerX - this.textRenderer.getWidth(text) / 2;
            context.drawText(this.textRenderer, text, x, y, color, false);
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

        // クリエイティブタブの順序を集約し、タブ外の登録アイテムを末尾へ補完する。
        private static List<Entry> createEntries() {
            Map<String, Entry> entries = new LinkedHashMap<>();
            rebuildCreativeTabs();
            for (ItemGroup group : ItemGroups.getGroupsToDisplay()) {
                addCreativeEntries(entries, group);
            }
            Registries.ITEM.stream().forEach(item -> addEntry(entries, new Entry(item)));
            List<Entry> result = new ArrayList<>(entries.values());
            if (MinecraftClient.getInstance().world != null) {
                StackPlusCreativeOrderCache.save(result.stream().map(Entry::key).toList());
            } else {
                result = applyCachedCreativeOrder(result);
            }
            return result;
        }

        private static List<Entry> applyCachedCreativeOrder(List<Entry> entries) {
            List<String> cachedOrder = StackPlusCreativeOrderCache.load();
            if (cachedOrder.isEmpty()) {
                return entries;
            }
            Map<String, Entry> entriesByKey = new LinkedHashMap<>();
            entries.forEach(entry -> entriesByKey.put(entry.key(), entry));
            List<Entry> orderedEntries = new ArrayList<>();
            for (String key : cachedOrder) {
                Entry entry = entriesByKey.remove(key);
                if (entry != null) {
                    orderedEntries.add(entry);
                }
            }
            orderedEntries.addAll(entriesByKey.values());
            return orderedEntries;
        }

        private static void rebuildCreativeTabs() {
            try {
                var registryLookup = DynamicRegistryManager.of(Registries.REGISTRIES);
                var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
                if (networkHandler != null) {
                    registryLookup = networkHandler.getRegistryManager();
                }
                ItemGroups.updateDisplayContext(FeatureFlags.DEFAULT_ENABLED_FEATURES, true, registryLookup);
            } catch (RuntimeException exception) {
                // タイトル画面などで動的レジストリが未構築の場合は、既存内容と登録順で補完する。
            }
        }

        private static void addCreativeEntries(Map<String, Entry> entries, ItemGroup group) {
            try {
                for (ItemStack stack : group.getDisplayStacks()) {
                    addEntry(entries, new Entry(stack.copyWithCount(1)));
                }
            } catch (IllegalStateException exception) {
                // クリエイティブタブが未構築の場合はレジストリ側の補完へ進む。
            }
        }

        private static void addEntry(Map<String, Entry> entries, Entry entry) {
            if (entry.isVisibleInSelection()) {
                entries.putIfAbsent(entry.key(), entry);
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
                    case CREATIVE -> Text.translatable("screen.stackplus.item_selection.sort.creative").getString();
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
            private Entry(Item item) {
                this(item, new ItemStack(item));
            }

            private Entry(ItemStack stack) {
                this(stack.getItem(), stack);
            }

            private Entry(Item item, ItemStack stack) {
                this(item, stack, getItemId(item), stack.getName().getString().toLowerCase(Locale.ROOT), getEntryKey(item, stack));
            }

            private static String getItemId(Item item) {
                Identifier id = Registries.ITEM.getId(item);
                return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
            }

            private static String getEntryKey(Item item, ItemStack stack) {
                String itemId = getItemId(item);
                try {
                    if (ItemStack.areItemsAndComponentsEqual(new ItemStack(item), stack)) {
                        return itemId;
                    }
                    return itemId + "#" + ItemStack.hashCode(stack);
                } catch (RuntimeException exception) {
                    return itemId;
                }
            }

            /** 耐久値付きアイテムも検索・確認できるように一覧へ表示します。 */
            private boolean isVisibleInSelection() {
                return !stack.isEmpty();
            }
        }
    }
}
