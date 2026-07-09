package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * アイテム個数のGUI表示を設定された表示形式に整形します。
 */
@Mixin(DrawContext.class)
public class GuiGraphicsExtractorMixin {
    private static final float SLOT_SIZE = 16.0F;
    private static final float AUTO_FIT_MAX_WIDTH = 15.0F;
    private static final float THREE_CHARACTER_SCALE = 0.9F;
    private static final ThreadLocal<Boolean> STACKPLUS_SUPPRESS_CUSTOM_COUNT = ThreadLocal.withInitial(() -> false);

    @Inject(method = "drawStackOverlay(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void renderCustomCountLabel(TextRenderer textRenderer, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        if (STACKPLUS_SUPPRESS_CUSTOM_COUNT.get() || stack.isEmpty() || stack.getCount() == 1 && countLabel == null) {
            return;
        }

        String label = getCountLabel(stack, countLabel);
        if (shouldUseVanillaSizedLabel(label)) {
            return;
        }

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
        if (countLabel != null) {
            return countLabel;
        }

        int count = stack.getCount();
        if (shouldUseStackPlusLabel(count)) {
            return StackCountFormatter.format(count);
        }

        return String.valueOf(count);
    }

    private static boolean shouldUseVanillaSizedLabel(String label) {
        return label.length() <= 2 && label.chars().allMatch(Character::isDigit);
    }

    private static boolean shouldUseStackPlusLabel(int count) {
        if (StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99) {
            return count >= 100;
        }

        return count >= 1000;
    }

    private static void renderScaledCount(DrawContext context, TextRenderer textRenderer, String label, int x, int y) {
        Text labelText = Text.literal(label);
        int labelWidth = textRenderer.getWidth(label);
        float scale = getCountScale(label, labelWidth);
        float labelX = getCountX(label, x, labelWidth, scale);
        float labelY = getCountY(textRenderer, y, scale);
        MatrixStack matrices = context.getMatrices();

        matrices.push();
        matrices.translate(labelX, labelY, 200.0F);
        matrices.scale(scale, scale, 1.0F);
        context.drawText(textRenderer, labelText, 0, 0, 0xFFFFFFFF, true);
        matrices.pop();
    }

    private static float getCountScale(String label, int labelWidth) {
        if (label.length() <= 3) {
            return label.length() == 3 ? THREE_CHARACTER_SCALE : 1.0F;
        }
        if (labelWidth <= 0) {
            return 1.0F;
        }

        float widthScale = AUTO_FIT_MAX_WIDTH / labelWidth;
        return Math.min(1.0F, widthScale);
    }

    private static float getCountX(String label, int x, int labelWidth, float scale) {
        if (label.length() == 4) {
            return x + (SLOT_SIZE - labelWidth * scale) / 2.0F;
        }

        return x + 17.0F - labelWidth * scale;
    }

    private static float getCountY(TextRenderer textRenderer, int y, float scale) {
        return y + 18.0F - textRenderer.fontHeight * scale;
    }
}
