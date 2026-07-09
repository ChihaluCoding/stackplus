package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
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
    private static final float SLOT_SIZE = 16.0F;
    private static final float AUTO_FIT_MAX_WIDTH = 15.0F;
    private static final float THREE_CHARACTER_SCALE = 0.9F;

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
        if (shouldUseVanillaSizedLabel(label)) {
            return;
        }

        Component labelComponent = Component.literal(label);

        float scale = getCountScale(label, font, labelComponent);
        int labelWidth = font.width(labelComponent);
        float labelX = getCountX(label, x, labelWidth, scale);
        float labelY = getCountY(font, y, scale);

        pose.pushMatrix();
        pose.translate(labelX, labelY);
        pose.scale(scale, scale);
        text(font, labelComponent, 0, 0, 0xFFFFFFFF, true);
        pose.popMatrix();
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

    private static float getCountScale(String label, Font font, Component labelComponent) {
        if (label.length() <= 3) {
            return label.length() == 3 ? THREE_CHARACTER_SCALE : 1.0F;
        }

        int labelWidth = font.width(labelComponent);
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

    private static float getCountY(Font font, int y, float scale) {
        return y + 18.0F - font.lineHeight * scale;
    }

    private static boolean shouldUseStackPlusLabel(int count) {
        if (StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99) {
            return count >= 100;
        }

        return count >= 1000;
    }
}
