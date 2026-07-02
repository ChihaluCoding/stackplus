package chihalu.customstacklimit.modmenu;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.List;

public class StackPlusConfigScreen extends Screen {
    private static final double MIN_LOG = Math.log10(StackLimitConfig.MIN_STACK_LIMIT);
    private static final double MAX_LOG = Math.log10(StackLimitConfig.MAX_STACK_LIMIT);

    private final Screen parent;
    private int pendingStackLimit;
    private StackLimitSlider stackLimitSlider;

    public StackPlusConfigScreen(Screen parent) {
        super(Text.literal("StackPlus 設定"));
        this.parent = parent;
        this.pendingStackLimit = StackLimitConfig.getStackLimit();
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int controlWidth = Math.min(320, this.width - 40);
        int left = centerX - controlWidth / 2;
        int y = Math.max(52, this.height / 2 - 42);

        this.stackLimitSlider = new StackLimitSlider(left, y, controlWidth, 20, pendingStackLimit, value -> pendingStackLimit = value);
        addDrawableChild(this.stackLimitSlider);

        addDrawableChild(ButtonWidget.builder(Text.literal("保存"), button -> save())
                .dimensions(centerX - 154, y + 58, 96, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("リセット"), button -> {
                    pendingStackLimit = StackLimitConfig.DEFAULT_STACK_LIMIT;
                    stackLimitSlider.setStackLimit(pendingStackLimit);
                })
                .dimensions(centerX - 48, y + 58, 96, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.literal("キャンセル"), button -> close())
                .dimensions(centerX + 58, y + 58, 96, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("最大スタック数: " + StackLimitConfig.formatStackLimit(pendingStackLimit)), this.width / 2, Math.max(34, this.height / 2 - 60), 0xE0E0E0);
        if (pendingStackLimit > StackLimitConfig.WARNING_STACK_LIMIT) {
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("32,767を超える値です。保存時に警告が表示されます。"), this.width / 2, Math.max(78, this.height / 2 - 16), 0xFFD966);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void save() {
        if (pendingStackLimit > StackLimitConfig.WARNING_STACK_LIMIT) {
            MinecraftClient.getInstance().setScreen(new StackPlusWarningScreen(this, pendingStackLimit));
            return;
        }

        saveUnchecked();
    }

    void saveUnchecked() {
        StackLimitConfig.saveStackLimit(pendingStackLimit);
        close();
    }

    private interface StackLimitChangeListener {
        void onChange(int value);
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
            setMessage(Text.literal("最大スタック数: " + StackLimitConfig.formatStackLimit(toStackLimit(this.value))));
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
        private static final List<String> WARNING_LINES = List.of(
                "32,767を超えるスタック数は、通常の動作保証範囲外です。",
                "",
                "極端に大きな値を設定すると、以下の問題が発生する可能性があります。",
                "",
                "・アイテム数の消失や表示異常",
                "・チェストやインベントリの保存失敗",
                "・Shiftクリック、分割、クラフト時の不具合",
                "・他のModとの互換性問題",
                "・ワールドデータの破損",
                "",
                "重要なワールドでは、事前にバックアップを作成してください。",
                "",
                "推奨上限は32,767です。",
                "この値を超えて設定しますか？"
        );

        private final StackPlusConfigScreen parent;
        private final int stackLimit;

        StackPlusWarningScreen(StackPlusConfigScreen parent, int stackLimit) {
            super(Text.literal("⚠️警告⚠️"));
            this.parent = parent;
            this.stackLimit = stackLimit;
        }

        @Override
        protected void init() {
            int centerX = this.width / 2;
            int buttonY = Math.min(this.height - 32, 40 + WARNING_LINES.size() * 12 + 24);
            addDrawableChild(ButtonWidget.builder(Text.literal("戻る"), button -> MinecraftClient.getInstance().setScreen(parent))
                    .dimensions(centerX - 154, buttonY, 140, 20)
                    .build());
            addDrawableChild(ButtonWidget.builder(Text.literal("理解して続行"), button -> {
                        StackLimitConfig.saveStackLimit(stackLimit);
                        parent.close();
                    })
                    .dimensions(centerX + 14, buttonY, 140, 20)
                    .build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 18, 0xFF5555);
            int y = 42;
            for (String line : WARNING_LINES) {
                context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(line), this.width / 2, y, 0xFFFFFF);
                y += 12;
            }
        }

        @Override
        public void close() {
            MinecraftClient.getInstance().setScreen(parent);
        }
    }
}
