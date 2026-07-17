package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class StackPlusConfigScreen extends Screen {
    private static final double MIN_LOG = Math.log10(StackLimitConfig.MIN_STACK_LIMIT);
    private static final double MAX_LOG = Math.log10(StackLimitConfig.MAX_STACK_LIMIT);
    private static final int TITLE_Y = 12;
    private static final int MIN_CONTENT_Y = 36;
    private static final int CONTENT_CENTER_OFFSET = 154;
    private static final int SECTION_LABEL_OFFSET = 14;
    private static final int SLIDER_OFFSET = 42;
    private static final int PRESET_LABEL_OFFSET = 98;
    private static final int PRESET_OFFSET = 114;
    private static final int HINT_OFFSET = 178;
    private static final int DISPLAY_MODE_OFFSET = 216;
    private static final int DISPLAY_DESCRIPTION_OFFSET = 240;
    private static final int UPDATE_NOTIFICATIONS_OFFSET = 264;
    private static final int ACTION_BUTTON_OFFSET = 312;
    private static final int PANEL_HEIGHT = 374;
    private static final int COMPACT_PANEL_HEIGHT = 302;
    private static final int HIDDEN_PRESETS_PANEL_HEIGHT = 238;
    private static final int COMPACT_SECTION_LABEL_OFFSET = 10;
    private static final int COMPACT_SLIDER_OFFSET = 32;
    private static final int COMPACT_PRESET_LABEL_OFFSET = 80;
    private static final int COMPACT_PRESET_OFFSET = 94;
    private static final int COMPACT_HINT_OFFSET = 148;
    private static final int COMPACT_DISPLAY_MODE_OFFSET = 176;
    private static final int COMPACT_DISPLAY_DESCRIPTION_OFFSET = 200;
    private static final int COMPACT_UPDATE_NOTIFICATIONS_OFFSET = 224;
    private static final int COMPACT_ACTION_BUTTON_OFFSET = 272;
    private static final int HIDDEN_PRESETS_SECTION_LABEL_OFFSET = 10;
    private static final int HIDDEN_PRESETS_SLIDER_OFFSET = 32;
    private static final int HIDDEN_PRESETS_HINT_OFFSET = 62;
    private static final int HIDDEN_PRESETS_DISPLAY_MODE_OFFSET = 88;
    private static final int HIDDEN_PRESETS_DISPLAY_DESCRIPTION_OFFSET = 112;
    private static final int HIDDEN_PRESETS_UPDATE_NOTIFICATIONS_OFFSET = 136;
    private static final int HIDDEN_PRESETS_ACTION_BUTTON_OFFSET = 202;
    private static final int BOTTOM_MARGIN = 8;
    private static final int CONTENT_WIDTH = 360;
    private static final int INPUT_WIDTH = 112;
    private static final int CONTROL_GAP = 8;
    private static final int PRESET_COLUMNS = 4;
    private static final int PRESET_ROWS = 2;
    private static final int PRESETS_PER_PAGE = PRESET_COLUMNS * PRESET_ROWS;
    private static final int ACTION_BUTTON_WIDTH = 100;
    private static final int ACTION_BUTTON_GAP = 8;
    private static final int PANEL_COLOR = 0x58000000;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;
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
    private StackLimitConfig.SelectedItemCountMode pendingSelectedItemCountMode;
    private StackLimitConfig.SelectedItemCountPosition pendingSelectedItemCountPosition;
    private int pendingSelectedItemCountColorRgb;
    private boolean pendingUpdateNotificationsEnabled;
    private boolean pendingStackLimitPresetsVisible;
    private StackLimitSlider stackLimitSlider;
    private TextFieldWidget stackLimitInput;
    private ButtonWidget displayModeButton;
    private ButtonWidget selectedItemCountButton;
    private ButtonWidget selectedItemCountPositionButton;
    private ButtonWidget selectedItemCountColorButton;
    private ButtonWidget updateNotificationsButton;
    private int presetPage;
    private boolean updatingStackLimitInput;

    public StackPlusConfigScreen(Screen parent) {
        super(Text.translatable("screen.stackplus.config.title"));
        this.parent = parent;
        this.pendingStackLimit = StackLimitConfig.getStackLimit();
        this.pendingDisplayMode = StackLimitConfig.getDisplayMode();
        this.pendingSelectedItemCountMode = StackLimitConfig.getSelectedItemCountMode();
        this.pendingSelectedItemCountPosition = StackLimitConfig.getSelectedItemCountPosition();
        this.pendingSelectedItemCountColorRgb = StackLimitConfig.getSelectedItemCountColorRgb();
        this.pendingUpdateNotificationsEnabled = StackLimitConfig.isUpdateNotificationsEnabled();
        this.pendingStackLimitPresetsVisible = StackLimitConfig.areStackLimitPresetsVisible();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int controlWidth = getControlWidth();
        int sliderWidth = Math.max(132, controlWidth - INPUT_WIDTH - CONTROL_GAP);
        int left = centerX - controlWidth / 2;
        Layout layout = getLayout();
        int secondaryButtonWidth = (controlWidth - CONTROL_GAP) / 2;


        this.stackLimitSlider = new StackLimitSlider(left, layout.sliderY(), sliderWidth, 20, pendingStackLimit,
                value -> setPendingStackLimit(value, false, true));
        this.stackLimitSlider.setTooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.stack_limit")));
        addDrawableChild(this.stackLimitSlider);

        this.stackLimitInput = new TextFieldWidget(this.textRenderer, left + sliderWidth + CONTROL_GAP, layout.sliderY(), INPUT_WIDTH, 20, stackLimitText(pendingStackLimit));
        this.stackLimitInput.setTooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.stack_limit")));
        this.stackLimitInput.setMaxLength(14);
        this.stackLimitInput.setChangedListener(this::onStackLimitInputChanged);
        setStackLimitInputText(pendingStackLimit);
        addDrawableChild(this.stackLimitInput);

        addPresetButtons(left, layout.presetLabelY(), layout.presetY(), controlWidth);

        this.displayModeButton = ButtonWidget.builder(displayModeText(pendingDisplayMode), button -> {
                    pendingDisplayMode = pendingDisplayMode.next();
                    button.setMessage(displayModeText(pendingDisplayMode));
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.display_mode")))
                .dimensions(left, layout.displayModeY(), secondaryButtonWidth, 20)
                .build();
        addDrawableChild(this.displayModeButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.item_limits"), button -> {
                    syncPendingStackLimitFromInput();
                    StackPlusItemSelection.start(this, pendingStackLimit);
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.item_limits")))
                .dimensions(left + secondaryButtonWidth + CONTROL_GAP, layout.displayModeY(), secondaryButtonWidth, 20)
                .build());

        this.selectedItemCountButton = ButtonWidget.builder(selectedItemCountModeText(pendingSelectedItemCountMode), button -> {
                    pendingSelectedItemCountMode = pendingSelectedItemCountMode.next();
                    button.setMessage(selectedItemCountModeText(pendingSelectedItemCountMode));
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.selected_item_count")))
                .dimensions(left, layout.displayDescriptionY(), secondaryButtonWidth, 20)
                .build();
        addDrawableChild(this.selectedItemCountButton);

        this.updateNotificationsButton = ButtonWidget.builder(updateNotificationsText(pendingUpdateNotificationsEnabled), button -> {
                    pendingUpdateNotificationsEnabled = !pendingUpdateNotificationsEnabled;
                    button.setMessage(updateNotificationsText(pendingUpdateNotificationsEnabled));
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.update_notifications")))
                .dimensions(left + secondaryButtonWidth + CONTROL_GAP, layout.displayDescriptionY(), secondaryButtonWidth, 20)
                .build();
        addDrawableChild(this.updateNotificationsButton);

        addDrawableChild(ButtonWidget.builder(stackLimitPresetsVisibleText(), button -> {
                    pendingStackLimitPresetsVisible = !pendingStackLimitPresetsVisible;
                    StackLimitConfig.saveStackLimitPresetsVisible(pendingStackLimitPresetsVisible);
                    clearAndInit();
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.preset_visibility")))
                .dimensions(left + secondaryButtonWidth + CONTROL_GAP, layout.updateNotificationsY(), secondaryButtonWidth, 20)
                .build());

        this.selectedItemCountColorButton = ButtonWidget.builder(selectedItemCountColorText(pendingSelectedItemCountColorRgb), button ->
                    StackPlusColorPickerScreen.open(this, pendingSelectedItemCountColorRgb, this::setPendingSelectedItemCountColorRgb))
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.selected_item_count_color")))
                .dimensions(left, layout.updateNotificationsY(), secondaryButtonWidth, 20)
                .build();
        addDrawableChild(this.selectedItemCountColorButton);

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.font_settings"), button ->
                    MinecraftClient.getInstance().setScreen(new StackPlusFontConfigScreen(this)))
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.font_settings")))
                .dimensions(left + secondaryButtonWidth + CONTROL_GAP, layout.updateNotificationsY() + 24, secondaryButtonWidth, 20)
                .build());

        int firstButtonX = centerX - (ACTION_BUTTON_WIDTH * 2 + ACTION_BUTTON_GAP) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.save"), button -> save())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.settings.save")))
                .dimensions(firstButtonX, layout.actionButtonY(), ACTION_BUTTON_WIDTH, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.back"), button -> close())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.settings.back")))
                .dimensions(firstButtonX + ACTION_BUTTON_WIDTH + ACTION_BUTTON_GAP, layout.actionButtonY(), ACTION_BUTTON_WIDTH, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        Layout layout = getLayout();
        int left = this.width / 2 - getControlWidth() / 2;
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.title, this.width / 2, TITLE_Y, 0xFFFFFFFF);
        drawCenteredText(context, Text.translatable("screen.stackplus.config.stack_limit_label"), this.width / 2, layout.sectionLabelY(), 0xFFE0E0E0);
        if (pendingStackLimitPresetsVisible) {
            drawCenteredText(context, Text.translatable("screen.stackplus.config.presets_label"), this.width / 2, layout.presetLabelY(), 0xFFE0E0E0);
            int pageCount = getPresetPageCount(createPresetEntries().size());
            int currentPage = Math.max(0, Math.min(presetPage, pageCount - 1));

            context.drawText(this.textRenderer, Text.literal((currentPage + 1) + "/" + pageCount), left + 60, layout.presetLabelY(), 0xFFB8B8B8, false);
        }
        drawCenteredText(context, Text.translatable("screen.stackplus.config.existing_stack_hint"), this.width / 2, layout.hintY(), 0xFFB8B8B8);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Screen.render内の正規順序で、ぼかしなし背景の後にGUIパネルを描画する。
        renderStackPlusBackground(context, delta);
        Layout layout = getLayout();
        int left = this.width / 2 - getControlWidth() / 2;
        renderPanel(context, left - 12, layout.contentY(), getControlWidth() + 24, layout.panelHeight());
    }

    private void renderStackPlusBackground(DrawContext context, float delta) {
        if (this.client.world == null) {
            this.renderPanoramaBackground(context, delta);
        }
        this.applyBlur(delta);
        this.renderInGameBackground(context);
    }

    private void drawCenteredText(DrawContext context, Text text, int centerX, int y, int color) {
        int x = centerX - this.textRenderer.getWidth(text) / 2;
        context.drawText(this.textRenderer, text, x, y, color, false);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void save() {
        syncPendingStackLimitFromInput();
        saveUnchecked();
    }

    void saveUnchecked() {
        StackLimitConfig.saveSettings(pendingStackLimit, pendingDisplayMode, pendingSelectedItemCountMode, pendingSelectedItemCountPosition,
                pendingSelectedItemCountColorRgb, pendingUpdateNotificationsEnabled);
        StackLimitConfig.saveStackLimitPresetsVisible(pendingStackLimitPresetsVisible);
    }

    private static Text stackLimitText(int stackLimit) {
        return Text.translatable("screen.stackplus.config.stack_limit", StackLimitConfig.formatStackLimit(stackLimit));
    }

    private static Text displayModeText(StackLimitConfig.DisplayMode displayMode) {
        String modeKey = displayMode == StackLimitConfig.DisplayMode.PLUS_99
                ? "screen.stackplus.config.display_mode.99_plus"
                : "screen.stackplus.config.display_mode.compact";
        return Text.translatable("screen.stackplus.config.display_mode", Text.translatable(modeKey));
    }

    private static Text selectedItemCountModeText(StackLimitConfig.SelectedItemCountMode mode) {
        String modeKey = switch (mode) {
            case OFF -> "screen.stackplus.config.selected_item_count.off";
            case ON_SWITCH -> "screen.stackplus.config.selected_item_count.on_switch";
            case ALWAYS -> "screen.stackplus.config.selected_item_count.always";
        };
        return Text.translatable("screen.stackplus.config.selected_item_count", Text.translatable(modeKey));
    }

    private static Text selectedItemCountPositionText(StackLimitConfig.SelectedItemCountPosition position) {
        String positionKey = position.isBelow()
                ? "screen.stackplus.config.selected_item_count_position.below"
                : "screen.stackplus.config.selected_item_count_position.beside";
        return Text.translatable("screen.stackplus.config.selected_item_count_position", Text.translatable(positionKey));
    }

    private void setPendingSelectedItemCountColorRgb(int colorRgb) {
        pendingSelectedItemCountColorRgb = colorRgb & 0xFFFFFF;
        if (selectedItemCountColorButton != null) {
            selectedItemCountColorButton.setMessage(selectedItemCountColorText(pendingSelectedItemCountColorRgb));
        }
    }

    private static Text selectedItemCountColorText(int colorRgb) {
        return Text.translatable("screen.stackplus.config.selected_item_count_color",
                Text.literal(String.format("#%06X", colorRgb & 0xFFFFFF))
                        .styled(style -> style.withColor(colorRgb)));
    }

    private static Text updateNotificationsText(boolean enabled) {
        String statusKey = enabled ? "screen.stackplus.config.update_notifications.on" : "screen.stackplus.config.update_notifications.off";
        return Text.translatable("screen.stackplus.config.update_notifications", Text.translatable(statusKey));
    }

    private int getContentY() {
        int minimumPanelHeight = pendingStackLimitPresetsVisible ? COMPACT_PANEL_HEIGHT : HIDDEN_PRESETS_PANEL_HEIGHT;
        int contentCenterOffset = pendingStackLimitPresetsVisible ? CONTENT_CENTER_OFFSET : 112;
        int naturalY = Math.max(MIN_CONTENT_Y, this.height / 2 - contentCenterOffset);
        int lowestFittingY = this.height - minimumPanelHeight - BOTTOM_MARGIN;
        return Math.max(28, Math.min(naturalY, lowestFittingY));
    }

    private int getControlWidth() {
        return Math.min(CONTENT_WIDTH, this.width - 48);
    }

    private void addPresetEditButtons(int left, int top, int buttonWidth, int gap) {
        int firstColumn = PRESET_COLUMNS - 1;
        int firstX = left + firstColumn * (buttonWidth + gap);
        int editGap = 4;
        int editButtonWidth = (buttonWidth - editGap) / 2;
        int removeButtonWidth = buttonWidth - editButtonWidth - editGap;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.add_preset"), button -> addCustomPreset())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.add_preset")))
                .dimensions(firstX, top, editButtonWidth, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.remove_preset"), button -> removeCustomPreset())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.remove_preset")))
                .dimensions(firstX + editButtonWidth + editGap, top, removeButtonWidth, 20)
                .build());
    }

    private void addPresetButtons(int left, int presetLabelY, int presetY, int controlWidth) {
        int gap = 6;
        int buttonWidth = (controlWidth - gap * (PRESET_COLUMNS - 1)) / PRESET_COLUMNS;
        if (!pendingStackLimitPresetsVisible) {
            return;
        }
        addPresetEditButtons(left, presetLabelY - 6, buttonWidth, gap);

        List<PresetEntry> presets = createPresetEntries();
        int pageCount = getPresetPageCount(presets.size());
        presetPage = Math.max(0, Math.min(presetPage, pageCount - 1));
        addPresetPageButtons(left, presetLabelY, presetPage, pageCount);

        int startIndex = presetPage * PRESETS_PER_PAGE;
        int endIndex = Math.min(presets.size(), startIndex + PRESETS_PER_PAGE);
        for (int index = startIndex; index < endIndex; index++) {
            PresetEntry preset = presets.get(index);
            int pageIndex = index - startIndex;
            addPresetButton(left, presetY, buttonWidth, gap, pageIndex, preset.message(), preset.value());
        }
    }

    private List<PresetEntry> createPresetEntries() {
        List<PresetEntry> presets = new ArrayList<>();
        for (int builtInIndex = 0; builtInIndex < STACK_LIMIT_PRESETS.length; builtInIndex++) {
            presets.add(new PresetEntry(Text.translatable(STACK_LIMIT_PRESET_KEYS[builtInIndex]), STACK_LIMIT_PRESETS[builtInIndex]));
        }
        for (int customPreset : StackLimitConfig.getCustomStackLimitPresets()) {
            presets.add(new PresetEntry(Text.literal(formatPreset(customPreset)), customPreset));
        }
        return presets;
    }

    private void addPresetPageButtons(int left, int presetLabelY, int page, int pageCount) {
        ButtonWidget previousButton = ButtonWidget.builder(Text.literal("<"), button -> {
                    presetPage = Math.max(0, presetPage - 1);
                    clearAndInit();
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.previous_page")))
                .dimensions(left, presetLabelY - 6, 24, 20)
                .build();
        previousButton.active = page > 0;
        addDrawableChild(previousButton);

        ButtonWidget nextButton = ButtonWidget.builder(Text.literal(">"), button -> {
                    presetPage = Math.min(pageCount - 1, presetPage + 1);
                    clearAndInit();
                })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.next_page")))
                .dimensions(left + 30, presetLabelY - 6, 24, 20)
                .build();
        nextButton.active = page + 1 < pageCount;
        addDrawableChild(nextButton);
    }

    private void addPresetButton(int left, int presetY, int buttonWidth, int gap, int index, Text message, int presetValue) {
        int column = index % PRESET_COLUMNS;
        int row = index / PRESET_COLUMNS;
        int x = left + column * (buttonWidth + gap);
        int y = presetY + row * 22;
        addDrawableChild(ButtonWidget.builder(message, button -> setPendingStackLimit(presetValue, true, true))
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.config.preset")))
                .dimensions(x, y, buttonWidth, 20)
                .build());
    }

    private void addCustomPreset() {
        syncPendingStackLimitFromInput();
        if (!isBuiltInPreset(pendingStackLimit) && StackLimitConfig.addCustomStackLimitPreset(pendingStackLimit)) {
            presetPage = getPresetPageCount(createPresetEntries().size()) - 1;
            clearAndInit();
        }
    }

    private void removeCustomPreset() {
        syncPendingStackLimitFromInput();
        if (StackLimitConfig.removeCustomStackLimitPreset(pendingStackLimit)) {
            int pageCount = getPresetPageCount(createPresetEntries().size());
            presetPage = Math.max(0, Math.min(presetPage, pageCount - 1));
            clearAndInit();
        }
    }

    private static int getPresetPageCount(int presetCount) {
        return Math.max(1, (presetCount + PRESETS_PER_PAGE - 1) / PRESETS_PER_PAGE);
    }

    private static boolean isBuiltInPreset(int value) {
        for (int preset : STACK_LIMIT_PRESETS) {
            if (preset == value) {
                return true;
            }
        }
        return false;
    }

    private Text stackLimitPresetsVisibleText() {
        return Text.translatable(pendingStackLimitPresetsVisible
                ? "button.stackplus.hide_presets"
                : "button.stackplus.show_presets");
    }

    private static String formatPreset(int value) {
        if (value >= 1_000_000_000 && value % 1_000_000_000 == 0) {
            return value / 1_000_000_000 + "B";
        }
        if (value >= 1_000_000 && value % 1_000_000 == 0) {
            return value / 1_000_000 + "M";
        }
        if (value >= 1_000 && value % 1_000 == 0) {
            return value / 1_000 + "K";
        }
        return StackLimitConfig.formatStackLimit(value);
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
        stackLimitInput.setText(StackLimitConfig.formatStackLimit(stackLimit));
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
        Integer parsedValue = parseStackLimitInput(stackLimitInput.getText());
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
        if (!pendingStackLimitPresetsVisible) {
            return new Layout(
                    contentY,
                    HIDDEN_PRESETS_PANEL_HEIGHT,
                    contentY + HIDDEN_PRESETS_SECTION_LABEL_OFFSET,
                    contentY + HIDDEN_PRESETS_SLIDER_OFFSET,
                    contentY + HIDDEN_PRESETS_HINT_OFFSET,
                    contentY + HIDDEN_PRESETS_HINT_OFFSET,
                    contentY + HIDDEN_PRESETS_HINT_OFFSET,
                    contentY + HIDDEN_PRESETS_DISPLAY_MODE_OFFSET,
                    contentY + HIDDEN_PRESETS_DISPLAY_DESCRIPTION_OFFSET,
                    contentY + HIDDEN_PRESETS_UPDATE_NOTIFICATIONS_OFFSET,
                    contentY + HIDDEN_PRESETS_ACTION_BUTTON_OFFSET
            );
        }

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
                contentY + interpolate(COMPACT_UPDATE_NOTIFICATIONS_OFFSET, UPDATE_NOTIFICATIONS_OFFSET, expansion),
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

    private record PresetEntry(Text message, int value) {
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
            int updateNotificationsY,
            int actionButtonY
    ) {
    }

    private static void renderPanel(DrawContext context, int x, int y, int width, int height) {
        context.fill(x, y, x + width, y + height, PANEL_COLOR);
        context.drawBorder(x, y, width, height, PANEL_BORDER_COLOR);
    }

    private static class StackLimitSlider extends SliderWidget {
        private final StackLimitChangeListener listener;

        StackLimitSlider(int x, int y, int width, int height, int stackLimit, StackLimitChangeListener listener) {
            super(x, y, width, height, Text.empty(), toSliderValue(stackLimit));
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
