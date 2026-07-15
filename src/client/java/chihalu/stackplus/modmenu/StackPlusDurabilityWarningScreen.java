package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** 耐久値付きアイテムをスタック許可する前の確認画面です。 */
final class StackPlusDurabilityWarningScreen extends Screen {
    private final Screen parent;
    private final Runnable confirmAction;
    private Checkbox suppressWarning;
    private boolean suppressWarningSelected;

    private StackPlusDurabilityWarningScreen(Screen parent, Runnable confirmAction) {
        super(Component.translatable("screen.stackplus.durability_warning.title"));
        this.parent = parent;
        this.confirmAction = confirmAction;
    }

    static void open(Screen parent, Runnable confirmAction) {
        Minecraft.getInstance().setScreenAndShow(new StackPlusDurabilityWarningScreen(parent, confirmAction));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int buttonY = this.height / 2 + 42;
        suppressWarning = Checkbox.builder(Component.translatable("screen.stackplus.durability_warning.suppress"), this.font)
                .pos(centerX - 120, buttonY - 28)
                .selected(false)
                .onValueChange((checkbox, selected) -> suppressWarningSelected = selected)
                .build();
        suppressWarning.setTooltip(Tooltip.create(Component.translatable("tooltip.stackplus.durability_warning.suppress")));
        addRenderableWidget(suppressWarning);
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.save"), button -> confirm())
                .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.durability_warning.confirm")))
                .bounds(centerX - 104, buttonY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.back"), button -> onClose())
                .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.settings.back")))
                .bounds(centerX + 4, buttonY, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int centerY = this.height / 2 - 38;
        graphics.centeredText(this.font, title, centerX, centerY, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.durability_warning.line1"), centerX, centerY + 24, 0xFFFF5555);
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.durability_warning.line2"), centerX, centerY + 40, 0xFFFF5555);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    private void confirm() {
        if (suppressWarningSelected) {
            StackLimitConfig.setDurabilityWarningSuppressed(true);
        }
        confirmAction.run();
        onClose();
    }
}
