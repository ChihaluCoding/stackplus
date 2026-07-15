package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackCountFormatter;
import chihalu.stackplus.StackLimitConfig;
import chihalu.stackplus.client.StackPlusFontSupport;
import net.minecraft.client.font.BakedGlyph;
import net.minecraft.client.font.FontStorage;
import net.minecraft.client.font.Glyph;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** アイテム個数のGUI表示を設定された表示形式に整形します。 */
@Mixin(DrawContext.class)
public class GuiGraphicsExtractorMixin {
    private static final float AUTO_FIT_MAX_WIDTH = 16.0F;
    private static final float UNIFORM_TARGET_VISUAL_HEIGHT = 7.0F;
    private static final float LEGACY_TARGET_VISUAL_HEIGHT = 9.0F;
    private static final float LEGACY_MAX_UPSCALE = 1.5F;
    private static final float COUNT_BOTTOM_OFFSET = 17.0F;
    private static final ThreadLocal<Boolean> STACKPLUS_SUPPRESS_CUSTOM_COUNT = ThreadLocal.withInitial(() -> false);

    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void renderCustomCountLabel(TextRenderer textRenderer, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        if (STACKPLUS_SUPPRESS_CUSTOM_COUNT.get() || stack.isEmpty() || stack.getCount() == 1 && countLabel == null) {
            return;
        }

        String label = getCountLabel(stack, countLabel);
        DrawContext context = (DrawContext) (Object) this;
        STACKPLUS_SUPPRESS_CUSTOM_COUNT.set(true);
        try {
            context.drawStackOverlay(textRenderer, stack, x, y, "");
        } finally {
            STACKPLUS_SUPPRESS_CUSTOM_COUNT.set(false);
        }

        renderScaledCount(context, textRenderer, label, x, y);
        ci.cancel();
    }

    private static String getCountLabel(ItemStack stack, String countLabel) {
        if (countLabel != null && !isNumericCountLabel(countLabel)) {
            return countLabel;
        }
        int count = stack.getCount();
        return shouldUseStackPlusLabel(count) ? StackCountFormatter.format(count) : String.valueOf(count);
    }

    private static boolean isNumericCountLabel(String label) {
        if (label.isEmpty() || !Character.isDigit(label.charAt(0))) {
            return false;
        }
        for (int index = 1; index < label.length(); index++) {
            char character = label.charAt(index);
            boolean finalSuffix = index == label.length() - 1
                    && (character == 'K' || character == 'M' || character == 'B'
                    || character == 'k' || character == 'm' || character == 'b' || character == '+');
            if (!Character.isDigit(character) && character != '.' && character != ',' && !finalSuffix) {
                return false;
            }
        }
        return true;
    }

    private static void renderScaledCount(DrawContext context, TextRenderer textRenderer, String label, int x, int y) {
        Text labelText = StackPlusFontSupport.apply(Text.literal(label), StackLimitConfig.getSlotCountFont());
        float scale = getCountScale(label, textRenderer, labelText);
        VisualBounds visualBounds = getVisualBounds(textRenderer, labelText);
        float labelX = x + 17.0F - textRenderer.getWidth(labelText) * scale;
        float labelY = y + COUNT_BOTTOM_OFFSET - visualBounds.bottom() * scale;
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(labelX, labelY, 200.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, labelText, 0, 0, 0xFFFFFFFF, true);
        matrices.pop();
    }

    private static float getCountScale(String label, TextRenderer textRenderer, Text labelText) {
        char widestDigit = getWidestCharacter("0123456789", textRenderer, labelText);
        char widestUnit = getWidestCharacter("KMB", textRenderer, labelText);
        StringBuilder reference = new StringBuilder(label.length());
        for (int index = 0; index < label.length(); index++) {
            char character = label.charAt(index);
            if (Character.isDigit(character)) {
                reference.append(widestDigit);
            } else if (character == 'K' || character == 'M' || character == 'B'
                    || character == 'k' || character == 'm' || character == 'b') {
                reference.append(widestUnit);
            } else {
                reference.append(character);
            }
        }

        Text widthReference = Text.literal(reference.toString()).setStyle(labelText.getStyle());
        VisualBounds referenceBounds = getVisualBounds(textRenderer, widthReference);
        if (referenceBounds.width() <= 0 || referenceBounds.height() <= 0) {
            return 1.0F;
        }
        float widthScale = AUTO_FIT_MAX_WIDTH / referenceBounds.width();
        if (StackLimitConfig.isUniformSlotCountHeight()) {
            return Math.min(widthScale, UNIFORM_TARGET_VISUAL_HEIGHT / referenceBounds.height());
        }
        return Math.min(LEGACY_MAX_UPSCALE,
                Math.min(widthScale, LEGACY_TARGET_VISUAL_HEIGHT / referenceBounds.height()));
    }

    private static char getWidestCharacter(String candidates, TextRenderer textRenderer, Text styledText) {
        char widest = candidates.charAt(0);
        int widestWidth = 0;
        for (int index = 0; index < candidates.length(); index++) {
            char candidate = candidates.charAt(index);
            int width = textRenderer.getWidth(Text.literal(String.valueOf(candidate)).setStyle(styledText.getStyle()));
            if (width > widestWidth) {
                widest = candidate;
                widestWidth = width;
            }
        }
        return widest;
    }

    private static VisualBounds getVisualBounds(TextRenderer textRenderer, Text text) {
        Style style = text.getStyle();
        Identifier fontId = style.getFont() == null ? Style.DEFAULT_FONT_ID : style.getFont();
        FontStorage storage = ((TextRendererAccessor) textRenderer).stackplus$getFontStorage(fontId);
        float left = Float.MAX_VALUE;
        float top = Float.MAX_VALUE;
        float right = -Float.MAX_VALUE;
        float bottom = -Float.MAX_VALUE;
        float cursor = 0.0F;
        String value = text.getString();
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            Glyph glyph = storage.getGlyph(codePoint, false);
            BakedGlyph renderer = storage.getBaked(codePoint);
            GlyphRendererAccessor bounds = (GlyphRendererAccessor) renderer;
            left = Math.min(left, cursor + bounds.stackplus$getMinX());
            top = Math.min(top, bounds.stackplus$getMinY());
            right = Math.max(right, cursor + bounds.stackplus$getMaxX() + glyph.getShadowOffset());
            bottom = Math.max(bottom, bounds.stackplus$getMaxY() + glyph.getShadowOffset());
            cursor += glyph.getAdvance(style.isBold());
            offset += Character.charCount(codePoint);
        }
        if (left >= right || top >= bottom) {
            return new VisualBounds(0.0F, 0.0F, Math.max(1, textRenderer.getWidth(text)), textRenderer.fontHeight);
        }
        return new VisualBounds(left, top, right, bottom);
    }

    private static boolean shouldUseStackPlusLabel(int count) {
        return StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99
                ? count >= 100 : count >= 1000;
    }

    private record VisualBounds(float left, float top, float right, float bottom) {
        private float width() {
            return right - left;
        }

        private float height() {
            return bottom - top;
        }
    }
}
