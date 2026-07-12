package chihalu.stackplus.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.math.Fraction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 巨大スタックでバンドルの容量バーが異常値になるのを防ぎます。 */
@Mixin(BundleItem.class)
public class BundleItemMixin {
    @Inject(method = "getItemBarStep", at = @At("HEAD"), cancellable = true)
    private void stackplus$fixBundleBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        float occupancy = calculateBundleFullness(stack);
        cir.setReturnValue(Math.max(0, Math.min(1 + MathHelper.floor(occupancy * 12.0F), 13)));
    }

    @Inject(method = "getAmountFilled", at = @At("HEAD"), cancellable = true)
    private static void stackplus$fixBundleFullness(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(calculateBundleFullness(stack));
    }

    private static float calculateBundleFullness(ItemStack stack) {
        BundleContentsComponent contents = stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
        Fraction occupancy = contents.getOccupancy();
        if (occupancy.compareTo(Fraction.ZERO) < 0) {
            return 0.0F;
        }
        return Math.min(1.0F, occupancy.floatValue());
    }
}
