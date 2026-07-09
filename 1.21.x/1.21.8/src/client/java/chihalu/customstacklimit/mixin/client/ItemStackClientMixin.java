package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * クライアント側でItemStackの最大スタック数を補正
 * 優先度を高く設定
 */
@Mixin(value = ItemStack.class, priority = 1500)
public class ItemStackClientMixin {

    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    private void customMaxCountClient(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
    }
}
