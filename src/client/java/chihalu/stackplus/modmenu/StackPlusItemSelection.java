package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
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
        if (client.level == null && StackLimitConfig.areServerRulesActive()) {
            StackLimitConfig.endRemoteSession();
        }
        client.setScreenAndShow(new ItemSelectionScreen(parent, itemStackLimit));
    }

    private StackPlusItemSelection() {
    }

    private static final class ItemSelectionScreen extends Screen {
        private static final int SLOT_SIZE = 18;
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
        private static final int PANEL_COLOR = 0x80000000;
        private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;
        private static final int SLOT_HOVER_COLOR = 0x66FFFFFF;
        private static final int SLOT_SELECTED_COLOR = 0x8800A8FF;
        private static final long SELECTED_ITEM_DISPLAY_INTERVAL_MS = 3_000L;
        private static final int[] LIMIT_PRESETS = {64, 999, 1_000, 10_000, 32_767, 1_000_000};

        private final Screen parent;
        private final int initialStackLimit;
        private final boolean stackLimitPresetsVisible;
        private final boolean requiresWorldOrder;
        private final List<Entry> allItems;
        private final List<Entry> filteredItems = new ArrayList<>();
        private final Set<Entry> selectedEntries = new LinkedHashSet<>();
        private final Map<Entry, Integer> pendingLimits = new LinkedHashMap<>();
        private final Set<Entry> pendingAllowedEntries = new LinkedHashSet<>();
        private final Set<Entry> pendingForbiddenEntries = new LinkedHashSet<>();
        private EditBox searchBox;
        private EditBox limitInput;
        private Button stackingForbiddenButton;
        private Button previousSelectedButton;
        private Button nextSelectedButton;
        private Entry activeEntry;
        private SortMode sortMode = SortMode.CREATIVE;
        private String searchText = "";
        private boolean configuredOnly;
        private int pendingLimit;
        private int scrollRows;
        private boolean updatingLimitInput;

        private ItemSelectionScreen(Screen parent, int itemStackLimit) {
            super(Component.translatable("screen.stackplus.item_selection.title"));
            this.parent = parent;
            this.initialStackLimit = StackLimitConfig.clampStackLimit(itemStackLimit);
            this.stackLimitPresetsVisible = StackLimitConfig.areStackLimitPresetsVisible();
            this.pendingLimit = this.initialStackLimit;
            this.requiresWorldOrder = Minecraft.getInstance().level == null
                    && StackPlusCreativeOrderCache.load().isEmpty();
            this.allItems = createEntries();
            filterItems();
        }

        @Override
        protected void init() {
            int rightLeft = getRightLeft();
            int rightWidth = getRightWidth();
            int searchLeft = rightLeft + GRID_PADDING;
            int searchWidth = rightWidth - GRID_PADDING * 2 - SORT_BUTTON_WIDTH - CONFIGURED_FILTER_BUTTON_WIDTH - SEARCH_SORT_GAP * 2;
            this.searchBox = new EditBox(this.font, searchLeft, 28, searchWidth, SEARCH_HEIGHT,
                    Component.translatable("screen.stackplus.item_selection.search"));
            this.searchBox.setTooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.search")));
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
            }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.sort")))
                    .bounds(searchLeft + searchWidth + SEARCH_SORT_GAP, 28, SORT_BUTTON_WIDTH, SEARCH_HEIGHT).build());
            addRenderableWidget(Button.builder(configuredOnlyText(), button -> {
                configuredOnly = !configuredOnly;
                button.setMessage(configuredOnlyText());
                scrollRows = 0;
                filterItems();
            }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.configured_only")))
                    .bounds(searchLeft + searchWidth + SEARCH_SORT_GAP + SORT_BUTTON_WIDTH + SEARCH_SORT_GAP, 28,
                    CONFIGURED_FILTER_BUTTON_WIDTH, SEARCH_HEIGHT).build());

            int left = getLeftPanelLeft() + GRID_PADDING;
            int contentWidth = getLeftPanelWidth() - GRID_PADDING * 2;
            this.stackingForbiddenButton = Button.builder(stackingForbiddenText(false), button -> toggleStackingForbidden())
                    .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.stacking")))
                    .bounds(left, 104, contentWidth, BUTTON_HEIGHT)
                    .build();
            this.stackingForbiddenButton.active = false;
            addRenderableWidget(this.stackingForbiddenButton);

            this.previousSelectedButton = Button.builder(Component.literal("<"), button -> cycleActiveEntry(-1))
                    .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.previous")))
                    .bounds(left, 48, 20, BUTTON_HEIGHT)
                    .build();
            this.nextSelectedButton = Button.builder(Component.literal(">"), button -> cycleActiveEntry(1))
                    .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.next")))
                    .bounds(left + contentWidth - 20, 48, 20, BUTTON_HEIGHT)
                    .build();
            addRenderableWidget(this.previousSelectedButton);
            addRenderableWidget(this.nextSelectedButton);

            this.limitInput = new EditBox(this.font, left, 142, contentWidth, 20,
                    Component.translatable("screen.stackplus.item_selection.limit_input"));
            this.limitInput.setTooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.limit")));
            this.limitInput.setMaxLength(14);
            this.limitInput.setResponder(this::onLimitInputChanged);
            addRenderableWidget(limitInput);

            if (stackLimitPresetsVisible) {
                addPresetButtons(left, 166);
            }
            int backButtonY = this.height - 42;
            int halfWidth = (contentWidth - 6) / 2;
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.save"), button -> saveSelectedItem())
                    .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.save")))
                    .bounds(left, backButtonY - BUTTON_HEIGHT - 6, halfWidth, BUTTON_HEIGHT)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.reset"), button -> resetSelectedItem())
                    .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.reset")))
                    .bounds(left + halfWidth + 6, backButtonY - BUTTON_HEIGHT - 6, halfWidth, BUTTON_HEIGHT)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.back"), button -> onClose())
                    .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.settings.back")))
                    .bounds(left, backButtonY, contentWidth, BUTTON_HEIGHT)
                    .build());

            setLimitInputText(pendingLimit);
            setInitialFocus(searchBox);
            updateStackingForbiddenButton();
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            super.renderBackground(graphics, mouseX, mouseY, partialTick);
        }
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderPanels(graphics);
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

        private void drawCenteredText(GuiGraphics graphics, Component text, int centerX, int y, int color) {
            graphics.drawString(this.font, text, centerX - this.font.width(text) / 2, y, color, false);
        }
        @Override
        public void onClose() {
            Minecraft.getInstance().setScreenAndShow(parent);
        }

        private void renderPanels(GuiGraphics graphics) {
            graphics.fill(getLeftPanelLeft(), 6, getLeftPanelRight(), this.height - 6, PANEL_COLOR);
            graphics.renderOutline(getLeftPanelLeft(), 6, getLeftPanelWidth(), this.height - 12, PANEL_BORDER_COLOR);
            graphics.fill(getRightLeft(), 6, getRightRight(), this.height - 6, PANEL_COLOR);
            graphics.renderOutline(getRightLeft(), 6, getRightWidth(), this.height - 12, PANEL_BORDER_COLOR);
            drawCenteredText(graphics, Component.translatable("screen.stackplus.item_selection.selected_title"), getLeftPanelLeft() + getLeftPanelWidth() / 2, 18, 0xFFFFFFFF);
            drawCenteredText(graphics, this.title, getRightLeft() + getRightWidth() / 2, 10, 0xFFFFFFFF);
            if (isCreativeOrderUnavailable()) {
                drawCenteredText(graphics, Component.translatable("screen.stackplus.item_selection.world_required"),
                        getRightLeft() + getRightWidth() / 2, this.height / 2 - 5, 0xFFFFFFFF);
            }
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
            graphics.drawString(this.font, Component.translatable("screen.stackplus.item_selection.limit_label"), getLeftPanelLeft() + GRID_PADDING, 128, 0xFFE0E0E0, false);
        }

        private Entry getDisplayedSelectedEntry() {
            if (activeEntry != null && selectedEntries.contains(activeEntry)) {
                return activeEntry;
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
                int x = gridLeft + column * SLOT_SIZE;
                int y = gridTop + row * SLOT_SIZE;
                Entry entry = filteredItems.get(index);

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
                        .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.item_selection.preset")))
                        .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                        .build());
            }
        }

        private void selectEntry(Entry entry) {
            if (!hasShiftDown() && selectedEntries.size() > 1 && selectedEntries.contains(entry)) {
                activeEntry = entry;
                setPendingLimit(getPendingLimit(entry), true);
                updateStackingForbiddenButton();
                return;
            }
            if (hasShiftDown()) {
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
            if (entry.stack().isDamageableItem()) {
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
                    limitInput.setValue("");
                    updatingLimitInput = false;
                } else {
                    setPendingLimit(getPendingLimit(activeEntry), true);
                }
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
                limitInput.setValue("");
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
            boolean forbidden = !selectedEntries.isEmpty()
                    && selectedEntries.stream().allMatch(this::isStackingForbidden);
            stackingForbiddenButton.active = !selectedEntries.isEmpty();
            stackingForbiddenButton.setMessage(stackingForbiddenText(forbidden));
            if (limitInput != null) {
                limitInput.active = !selectedEntries.isEmpty() && !forbidden;
            }
            boolean canCycle = selectedEntries.size() > 1;
            if (previousSelectedButton != null) {
                previousSelectedButton.active = canCycle;
            }
            if (nextSelectedButton != null) {
                nextSelectedButton.active = canCycle;
            }
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
            return StackLimitConfig.getDefaultStackLimit(entry.stack(), initialStackLimit);
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
            limitInput.setValue(StackLimitConfig.formatStackLimit(value));
            updatingLimitInput = false;
        }

        private Entry getEntryAt(double mouseX, double mouseY) {
            int columns = getColumns();
            int gridLeft = getGridLeft();
            int gridTop = getGridTop();
            int column = (int) ((mouseX - gridLeft) / SLOT_SIZE);
            int row = (int) ((mouseY - gridTop) / SLOT_SIZE);
            if (column < 0 || column >= columns || row < 0 || row >= getVisibleRows()) {
                return null;
            }

            int slotX = gridLeft + column * SLOT_SIZE;
            int slotY = gridTop + row * SLOT_SIZE;
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
            if (isCreativeOrderUnavailable()) {
                return;
            }
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

        private boolean isCreativeOrderUnavailable() {
            return requiresWorldOrder && sortMode == SortMode.CREATIVE;
        }

        private Component configuredOnlyText() {
            return Component.translatable("screen.stackplus.item_selection.configured_only",
                    Component.translatable(configuredOnly ? "screen.stackplus.item_selection.configured_only.on" : "screen.stackplus.item_selection.configured_only.off"));
        }

        private static Component stackingForbiddenText(boolean forbidden) {
            return Component.translatable("screen.stackplus.item_selection.stacking_forbidden",
                    Component.translatable(forbidden ? "screen.stackplus.item_selection.stacking_forbidden.on" : "screen.stackplus.item_selection.stacking_forbidden.off"));
        }

        private void updateSearchHint() {
            if (searchBox == null) {
                return;
            }
            searchBox.setSuggestion(searchText.isEmpty()
                    ? Component.translatable("screen.stackplus.item_selection.search_hint").getString()
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
            return getRightLeft() + GRID_PADDING;
        }

        private int getGridTop() {
            return HEADER_HEIGHT;
        }

        private int getColumns() {
            return Math.max(1, (getRightWidth() - GRID_PADDING) / SLOT_SIZE);
        }

        private int getVisibleRows() {
            return Math.max(1, (this.height - HEADER_HEIGHT - FOOTER_HEIGHT) / SLOT_SIZE);
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

            List<Entry> result = new ArrayList<>(entries.values());
            if (Minecraft.getInstance().level != null) {
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
                RegistryAccess registryAccess = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
                CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                        FeatureFlags.DEFAULT_FLAGS, true, registryAccess);
                for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
                    try {
                        tab.buildContents(parameters);
                    } catch (RuntimeException exception) {
                        // 一部のModアイテムがタイトル画面で未初期化でも、他のタブの順序は維持します。
                    }
                }
            } catch (RuntimeException exception) {
                // タイトル画面でレジストリが未準備の場合はレジストリ順へフォールバックします。
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
                if (entry.isVisibleInSelection()) {
                    entries.putIfAbsent(entry.key(), entry);
                }
            } catch (RuntimeException exception) {
                // Some item components are not bound yet when opened from the title screen.
            }
        }

        private static void addEntries(Map<String, Entry> entries, Iterable<ItemStack> stacks) {
            for (ItemStack stack : stacks) {
                Entry entry = new Entry(stack.copyWithCount(1));
                if (entry.isVisibleInSelection()) {
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
                    case CREATIVE -> Component.translatable("screen.stackplus.item_selection.sort.creative").getString();
                    case ID_ASCENDING -> "ID ↑";
                    case ID_DESCENDING -> "ID ↓";
                    case NAME_ASCENDING -> "A-Z";
                    case NAME_DESCENDING -> "Z-A";
                };
            }

            private Comparator<Entry> comparator() {
                return switch (this) {
                    // クリエイティブインベントリと同じ順番を維持します。
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

            /** 耐久値付きアイテムも検索・確認できるように一覧へ表示します。 */
            private boolean isVisibleInSelection() {
                return !stack.isEmpty();
            }
        }
    }
}
