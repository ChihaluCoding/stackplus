package chihalu.stackplus.mixin;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.item.BundleItem;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 巨大スタックを含むバンドルの容量表示を0〜1へ収めます。 */
@Mixin(BundleItem.class)
public class BundleItemMixin {
    @Inject(method = "getAmountFilled", at = @At("HEAD"), cancellable = true)
    private static void stackplus$fixBundleAmountFilled(ItemStack stack, CallbackInfoReturnable<Float> cir) {
        BundleContentsComponent contents = stack.getOrDefault(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT);
        float occupancy = contents.getOccupancy().floatValue();
        cir.setReturnValue(Math.max(0.0F, Math.min(1.0F, occupancy)));
    }
}
