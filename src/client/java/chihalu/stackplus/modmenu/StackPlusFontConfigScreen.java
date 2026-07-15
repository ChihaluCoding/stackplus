package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import chihalu.stackplus.client.StackPlusCustomFont;
import chihalu.stackplus.client.StackPlusFontSupport;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

final class StackPlusFontConfigScreen extends Screen {
    private static final int WIDTH = 280;
    private static final int FONT_BUTTON_WIDTH = 176;
    private static final int PREVIEW_GAP = 8;
    private static final int PREVIEW_WIDTH = WIDTH - FONT_BUTTON_WIDTH - PREVIEW_GAP;
    private static final int PANEL_HEIGHT = 210;
    private static final int PANEL_COLOR = 0x80000000;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;
    private static final int PREVIEW_COLOR = 0xA0202020;
    private final Screen parent;
    private StackLimitConfig.CountFont slotFont;
    private StackLimitConfig.CountFont selectedItemFont;
    private StackLimitConfig.CountFont tooltipFont;
    private StackLimitConfig.FontPriority fontPriority;
    private boolean uniformSlotCountHeight;

    StackPlusFontConfigScreen(Screen parent) {
        super(Text.translatable("screen.stackplus.font_config.title"));
        this.parent = parent;
        this.slotFont = StackLimitConfig.getSlotCountFont();
        this.selectedItemFont = StackLimitConfig.getSelectedItemCountFont();
        this.tooltipFont = StackLimitConfig.getTooltipCountFont();
        this.fontPriority = StackLimitConfig.getFontPriority();
        this.uniformSlotCountHeight = StackLimitConfig.isUniformSlotCountHeight();
    }

    @Override
    protected void init() {
        int left = this.width / 2 - WIDTH / 2;
        int top = contentTop();

        addDrawableChild(ButtonWidget.builder(fontText("screen.stackplus.font_config.slot", slotFont), button -> {
                    slotFont = slotFont.next();
                    button.setMessage(fontText("screen.stackplus.font_config.slot", slotFont));
                }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.slot")))
                .dimensions(left, top, FONT_BUTTON_WIDTH, 20).build());
        addDrawableChild(ButtonWidget.builder(fontText("screen.stackplus.font_config.selected_item", selectedItemFont), button -> {
                    selectedItemFont = selectedItemFont.next();
                    button.setMessage(fontText("screen.stackplus.font_config.selected_item", selectedItemFont));
                }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.selected_item")))
                .dimensions(left, top + 24, FONT_BUTTON_WIDTH, 20).build());
        addDrawableChild(ButtonWidget.builder(fontText("screen.stackplus.font_config.tooltip", tooltipFont), button -> {
                    tooltipFont = tooltipFont.next();
                    button.setMessage(fontText("screen.stackplus.font_config.tooltip", tooltipFont));
                }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.tooltip")))
                .dimensions(left, top + 48, FONT_BUTTON_WIDTH, 20).build());

        int columnWidth = (WIDTH - 8) / 2;
        addDrawableChild(ButtonWidget.builder(priorityText(fontPriority), button -> {
                    fontPriority = fontPriority.next();
                    button.setMessage(priorityText(fontPriority));
                }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.priority")))
                .dimensions(left, top + 76, columnWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(sizeModeText(uniformSlotCountHeight), button -> {
                    uniformSlotCountHeight = !uniformSlotCountHeight;
                    button.setMessage(sizeModeText(uniformSlotCountHeight));
                }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.display_mode")))
                .dimensions(left + columnWidth + 8, top + 76, columnWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.import_font"), button -> {
                    if (StackPlusCustomFont.importFont(Text.translatable("screen.stackplus.font_config.import_title").getString())) {
                        MinecraftClient.getInstance().reloadResources();
                    }
                }).tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.import")))
                .dimensions(left, top + 100, columnWidth, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.reload_fonts"), button ->
                        {
                            StackPlusCustomFont.prepareFont();
                            MinecraftClient.getInstance().reloadResources();
                        })
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.font_config.reload")))
                .dimensions(left + columnWidth + 8, top + 100, columnWidth, 20).build());

        int actionWidth = (WIDTH - 8) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.save"), button -> save())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.settings.save")))
                .dimensions(left, top + 128, actionWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.back"), button -> close())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.settings.back")))
                .dimensions(left + actionWidth + 8, top + 128, actionWidth, 20).build());
    }

    @Override
    public void renderBackground(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - WIDTH / 2 - 12;
        int panelTop = panelTop();
        int panelWidth = WIDTH + 24;
        graphics.fill(left, panelTop, left + panelWidth, panelTop + PANEL_HEIGHT, PANEL_COLOR);
        drawBorder(graphics, left, panelTop, panelWidth, PANEL_HEIGHT);
    }

    @Override
    public void render(DrawContext graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int top = contentTop();
        graphics.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, panelTop() + 12, 0xFFFFFFFF);
        graphics.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.font_config.minecraft_hint"),
                this.width / 2, Math.max(28, top - 22), 0xFFB8B8B8);

        int previewX = this.width / 2 - WIDTH / 2 + FONT_BUTTON_WIDTH + PREVIEW_GAP;
        drawPreview(graphics, slotFont, previewX, top);
        drawPreview(graphics, selectedItemFont, previewX, top + 24);
        drawPreview(graphics, tooltipFont, previewX, top + 48);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void save() {
        StackLimitConfig.saveCountFonts(slotFont, selectedItemFont, tooltipFont, fontPriority, uniformSlotCountHeight);
    }

    private int contentTop() {
        return panelTop() + 50;
    }

    private int panelTop() {
        return Math.max(4, (this.height - PANEL_HEIGHT) / 2);
    }

    private void drawPreview(DrawContext graphics, StackLimitConfig.CountFont previewFont, int x, int y) {
        Text sample = StackPlusFontSupport.applyPreview(Text.literal("500M"), previewFont);
        int sampleWidth = this.textRenderer.getWidth(sample);
        int sampleX = x + (PREVIEW_WIDTH - sampleWidth) / 2;
        int sampleY = Math.round(y + 10.0F - getVisualCenter(sample));
        graphics.fill(x, y, x + PREVIEW_WIDTH, y + 20, PREVIEW_COLOR);
        drawBorder(graphics, x, y, PREVIEW_WIDTH, 20);
        graphics.drawText(this.textRenderer, sample, sampleX, sampleY, 0xFFFFFFFF, true);
    }

    private float getVisualCenter(Text text) {
        return this.textRenderer.fontHeight / 2.0F;
    }

    private static void drawBorder(DrawContext graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, PANEL_BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, PANEL_BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, PANEL_BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER_COLOR);
    }

    private static Text fontText(String labelKey, StackLimitConfig.CountFont font) {
        String fontKey = switch (font) {
            case DEFAULT -> "screen.stackplus.font_config.font.minecraft";
            case JETBRAINS_MONO_EXTRA_BOLD -> "screen.stackplus.font_config.font.jetbrains_mono_extra_bold";
            case ROBOTO_CONDENSED_BLACK -> "screen.stackplus.font_config.font.roboto_condensed_black";
            case BUNGEE -> "screen.stackplus.font_config.font.bungee";
            case BLACK_OPS_ONE -> "screen.stackplus.font_config.font.black_ops_one";
            case LILITA_ONE -> "screen.stackplus.font_config.font.lilita_one";
            case PRESS_START_2P -> "screen.stackplus.font_config.font.press_start_2p";
            case CUSTOM -> "screen.stackplus.font_config.font.custom";
        };
        return Text.translatable(labelKey, Text.translatable(fontKey));
    }

    private static Text priorityText(StackLimitConfig.FontPriority priority) {
        String priorityKey = priority == StackLimitConfig.FontPriority.LOW
                ? "screen.stackplus.font_config.priority.low"
                : "screen.stackplus.font_config.priority.high";
        return Text.translatable("screen.stackplus.font_config.priority", Text.translatable(priorityKey));
    }

    private static Text sizeModeText(boolean uniformHeight) {
        String modeKey = uniformHeight
                ? "screen.stackplus.font_config.size_mode.uniform"
                : "screen.stackplus.font_config.size_mode.legacy";
        return Text.translatable("screen.stackplus.font_config.size_mode", Text.translatable(modeKey));
    }
}
