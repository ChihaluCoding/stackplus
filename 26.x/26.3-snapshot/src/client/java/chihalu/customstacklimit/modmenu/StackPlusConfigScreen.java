package chihalu.customstacklimit.modmenu;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class StackPlusConfigScreen extends Screen {
    private static final double MIN_LOG = Math.log10(StackLimitConfig.MIN_STACK_LIMIT);
    private static final double MAX_LOG = Math.log10(StackLimitConfig.MAX_STACK_LIMIT);
    private static final int TITLE_Y = 12;
    private static final int MIN_CONTENT_Y = 36;
    private static final int CONTENT_CENTER_OFFSET = 154;
    private static final int SECTION_LABEL_OFFSET = 14;
    private static final int SLIDER_OFFSET = 42;
    private static final int PRESET_LABEL_OFFSET = 76;
    private static final int PRESET_OFFSET = 92;
    private static final int HINT_OFFSET = 156;
    private static final int DISPLAY_MODE_OFFSET = 194;
    private static final int DISPLAY_DESCRIPTION_OFFSET = 218;
    private static final int ACTION_BUTTON_OFFSET = 242;
    private static final int PANEL_HEIGHT = 304;
    private static final int COMPACT_PANEL_HEIGHT = 232;
    private static final int COMPACT_SECTION_LABEL_OFFSET = 10;
    private static final int COMPACT_SLIDER_OFFSET = 32;
    private static final int COMPACT_PRESET_LABEL_OFFSET = 58;
    private static final int COMPACT_PRESET_OFFSET = 72;
    private static final int COMPACT_HINT_OFFSET = 126;
    private static final int COMPACT_DISPLAY_MODE_OFFSET = 154;
    private static final int COMPACT_DISPLAY_DESCRIPTION_OFFSET = 178;
    private static final int COMPACT_ACTION_BUTTON_OFFSET = 202;
    private static final int BOTTOM_MARGIN = 8;
    private static final int CONTENT_WIDTH = 360;
    private static final int INPUT_WIDTH = 112;
    private static final int CONTROL_GAP = 8;
    private static final int PRESET_COLUMNS = 4;
    private static final int ACTION_BUTTON_WIDTH = 100;
    private static final int ACTION_BUTTON_GAP = 8;
    private static final int PANEL_COLOR = 0x80000000;
    private static final int PANEL_BORDER_COLOR = 0x66FFFFFF;
    private static final int[] STACK_LIMIT_PRESETS = {64, 999, 1_000, 10_000, 32_767, 1_000_000, 100_000_000, 1_000_000_000};
    private static final String[] STACK_LIMIT_PRESET_KEYS = {
            "screen.stackplus.config.preset.64",
            "screen.stackplus.config.preset.999",
            "screen.stackplus.config.preset.1k",
            "screen.stackplus.config.preset.10k",
            "screen.stackplus.config.preset.32k",
            "screen.stackplus.config.preset.1m",
            "screen.stackplus.config.preset.100m",
            "screen.stackplus.config.preset.1b"
    };

    private final Screen parent;
    private int pendingStackLimit;
    private StackLimitConfig.DisplayMode pendingDisplayMode;
    private boolean pendingSelectedItemCountAlwaysVisible;
    private StackLimitSlider stackLimitSlider;
    private EditBox stackLimitInput;
    private Button displayModeButton;
    private Button selectedItemCountButton;
    private boolean updatingStackLimitInput;

    public StackPlusConfigScreen(Screen parent) {
        super(Component.translatable("screen.stackplus.config.title"));
        this.parent = parent;
        this.pendingStackLimit = StackLimitConfig.getStackLimit();
        this.pendingDisplayMode = StackLimitConfig.getDisplayMode();
        this.pendingSelectedItemCountAlwaysVisible = StackLimitConfig.isSelectedItemCountAlwaysVisible();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int controlWidth = getControlWidth();
        int sliderWidth = Math.max(132, controlWidth - INPUT_WIDTH - CONTROL_GAP);
        int left = centerX - controlWidth / 2;
        Layout layout = getLayout();
        int secondaryButtonWidth = (controlWidth - CONTROL_GAP) / 2;

        addRenderableWidget(new PanelWidget(left - 12, layout.contentY(), controlWidth + 24, layout.panelHeight()));

        this.stackLimitSlider = new StackLimitSlider(left, layout.sliderY(), sliderWidth, 20, pendingStackLimit,
                value -> setPendingStackLimit(value, false, true));
        addRenderableWidget(this.stackLimitSlider);

        this.stackLimitInput = new EditBox(this.font, left + sliderWidth + CONTROL_GAP, layout.sliderY(), INPUT_WIDTH, 20, stackLimitText(pendingStackLimit));
        this.stackLimitInput.setMaxLength(14);
        this.stackLimitInput.setResponder(this::onStackLimitInputChanged);
        setStackLimitInputText(pendingStackLimit);
        addRenderableWidget(this.stackLimitInput);

        addPresetButtons(left, layout.presetY(), controlWidth);

        this.displayModeButton = Button.builder(displayModeText(pendingDisplayMode), button -> {
                    pendingDisplayMode = pendingDisplayMode.next();
                    button.setMessage(displayModeText(pendingDisplayMode));
                })
                .bounds(left, layout.displayModeY(), secondaryButtonWidth, 20)
                .build();
        addRenderableWidget(this.displayModeButton);

        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.item_limits"), button -> {
                    syncPendingStackLimitFromInput();
                    StackPlusItemSelection.start(this, pendingStackLimit);
                })
                .bounds(left + secondaryButtonWidth + CONTROL_GAP, layout.displayModeY(), secondaryButtonWidth, 20)
                .build());

        this.selectedItemCountButton = Button.builder(selectedItemCountVisibilityText(pendingSelectedItemCountAlwaysVisible), button -> {
                    pendingSelectedItemCountAlwaysVisible = !pendingSelectedItemCountAlwaysVisible;
                    button.setMessage(selectedItemCountVisibilityText(pendingSelectedItemCountAlwaysVisible));
                })
                .bounds(left, layout.displayDescriptionY(), controlWidth, 20)
                .build();
        addRenderableWidget(this.selectedItemCountButton);

        int firstButtonX = centerX - (ACTION_BUTTON_WIDTH * 3 + ACTION_BUTTON_GAP * 2) / 2;
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.save"), button -> save())
                .bounds(firstButtonX, layout.actionButtonY(), ACTION_BUTTON_WIDTH, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.reset"), button -> {
                    setPendingStackLimit(StackLimitConfig.DEFAULT_STACK_LIMIT, true, true);
                    pendingDisplayMode = StackLimitConfig.DisplayMode.COMPACT;
                    displayModeButton.setMessage(displayModeText(pendingDisplayMode));
                    pendingSelectedItemCountAlwaysVisible = false;
                    selectedItemCountButton.setMessage(selectedItemCountVisibilityText(pendingSelectedItemCountAlwaysVisible));
                })
                .bounds(firstButtonX + ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP, layout.actionButtonY(), ACTION_BUTTON_WIDTH, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.back"), button -> onClose())
                .bounds(firstButtonX + (ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP) * 2, layout.actionButtonY(), ACTION_BUTTON_WIDTH, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Layout layout = getLayout();
        graphics.centeredText(this.font, this.title, this.width / 2, TITLE_Y, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.config.stack_limit_label"), this.width / 2, layout.sectionLabelY(), 0xFFE0E0E0);
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.config.presets_label"), this.width / 2, layout.presetLabelY(), 0xFFE0E0E0);
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.config.existing_stack_hint"), this.width / 2, layout.hintY(), 0xFFB8B8B8);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    private void save() {
        syncPendingStackLimitFromInput();
        saveUnchecked();
    }

    void saveUnchecked() {
        StackLimitConfig.saveSettings(pendingStackLimit, pendingDisplayMode, pendingSelectedItemCountAlwaysVisible);
    }

    private static Component stackLimitText(int stackLimit) {
        return Component.translatable("screen.stackplus.config.stack_limit", StackLimitConfig.formatStackLimit(stackLimit));
    }

    private static Component displayModeText(StackLimitConfig.DisplayMode displayMode) {
        String modeKey = displayMode == StackLimitConfig.DisplayMode.PLUS_99
                ? "screen.stackplus.config.display_mode.99_plus"
                : "screen.stackplus.config.display_mode.compact";
        return Component.translatable("screen.stackplus.config.display_mode", Component.translatable(modeKey));
    }

    private static Component selectedItemCountVisibilityText(boolean alwaysVisible) {
        String modeKey = alwaysVisible
                ? "screen.stackplus.config.selected_item_count.always"
                : "screen.stackplus.config.selected_item_count.fade";
        return Component.translatable("screen.stackplus.config.selected_item_count", Component.translatable(modeKey));
    }

    private int getContentY() {
        int naturalY = Math.max(MIN_CONTENT_Y, this.height / 2 - CONTENT_CENTER_OFFSET);
        int lowestFittingY = this.height - COMPACT_PANEL_HEIGHT - BOTTOM_MARGIN;
        return Math.max(28, Math.min(naturalY, lowestFittingY));
    }

    private int getControlWidth() {
        return Math.min(CONTENT_WIDTH, this.width - 48);
    }

    private void addPresetButtons(int left, int presetY, int controlWidth) {
        int gap = 6;
        int buttonWidth = (controlWidth - gap * (PRESET_COLUMNS - 1)) / PRESET_COLUMNS;

        for (int index = 0; index < STACK_LIMIT_PRESETS.length; index++) {
            int column = index % PRESET_COLUMNS;
            int row = index / PRESET_COLUMNS;
            int x = left + column * (buttonWidth + gap);
            int y = presetY + row * 22;
            int presetValue = STACK_LIMIT_PRESETS[index];
            addRenderableWidget(Button.builder(Component.translatable(STACK_LIMIT_PRESET_KEYS[index]), button -> setPendingStackLimit(presetValue, true, true))
                    .bounds(x, y, buttonWidth, 20)
                    .build());
        }
    }

    private void setPendingStackLimit(int value, boolean updateSlider, boolean updateInput) {
        pendingStackLimit = StackLimitConfig.clampStackLimit(value);
        if (updateSlider && stackLimitSlider != null) {
            stackLimitSlider.setStackLimit(pendingStackLimit);
        }
        if (updateInput && stackLimitInput != null) {
            setStackLimitInputText(pendingStackLimit);
        }
    }

    private void setStackLimitInputText(int stackLimit) {
        updatingStackLimitInput = true;
        stackLimitInput.setValue(StackLimitConfig.formatStackLimit(stackLimit));
        updatingStackLimitInput = false;
    }

    private void onStackLimitInputChanged(String input) {
        if (updatingStackLimitInput || !isValidStackLimitInput(input)) {
            return;
        }

        Integer parsedValue = parseStackLimitInput(input);
        if (parsedValue != null) {
            setPendingStackLimit(parsedValue, true, false);
        }
    }

    private void syncPendingStackLimitFromInput() {
        Integer parsedValue = parseStackLimitInput(stackLimitInput.getValue());
        if (parsedValue == null) {
            setStackLimitInputText(pendingStackLimit);
            return;
        }

        setPendingStackLimit(parsedValue, true, true);
    }

    private static boolean isValidStackLimitInput(String input) {
        if (input.length() > 14) {
            return false;
        }

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (isStackLimitSuffix(character)) {
                return index == input.length() - 1;
            }
            if (!Character.isDigit(character) && character != ',') {
                return false;
            }
        }
        return true;
    }

    private Layout getLayout() {
        int contentY = getContentY();
        int availablePanelHeight = Math.max(COMPACT_PANEL_HEIGHT, Math.min(PANEL_HEIGHT, this.height - contentY - BOTTOM_MARGIN));
        double expansion = (double) (availablePanelHeight - COMPACT_PANEL_HEIGHT) / (PANEL_HEIGHT - COMPACT_PANEL_HEIGHT);
        return new Layout(
                contentY,
                availablePanelHeight,
                contentY + interpolate(COMPACT_SECTION_LABEL_OFFSET, SECTION_LABEL_OFFSET, expansion),
                contentY + interpolate(COMPACT_SLIDER_OFFSET, SLIDER_OFFSET, expansion),
                contentY + interpolate(COMPACT_PRESET_LABEL_OFFSET, PRESET_LABEL_OFFSET, expansion),
                contentY + interpolate(COMPACT_PRESET_OFFSET, PRESET_OFFSET, expansion),
                contentY + interpolate(COMPACT_HINT_OFFSET, HINT_OFFSET, expansion),
                contentY + interpolate(COMPACT_DISPLAY_MODE_OFFSET, DISPLAY_MODE_OFFSET, expansion),
                contentY + interpolate(COMPACT_DISPLAY_DESCRIPTION_OFFSET, DISPLAY_DESCRIPTION_OFFSET, expansion),
                contentY + interpolate(COMPACT_ACTION_BUTTON_OFFSET, ACTION_BUTTON_OFFSET, expansion)
        );
    }

    private static int interpolate(int compactValue, int fullValue, double expansion) {
        return (int) Math.round(compactValue + (fullValue - compactValue) * expansion);
    }

    private static Integer parseStackLimitInput(String input) {
        String normalizedInput = input.trim().replace(",", "");
        if (normalizedInput.isEmpty()) {
            return null;
        }

        long multiplier = getStackLimitInputMultiplier(normalizedInput.charAt(normalizedInput.length() - 1));
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

    private static boolean isStackLimitSuffix(char character) {
        return character == 'k' || character == 'K'
                || character == 'm' || character == 'M'
                || character == 'b' || character == 'B';
    }

    private static long getStackLimitInputMultiplier(char suffix) {
        return switch (suffix) {
            case 'k', 'K' -> 1_000L;
            case 'm', 'M' -> 1_000_000L;
            case 'b', 'B' -> 1_000_000_000L;
            default -> 1L;
        };
    }

    private interface StackLimitChangeListener {
        void onChange(int value);
    }

    private record Layout(
            int contentY,
            int panelHeight,
            int sectionLabelY,
            int sliderY,
            int presetLabelY,
            int presetY,
            int hintY,
            int displayModeY,
            int displayDescriptionY,
            int actionButtonY
    ) {
    }

    private static class PanelWidget extends AbstractWidget {
        PanelWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
            this.active = false;
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), PANEL_COLOR);
            graphics.outline(getX(), getY(), getWidth(), getHeight(), PANEL_BORDER_COLOR);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    private static class StackLimitSlider extends AbstractSliderButton {
        private final StackLimitChangeListener listener;

        StackLimitSlider(int x, int y, int width, int height, int stackLimit, StackLimitChangeListener listener) {
            super(x, y, width, height, Component.empty(), toSliderValue(stackLimit));
            this.listener = listener;
            updateMessage();
        }

        void setStackLimit(int stackLimit) {
            this.value = toSliderValue(stackLimit);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(stackLimitText(toStackLimit(this.value)));
        }

        @Override
        protected void applyValue() {
            listener.onChange(toStackLimit(this.value));
            updateMessage();
        }

        private static double toSliderValue(int stackLimit) {
            int clamped = StackLimitConfig.clampStackLimit(stackLimit);
            return (Math.log10(clamped) - MIN_LOG) / (MAX_LOG - MIN_LOG);
        }

        private static int toStackLimit(double sliderValue) {
            double clampedValue = Math.max(0.0, Math.min(1.0, sliderValue));
            double raw = Math.pow(10.0, MIN_LOG + (MAX_LOG - MIN_LOG) * clampedValue);
            return StackLimitConfig.clampStackLimit((int) Math.round(raw));
        }
    }

}
