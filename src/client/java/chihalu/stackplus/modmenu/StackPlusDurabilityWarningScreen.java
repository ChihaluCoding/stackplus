package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;

/** 耐久値付きアイテムをスタック許可する前の確認画面です。 */
final class StackPlusDurabilityWarningScreen extends Screen {
    private final Screen parent;
    private final Runnable confirmAction;
    private boolean suppressWarningSelected;

    private StackPlusDurabilityWarningScreen(Screen parent, Runnable confirmAction) {
        super(Text.translatable("screen.stackplus.durability_warning.title"));
        this.parent = parent;
        this.confirmAction = confirmAction;
    }

    static void open(Screen parent, Runnable confirmAction) {
        MinecraftClient.getInstance().setScreen(new StackPlusDurabilityWarningScreen(parent, confirmAction));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 42;
        CheckboxWidget checkbox = CheckboxWidget.builder(Text.translatable("screen.stackplus.durability_warning.suppress"), this.textRenderer)
                .pos(centerX - 120, buttonY - 28).checked(false)
                .callback((widget, checked) -> suppressWarningSelected = checked).build();
        addDrawableChild(checkbox);
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.save"), button -> confirm())
                .dimensions(centerX - 104, buttonY, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.back"), button -> close())
                .dimensions(centerX + 4, buttonY, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 38;
        context.drawCenteredTextWithShadow(this.textRenderer, title, centerX, centerY, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.durability_warning.line1"), centerX, centerY + 24, 0xFFFF5555);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.durability_warning.line2"), centerX, centerY + 40, 0xFFFF5555);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void confirm() {
        if (suppressWarningSelected) {
            StackLimitConfig.setDurabilityWarningSuppressed(true);
        }
        confirmAction.run();
        close();
    }
}

