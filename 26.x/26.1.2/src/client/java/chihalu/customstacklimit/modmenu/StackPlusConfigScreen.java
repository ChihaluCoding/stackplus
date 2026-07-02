package chihalu.customstacklimit.modmenu;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StackPlusConfigScreen extends Screen {
    private static final double MIN_LOG = Math.log10(StackLimitConfig.MIN_STACK_LIMIT);
    private static final double MAX_LOG = Math.log10(StackLimitConfig.MAX_STACK_LIMIT);
    private static final int[] STACK_LIMIT_PRESETS = {64, 999, 1_000, 10_000, 1_000_000, 1_000_000_000};
    private static final String[] STACK_LIMIT_PRESET_KEYS = {
            "screen.stackplus.config.preset.64",
            "screen.stackplus.config.preset.999",
            "screen.stackplus.config.preset.1k",
            "screen.stackplus.config.preset.10k",
            "screen.stackplus.config.preset.1m",
            "screen.stackplus.config.preset.1b"
    };

    private final Screen parent;
    private int pendingStackLimit;
    private StackLimitConfig.DisplayMode pendingDisplayMode;
    private StackLimitSlider stackLimitSlider;
    private EditBox stackLimitInput;
    private Button displayModeButton;
    private boolean updatingStackLimitInput;

    public StackPlusConfigScreen(Screen parent) {
        super(Component.translatable("screen.stackplus.config.title"));
        this.parent = parent;
        this.pendingStackLimit = StackLimitConfig.getStackLimit();
        this.pendingDisplayMode = StackLimitConfig.getDisplayMode();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int controlWidth = Math.min(420, this.width - 40);
        int inputWidth = 104;
        int gap = 8;
        int sliderWidth = Math.max(120, controlWidth - inputWidth - gap);
        int left = centerX - controlWidth / 2;
        int sliderY = Math.max(50, this.height / 2 - 112);
        int presetY = sliderY + 30;
        int displayModeY = sliderY + 108;
        int buttonY = displayModeY + 50;

        this.stackLimitSlider = new StackLimitSlider(left, sliderY, sliderWidth, 20, pendingStackLimit,
                value -> setPendingStackLimit(value, false, true));
        addRenderableWidget(this.stackLimitSlider);

        this.stackLimitInput = new EditBox(this.font, left + sliderWidth + gap, sliderY, inputWidth, 20, stackLimitText(pendingStackLimit));
        this.stackLimitInput.setMaxLength(13);
        this.stackLimitInput.setResponder(this::onStackLimitInputChanged);
        setStackLimitInputText(pendingStackLimit);
        addRenderableWidget(this.stackLimitInput);

        addPresetButtons(left, presetY, controlWidth);

        this.displayModeButton = Button.builder(displayModeText(pendingDisplayMode), button -> {
                    pendingDisplayMode = pendingDisplayMode.next();
                    button.setMessage(displayModeText(pendingDisplayMode));
                })
                .bounds(left, displayModeY, controlWidth, 20)
                .build();
        addRenderableWidget(this.displayModeButton);

        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.save"), button -> save())
                .bounds(centerX - 154, buttonY, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.reset"), button -> {
                    setPendingStackLimit(StackLimitConfig.DEFAULT_STACK_LIMIT, true, true);
                    pendingDisplayMode = StackLimitConfig.DisplayMode.COMPACT;
                    displayModeButton.setMessage(displayModeText(pendingDisplayMode));
                })
                .bounds(centerX - 48, buttonY, 96, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.cancel"), button -> onClose())
                .bounds(centerX + 58, buttonY, 96, 20)
                .build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int sliderY = Math.max(50, this.height / 2 - 112);
        int displayModeY = sliderY + 108;
        graphics.centeredText(this.font, this.title, this.width / 2, 20, 0xFFFFFFFF);
        graphics.centeredText(this.font, stackLimitText(pendingStackLimit), this.width / 2, sliderY - 18, 0xFFE0E0E0);
        if (pendingStackLimit > StackLimitConfig.WARNING_STACK_LIMIT) {
            graphics.centeredText(this.font, Component.translatable(getWarningHintKey(pendingStackLimit)), this.width / 2, sliderY + 76, 0xFFFFD966);
        }
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.config.existing_stack_hint"), this.width / 2, sliderY + 90, 0xFFB8B8B8);
        graphics.centeredText(this.font, displayModeDescriptionText(pendingDisplayMode), this.width / 2, displayModeY + 24, 0xFFE0E0E0);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    private void save() {
        syncPendingStackLimitFromInput();
        if (pendingStackLimit > StackLimitConfig.WARNING_STACK_LIMIT) {
            Minecraft.getInstance().setScreenAndShow(new StackPlusWarningScreen(this, pendingStackLimit));
            return;
        }

        saveUnchecked();
    }

    void saveUnchecked() {
        StackLimitConfig.saveSettings(pendingStackLimit, pendingDisplayMode);
        onClose();
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

    private static Component displayModeDescriptionText(StackLimitConfig.DisplayMode displayMode) {
        return Component.translatable(displayMode == StackLimitConfig.DisplayMode.PLUS_99
                ? "screen.stackplus.config.display_mode.description.99_plus"
                : "screen.stackplus.config.display_mode.description.compact");
    }

    private static String getWarningHintKey(int stackLimit) {
        if (stackLimit >= 100_000_000) {
            return "screen.stackplus.config.warning_hint.experimental";
        }
        if (stackLimit >= 1_000_000) {
            return "screen.stackplus.config.warning_hint.strong";
        }

        return "screen.stackplus.config.warning_hint";
    }

    private void addPresetButtons(int left, int presetY, int controlWidth) {
        int columns = 3;
        int gap = 6;
        int buttonWidth = (controlWidth - gap * (columns - 1)) / columns;

        for (int index = 0; index < STACK_LIMIT_PRESETS.length; index++) {
            int column = index % columns;
            int row = index / columns;
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
        if (input.length() > 13) {
            return false;
        }

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (!Character.isDigit(character) && character != ',') {
                return false;
            }
        }
        return true;
    }

    private static Integer parseStackLimitInput(String input) {
        String digitsOnly = input.replace(",", "");
        if (digitsOnly.isEmpty()) {
            return null;
        }

        try {
            long value = Long.parseLong(digitsOnly);
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

    private interface StackLimitChangeListener {
        void onChange(int value);
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

    private static class StackPlusWarningScreen extends Screen {
        private static final List<Component> WARNING_LINES = List.of(
                Component.translatable("screen.stackplus.warning.line_1"),
                Component.translatable("screen.stackplus.warning.line_2"),
                Component.empty(),
                Component.translatable("screen.stackplus.warning.line_3"),
                Component.empty(),
                Component.translatable("screen.stackplus.warning.line_4"),
                Component.translatable("screen.stackplus.warning.line_5"),
                Component.translatable("screen.stackplus.warning.line_6"),
                Component.empty(),
                Component.translatable("screen.stackplus.warning.line_7"),
                Component.translatable("screen.stackplus.warning.line_8")
        );

        private final StackPlusConfigScreen parent;

        StackPlusWarningScreen(StackPlusConfigScreen parent, int stackLimit) {
            super(Component.translatable("screen.stackplus.warning.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int buttonY = Math.min(this.height - 32, 52 + WARNING_LINES.size() * 12 + 24);
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.back"), button -> Minecraft.getInstance().setScreenAndShow(parent))
                    .bounds(centerX - 154, buttonY, 140, 20)
                    .build());
            addRenderableWidget(Button.builder(Component.translatable("button.stackplus.continue"), button -> parent.saveUnchecked())
                    .bounds(centerX + 14, buttonY, 140, 20)
                    .build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            graphics.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFF5555);
            int y = 52;
            for (Component line : WARNING_LINES) {
                graphics.centeredText(this.font, line, this.width / 2, y, 0xFFFFFFFF);
                y += 12;
            }
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreenAndShow(parent);
        }
    }
}
