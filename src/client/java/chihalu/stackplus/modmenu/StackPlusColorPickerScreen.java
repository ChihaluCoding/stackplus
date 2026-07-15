package chihalu.stackplus.modmenu;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.function.IntConsumer;

/** HSVカラーサークルから選択アイテム数の表示色を選ぶ画面です。 */
final class StackPlusColorPickerScreen extends Screen {
    private static final int PANEL_COLOR = 0x80000000;
    private static final int PANEL_BORDER_COLOR = 0xFFFFFFFF;
    private static final int WHEEL_RADIUS = 64;
    private static final int WHEEL_SIZE = WHEEL_RADIUS * 2 + 1;
    private static final int TEXTURE_SCALE = 4;
    private static final int TEXTURE_RADIUS = WHEEL_RADIUS * TEXTURE_SCALE;
    private static final int TEXTURE_SIZE = TEXTURE_RADIUS * 2 + 1;
    private static final Identifier WHEEL_TEXTURE_ID = Identifier.of("stackplus", "color_wheel");
    private final Screen parent;
    private final IntConsumer colorConsumer;
    private int selectedColorRgb;
    private boolean dragging;
    private NativeImageBackedTexture wheelTexture;

    private StackPlusColorPickerScreen(Screen parent, int initialColorRgb, IntConsumer colorConsumer) {
        super(Text.translatable("screen.stackplus.color_picker.title"));
        this.parent = parent;
        this.selectedColorRgb = initialColorRgb & 0xFFFFFF;
        this.colorConsumer = colorConsumer;
    }

    static void open(Screen parent, int initialColorRgb, IntConsumer colorConsumer) {
        MinecraftClient.getInstance().setScreen(new StackPlusColorPickerScreen(parent, initialColorRgb, colorConsumer));
    }

    @Override
    protected void init() {
        createWheelTexture();
        int buttonY = getWheelCenterY() + WHEEL_RADIUS + 18;
        int buttonWidth = 68;
        int buttonGap = 6;
        int firstButtonX = this.width / 2 - (buttonWidth * 3 + buttonGap * 2) / 2;
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.save"), button -> save())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.color_picker.save")))
                .dimensions(firstButtonX, buttonY, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.reset"), button -> resetColor())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.color_picker.reset")))
                .dimensions(firstButtonX + buttonWidth + buttonGap, buttonY, buttonWidth, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("button.stackplus.back"), button -> close())
                .tooltip(Tooltip.of(Text.translatable("tooltip.stackplus.settings.back")))
                .dimensions(firstButtonX + (buttonWidth + buttonGap) * 2, buttonY, buttonWidth, 20).build());
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = getWheelCenterY();
        int left = centerX - 120;
        int top = centerY - WHEEL_RADIUS - 40;
        int panelWidth = 240;
        int panelHeight = WHEEL_SIZE + 87;
        context.fill(left, top, left + panelWidth, top + panelHeight, PANEL_COLOR);
        context.drawBorder(left, top, panelWidth, panelHeight, PANEL_BORDER_COLOR);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = getWheelCenterY();
        context.drawTexture(RenderLayer::getGuiTextured, WHEEL_TEXTURE_ID, centerX - WHEEL_RADIUS, centerY - WHEEL_RADIUS,
                    0.0F, 0.0F, WHEEL_SIZE, WHEEL_SIZE,
                    TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE, TEXTURE_SIZE);
        drawSelectionMarker(context, centerX, centerY);
        context.drawCenteredTextWithShadow(this.textRenderer, title, centerX, centerY - WHEEL_RADIUS - 28, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("screen.stackplus.color_picker.hint"), centerX, centerY - WHEEL_RADIUS - 12, 0xFFB8B8B8);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(String.format("#%06X", selectedColorRgb)).styled(style -> style.withColor(selectedColorRgb)),
                centerX, centerY + WHEEL_RADIUS + 4, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0 || !selectColorAt(mouseX, mouseY)) {
            return false;
        }
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && dragging) {
            selectColorAt(mouseX, mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        if (wheelTexture != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(WHEEL_TEXTURE_ID);
            wheelTexture = null;
        }
        MinecraftClient.getInstance().setScreen(parent);
    }

    private void save() {
        colorConsumer.accept(selectedColorRgb);
    }

    private void resetColor() {
        selectedColorRgb = 0xFFFFFF;
    }

    private int getWheelCenterY() {
        return this.height / 2 - 10;
    }

    private boolean selectColorAt(double mouseX, double mouseY) {
        double dx = mouseX - this.width / 2.0;
        double dy = mouseY - getWheelCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);
        if (distance > WHEEL_RADIUS) {
            return false;
        }
        float hue = (float) ((Math.atan2(dy, dx) / (Math.PI * 2.0) + 1.0) % 1.0);
        float saturation = (float) Math.min(1.0, distance / WHEEL_RADIUS);
        selectedColorRgb = hsvToRgb(hue, saturation, 1.0F);
        return true;
    }

    private void createWheelTexture() {
        NativeImage image = new NativeImage(TEXTURE_SIZE, TEXTURE_SIZE, true);
        for (int y = -TEXTURE_RADIUS; y <= TEXTURE_RADIUS; y++) {
            for (int x = -TEXTURE_RADIUS; x <= TEXTURE_RADIUS; x++) {
                double distance = Math.sqrt(x * x + y * y);
                if (distance > TEXTURE_RADIUS + 0.5) {
                    continue;
                }
                float hue = (float) ((Math.atan2(y, x) / (Math.PI * 2.0) + 1.0) % 1.0);
                float saturation = (float) (distance / TEXTURE_RADIUS);
                int alpha = getEdgeAlpha(x, y);
                int rgb = hsvToRgb(hue, saturation, 1.0F);
                int abgr = (alpha << 24) | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
                image.setColorArgb(x + TEXTURE_RADIUS, y + TEXTURE_RADIUS, abgr);
            }
        }
        wheelTexture = new NativeImageBackedTexture(() -> "stackplus_color_wheel", image);
        wheelTexture.setFilter(true, false);
        MinecraftClient.getInstance().getTextureManager().registerTexture(WHEEL_TEXTURE_ID, wheelTexture);
    }

    private void drawSelectionMarker(DrawContext context, int centerX, int centerY) {
        float[] hsv = rgbToHsv(selectedColorRgb);
        double angle = hsv[0] * Math.PI * 2.0;
        int markerX = centerX + (int) Math.round(Math.cos(angle) * hsv[1] * WHEEL_RADIUS);
        int markerY = centerY + (int) Math.round(Math.sin(angle) * hsv[1] * WHEEL_RADIUS);
        context.drawBorder(markerX - 4, markerY - 4, 9, 9, 0xFF000000);
        context.drawBorder(markerX - 3, markerY - 3, 7, 7, 0xFFFFFFFF);
    }

    private static int getEdgeAlpha(int x, int y) {
        double distanceFromEdge = TEXTURE_RADIUS - Math.sqrt(x * x + y * y);
        double opacity = Math.max(0.0, Math.min(1.0, distanceFromEdge / (2.0 * TEXTURE_SCALE)));
        return (int) Math.round(opacity * 255.0);
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float chroma = value * saturation;
        float section = hue * 6.0F;
        float x = chroma * (1.0F - Math.abs(section % 2.0F - 1.0F));
        float red = 0, green = 0, blue = 0;
        if (section < 1) { red = chroma; green = x; }
        else if (section < 2) { red = x; green = chroma; }
        else if (section < 3) { green = chroma; blue = x; }
        else if (section < 4) { green = x; blue = chroma; }
        else if (section < 5) { red = x; blue = chroma; }
        else { red = chroma; blue = x; }
        float match = value - chroma;
        return ((int) ((red + match) * 255) << 16) | ((int) ((green + match) * 255) << 8) | (int) ((blue + match) * 255);
    }

    private static float[] rgbToHsv(int colorRgb) {
        float red = ((colorRgb >> 16) & 0xFF) / 255.0F;
        float green = ((colorRgb >> 8) & 0xFF) / 255.0F;
        float blue = (colorRgb & 0xFF) / 255.0F;
        float maximum = Math.max(red, Math.max(green, blue));
        float minimum = Math.min(red, Math.min(green, blue));
        float delta = maximum - minimum;
        float hue = delta == 0 ? 0 : red == maximum ? ((green - blue) / delta + 6) % 6 / 6.0F
                : green == maximum ? ((blue - red) / delta + 2) / 6.0F : ((red - green) / delta + 4) / 6.0F;
        return new float[]{hue, maximum == 0 ? 0 : delta / maximum, maximum};
    }
}
