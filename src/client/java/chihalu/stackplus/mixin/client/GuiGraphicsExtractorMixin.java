package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackCountFormatter;
import chihalu.stackplus.StackLimitConfig;
import chihalu.stackplus.client.StackPlusFontSupport;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * アイテム個数のGUI表示を設定された表示形式に整形します。
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {
    private static final float AUTO_FIT_MAX_WIDTH = 16.0F;
    private static final float UNIFORM_TARGET_VISUAL_HEIGHT = 7.0F;
    private static final float LEGACY_TARGET_VISUAL_HEIGHT = 9.0F;
    private static final float LEGACY_MAX_UPSCALE = 1.5F;
    private static final float COUNT_BOTTOM_OFFSET = 17.0F;

    @Shadow
    private Matrix3x2fStack pose;

    @Shadow
    public abstract void text(Font font, Component component, int x, int y, int color, boolean dropShadow);

    @Inject(method = "itemCount", at = @At("HEAD"), cancellable = true)
    private void renderCustomItemCount(Font font, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        if (stack.getCount() == 1 && countLabel == null) {
            return;
        }

        String label = getCountLabel(stack, countLabel);
        Component labelComponent = StackPlusFontSupport.apply(Component.literal(label), StackLimitConfig.getSlotCountFont());

        float scale = getCountScale(label, font, labelComponent);
        VisualBounds visualBounds = getVisualBounds(font, labelComponent);
        float labelX = x + 17.0F - font.width(labelComponent) * scale;
        float labelY = y + COUNT_BOTTOM_OFFSET - visualBounds.bottom() * scale;

        pose.pushMatrix();
        pose.translate(labelX, labelY);
        pose.scale(scale, scale);
        text(font, labelComponent, 0, 0, 0xFFFFFFFF, true);
        pose.popMatrix();
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

    private static float getCountScale(String label, Font font, Component labelComponent) {
        char widestDigit = getWidestCharacter("0123456789", font, labelComponent);
        char widestUnit = getWidestCharacter("KMB", font, labelComponent);

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

        Component widthReference = Component.literal(reference.toString())
                .withStyle(labelComponent.getStyle());
        VisualBounds referenceBounds = getVisualBounds(font, widthReference);
        if (referenceBounds.width() <= 0 || referenceBounds.height() <= 0) {
            return 1.0F;
        }

        float widthScale = AUTO_FIT_MAX_WIDTH / referenceBounds.width();
        if (StackLimitConfig.isUniformSlotCountHeight()) {
            float heightScale = UNIFORM_TARGET_VISUAL_HEIGHT / referenceBounds.height();
            return Math.min(widthScale, heightScale);
        }

        float heightScale = LEGACY_TARGET_VISUAL_HEIGHT / referenceBounds.height();
        return Math.min(LEGACY_MAX_UPSCALE, Math.min(widthScale, heightScale));
    }

    private static char getWidestCharacter(String candidates, Font font, Component styledComponent) {
        char widest = candidates.charAt(0);
        int widestWidth = 0;
        for (int index = 0; index < candidates.length(); index++) {
            char candidate = candidates.charAt(index);
            int width = font.width(Component.literal(String.valueOf(candidate)).withStyle(styledComponent.getStyle()));
            if (width > widestWidth) {
                widest = candidate;
                widestWidth = width;
            }
        }
        return widest;
    }

    private static VisualBounds getVisualBounds(Font font, Component component) {
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        font.prepareText(component.getVisualOrderText(), 0.0F, 0.0F,
                0xFFFFFFFF, true, false, 0).visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled renderable) {
                bounds[0] = Math.min(bounds[0], renderable.left());
                bounds[1] = Math.min(bounds[1], renderable.top());
                bounds[2] = Math.max(bounds[2], renderable.right());
                bounds[3] = Math.max(bounds[3], renderable.bottom());
            }
        });
        if (bounds[0] >= bounds[2] || bounds[1] >= bounds[3]) {
            return new VisualBounds(0.0F, 0.0F, Math.max(1, font.width(component)), font.lineHeight);
        }
        return new VisualBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    private record VisualBounds(float left, float top, float right, float bottom) {
        private float width() {
            return right - left;
        }

        private float height() {
            return bottom - top;
        }
    }

    private static boolean shouldUseStackPlusLabel(int count) {
        if (StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99) {
            return count >= 100;
        }

        return count >= 1000;
    }

}
