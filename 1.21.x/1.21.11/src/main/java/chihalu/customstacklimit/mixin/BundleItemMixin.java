package chihalu.customstacklimit.mixin;

import com.mojang.serialization.DataResult;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * バンドルの容量バーが Mod の最大スタック数変更の影響で負の幅や異常な値を返すのを防ぎます。
 * weight を 0.0F〜1.0F にクランプし、整数オーバーフローによる表示崩れを回避します。
 */
@Mixin(BundleItem.class)
public class BundleItemMixin {

    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void stackplus$fixBundleBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(calculateBundleBarWidth(stack));
    }

    @Inject(method = "getFullnessDisplay", at = @At("HEAD"), cancellable = true)
    private static void stackplus$fixBundleFullnessDisplay(ItemStack itemStack, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(calculateBundleFullness(itemStack));
    }

    private static int calculateBundleBarWidth(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        float weight = clampBundleWeight(contents.weight());
        return Math.max(0, Math.min(1 + Mth.floor(weight * 12.0F), 13));
    }

    private static float calculateBundleFullness(ItemStack stack) {
        BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
        return clampBundleWeight(contents.weight());
    }

    private static float clampBundleWeight(Fraction weight) {
        if (weight.compareTo(Fraction.ZERO) < 0) {
            return 0.0F;
        }

        return Math.min(1.0F, weight.floatValue());
    }
}
