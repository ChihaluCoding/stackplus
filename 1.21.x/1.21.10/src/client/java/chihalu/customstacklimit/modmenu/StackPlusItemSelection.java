package chihalu.customstacklimit.modmenu;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.lwjgl.glfw.GLFW;

public final class StackPlusItemSelection {
    private static final int LEFT_MOUSE_BUTTON = 0;

    public static void start(Screen parent, int itemStackLimit) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new ItemSelectionScreen(parent, itemStackLimit));
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
        private static final int HEADER_HEIGHT = 54;
        private static final int FOOTER_HEIGHT = 14;
        private static final int BUTTON_HEIGHT = 20;
        private static final int PANEL_COLOR = 0xB0000000;
        private static final int PANEL_BORDER_COLOR = 0x66FFFFFF;
        private static final int SLOT_HOVER_COLOR = 0x66FFFFFF;
        private static final int SLOT_SELECTED_COLOR = 0x8800A8FF;
        private static final long SELECTED_ITEM_DISPLAY_INTERVAL_MS = 3_000L;
        private static final int[] LIMIT_PRESETS = {64, 999, 1_000, 10_000, 32_767, 1_000_000};

        private final Screen parent;
        private final int initialStackLimit;
        private final List<Entry> allItems;
        private final List<Entry> filteredItems = new ArrayList<>();
        private final Set<Entry> selectedEntries = new LinkedHashSet<>();
        private TextFieldWidget searchBox;
        private TextFieldWidget limitInput;
        private Entry selectedEntry;
        private String searchText = "";
        private int pendingLimit;
        private int scrollRows;
        private boolean updatingLimitInput;
        private boolean stackplus$clickPending;
        private boolean stackplus$mouseWasPressed;
        private double stackplus$clickX;
        private double stackplus$clickY;

        private ItemSelectionScreen(Screen parent, int itemStackLimit) {
            super(Text.translatable("screen.stackplus.item_selection.title"));
            this.parent = parent;
            this.initialStackLimit = StackLimitConfig.clampStackLimit(itemStackLimit);
            this.pendingLimit = this.initialStackLimit;
            this.allItems = Registries.ITEM.stream()
                    .map(Entry::new)
                    .filter(Entry::isConfigurable)
                    .sorted(Comparator.comparing(Entry::id))
                    .toList();
            filterItems();
        }

        @Override
        protected void init() {
            int rightLeft = getRightLeft();
            int rightWidth = getRightWidth();
            this.searchBox = new TextFieldWidget(this.textRenderer, rightLeft + GRID_PADDING, 28, rightWidth - GRID_PADDING * 2, SEARCH_HEIGHT,
                    Text.translatable("screen.stackplus.item_selection.search"));
            this.searchBox.setMaxLength(80);
            this.searchBox.setSuggestion(Text.translatable("screen.stackplus.item_selection.search_hint").getString());
            this.searchBox.setText(searchText);
            this.searchBox.setChangedListener(value -> {
                searchText = value;
                scrollRows = 0;
                filterItems();
            });
            addDrawableChild(searchBox);

            int left = getLeftPanelLeft() + GRID_PADDING;
            int contentWidth = getLeftPanelWidth() - GRID_PADDING * 2;
            this.limitInput = new TextFieldWidget(this.textRenderer, left, 118, contentWidth, 20,
                    Text.translatable("screen.stackplus.item_selection.limit_input"));
            this.limitInput.setMaxLength(14);
            this.limitInput.setTextPredicate(ItemSelectionScreen::isValidLimitInput);
            this.limitInput.setText(String.valueOf(pendingLimit));
            this.limitInput.setChangedListener(this::onLimitInputChanged);
            addDrawableChild(limitInput);

            addPresetButtons(left, 142);
            int backButtonY = this.height - 42;
            addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.save"), button -> saveSelectedItem())
                    .dimensions(left, backButtonY - BUTTON_HEIGHT - 6, contentWidth, BUTTON_HEIGHT)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.back"), button -> close())
                    .dimensions(left, backButtonY, contentWidth, BUTTON_HEIGHT)
                    .build());

            setLimitInputText(pendingLimit);
            setInitialFocus(searchBox);
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            long window = MinecraftClient.getInstance().getWindow().getHandle();
            boolean mousePressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            if (mousePressed && !stackplus$mouseWasPressed) {
                stackplus$clickPending = true;
                stackplus$clickX = mouseX;
                stackplus$clickY = mouseY;
            }
            stackplus$mouseWasPressed = mousePressed;
            renderPanels(context);
            super.render(context, mouseX, mouseY, delta);
            renderSelectedItem(context);
            renderItems(context, mouseX, mouseY);
            handlePendingClick();
        }

        private void handlePendingClick() {
            if (!stackplus$clickPending) {
                return;
            }
            stackplus$clickPending = false;
            Entry entry = getEntryAt(stackplus$clickX, stackplus$clickY);
            if (entry != null) {
                selectEntry(entry);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
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

        private void renderPanels(DrawContext context) {
            context.fill(getLeftPanelLeft(), 6, getLeftPanelRight(), this.height - 6, PANEL_COLOR);
            stackplus_drawBorder(context, getLeftPanelLeft(), 6, getLeftPanelWidth(), this.height - 12, PANEL_BORDER_COLOR);
            context.fill(getRightLeft(), 6, getRightRight(), this.height - 6, PANEL_COLOR);
            stackplus_drawBorder(context, getRightLeft(), 6, getRightWidth(), this.height - 12, PANEL_BORDER_COLOR);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.item_selection.selected_title"), getLeftPanelLeft() + getLeftPanelWidth() / 2, 18, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, getRightLeft() + getRightWidth() / 2, 10, 0xFFFFFFFF);
        }

        private void renderSelectedItem(DrawContext context) {
            int centerX = getLeftPanelLeft() + getLeftPanelWidth() / 2;
            if (selectedEntries.isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.item_selection.no_selection"), centerX, 74, 0xFFE0E0E0);
                return;
            }

            Entry displayedEntry = getDisplayedSelectedEntry();
            context.drawItem(displayedEntry.stack(), centerX - 8, 48);
            context.drawCenteredTextWithShadow(this.textRenderer, displayedEntry.stack().getName(), centerX, 72, 0xFFFFFFFF);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(displayedEntry.id()), centerX, 88, 0xFFB8B8B8);
            context.drawTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.item_selection.limit_label"), getLeftPanelLeft() + GRID_PADDING, 104, 0xFFE0E0E0);
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
                int x = gridLeft + column * SLOT_SIZE;
                int y = gridTop + row * SLOT_SIZE;
                Entry entry = filteredItems.get(index);

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
                        .dimensions(x, y, buttonWidth, BUTTON_HEIGHT)
                        .build());
            }
        }

        private void selectEntry(Entry entry) {
            if (hasShiftDown()) {
                toggleSelectedEntry(entry);
            } else {
                selectedEntries.clear();
                selectedEntries.add(entry);
                selectedEntry = entry;
            }
            setPendingLimit(StackLimitConfig.getAdjustedStackLimit(entry.item(), initialStackLimit), true);
        }

        private void toggleSelectedEntry(Entry entry) {
            if (!selectedEntries.add(entry)) {
                selectedEntries.remove(entry);
            }
            selectedEntry = selectedEntries.stream().reduce((first, second) -> second).orElse(null);
        }

        private void saveSelectedItem() {
            if (selectedEntries.isEmpty()) {
                return;
            }

            syncPendingLimitFromInput();
            StackLimitConfig.saveItemStackLimits(selectedEntries.stream().map(Entry::item).toList(), pendingLimit);
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

        private void setPendingLimit(int value, boolean updateInput) {
            pendingLimit = StackLimitConfig.clampStackLimit(value);
            if (updateInput && limitInput != null) {
                setLimitInputText(pendingLimit);
            }
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
            for (Entry entry : allItems) {
                if (query.isEmpty() || entry.id().contains(query) || entry.name().contains(query)) {
                    filteredItems.add(entry);
                }
            }
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
            return Math.max(1, (getRightWidth() - GRID_PADDING * 2) / SLOT_SIZE);
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

            private static void stackplus_drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y, x + 1, y + height, color);
        context.fill(x + width - 1, y, x + width, y + height, color);
    }

    private static boolean hasShiftDown() {
        long window = net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
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

        private record Entry(Item item, ItemStack stack, String id, String name) {
            private Entry(Item item) {
                this(item, new ItemStack(item));
            }

            private Entry(Item item, ItemStack stack) {
                this(item, stack, getItemId(item), stack.getName().getString().toLowerCase(Locale.ROOT));
            }

            private static String getItemId(Item item) {
                Identifier id = Registries.ITEM.getId(item);
                return id == null ? "" : id.toString().toLowerCase(Locale.ROOT);
            }

            private boolean isConfigurable() {
                return !stack.isEmpty() && stack.getMaxCount() > 1;
            }
        }
    }
}
