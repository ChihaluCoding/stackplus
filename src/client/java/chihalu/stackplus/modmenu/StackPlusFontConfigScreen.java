package chihalu.stackplus.modmenu;

import chihalu.stackplus.StackLimitConfig;
import chihalu.stackplus.client.StackPlusCustomFont;
import chihalu.stackplus.client.StackPlusFontSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        super(Component.translatable("screen.stackplus.font_config.title"));
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

        addRenderableWidget(Button.builder(fontText("screen.stackplus.font_config.slot", slotFont), button -> {
                    slotFont = slotFont.next();
                    button.setMessage(fontText("screen.stackplus.font_config.slot", slotFont));
                }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.slot")))
                .bounds(left, top, FONT_BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(fontText("screen.stackplus.font_config.selected_item", selectedItemFont), button -> {
                    selectedItemFont = selectedItemFont.next();
                    button.setMessage(fontText("screen.stackplus.font_config.selected_item", selectedItemFont));
                }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.selected_item")))
                .bounds(left, top + 24, FONT_BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(fontText("screen.stackplus.font_config.tooltip", tooltipFont), button -> {
                    tooltipFont = tooltipFont.next();
                    button.setMessage(fontText("screen.stackplus.font_config.tooltip", tooltipFont));
                }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.tooltip")))
                .bounds(left, top + 48, FONT_BUTTON_WIDTH, 20).build());

        int columnWidth = (WIDTH - 8) / 2;
        addRenderableWidget(Button.builder(priorityText(fontPriority), button -> {
                    fontPriority = fontPriority.next();
                    button.setMessage(priorityText(fontPriority));
                }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.priority")))
                .bounds(left, top + 76, columnWidth, 20).build());

        addRenderableWidget(Button.builder(sizeModeText(uniformSlotCountHeight), button -> {
                    uniformSlotCountHeight = !uniformSlotCountHeight;
                    button.setMessage(sizeModeText(uniformSlotCountHeight));
                }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.display_mode")))
                .bounds(left + columnWidth + 8, top + 76, columnWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.import_font"), button -> {
                    if (StackPlusCustomFont.importFont(Component.translatable("screen.stackplus.font_config.import_title").getString())) {
                        Minecraft.getInstance().reloadResourcePacks();
                    }
                }).tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.import")))
                .bounds(left, top + 100, columnWidth, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.reload_fonts"), button ->
                        {
                            StackPlusCustomFont.prepareFont();
                            Minecraft.getInstance().reloadResourcePacks();
                        })
                .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.font_config.reload")))
                .bounds(left + columnWidth + 8, top + 100, columnWidth, 20).build());

        int actionWidth = (WIDTH - 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.save"), button -> save())
                .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.settings.save")))
                .bounds(left, top + 128, actionWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("button.stackplus.back"), button -> onClose())
                .tooltip(Tooltip.create(Component.translatable("tooltip.stackplus.settings.back")))
                .bounds(left + actionWidth + 8, top + 128, actionWidth, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = this.width / 2 - WIDTH / 2 - 12;
        int panelTop = panelTop();
        int panelWidth = WIDTH + 24;
        graphics.fill(left, panelTop, left + panelWidth, panelTop + PANEL_HEIGHT, PANEL_COLOR);
        graphics.outline(left, panelTop, panelWidth, PANEL_HEIGHT, PANEL_BORDER_COLOR);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int top = contentTop();
        graphics.centeredText(this.font, this.title, this.width / 2, panelTop() + 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("screen.stackplus.font_config.minecraft_hint"),
                this.width / 2, Math.max(28, top - 22), 0xFFB8B8B8);

        int previewX = this.width / 2 - WIDTH / 2 + FONT_BUTTON_WIDTH + PREVIEW_GAP;
        drawPreview(graphics, slotFont, previewX, top);
        drawPreview(graphics, selectedItemFont, previewX, top + 24);
        drawPreview(graphics, tooltipFont, previewX, top + 48);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
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

    private void drawPreview(GuiGraphicsExtractor graphics, StackLimitConfig.CountFont previewFont, int x, int y) {
        Component sample = StackPlusFontSupport.applyPreview(Component.literal("500M"), previewFont);
        int sampleWidth = this.font.width(sample);
        int sampleX = x + (PREVIEW_WIDTH - sampleWidth) / 2;
        int sampleY = Math.round(y + 10.0F - getVisualCenter(sample));
        graphics.fill(x, y, x + PREVIEW_WIDTH, y + 20, PREVIEW_COLOR);
        graphics.outline(x, y, PREVIEW_WIDTH, 20, PANEL_BORDER_COLOR);
        graphics.text(this.font, sample, sampleX, sampleY, 0xFFFFFFFF, true);
    }

    private float getVisualCenter(Component text) {
        ScreenRectangle bounds = this.font.prepareText(text.getVisualOrderText(), 0.0F, 0.0F,
                0xFFFFFFFF, true, false, 0).bounds();
        return bounds == null ? this.font.lineHeight / 2.0F : bounds.top() + bounds.height() / 2.0F;
    }

    private static Component fontText(String labelKey, StackLimitConfig.CountFont font) {
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
        return Component.translatable(labelKey, Component.translatable(fontKey));
    }

    private static Component priorityText(StackLimitConfig.FontPriority priority) {
        String priorityKey = priority == StackLimitConfig.FontPriority.LOW
                ? "screen.stackplus.font_config.priority.low"
                : "screen.stackplus.font_config.priority.high";
        return Component.translatable("screen.stackplus.font_config.priority", Component.translatable(priorityKey));
    }

    private static Component sizeModeText(boolean uniformHeight) {
        String modeKey = uniformHeight
                ? "screen.stackplus.font_config.size_mode.uniform"
                : "screen.stackplus.font_config.size_mode.legacy";
        return Component.translatable("screen.stackplus.font_config.size_mode", Component.translatable(modeKey));
    }
}
