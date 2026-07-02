package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * クライアント側でItemInstanceの最大スタック数を補正
 * 優先度を高く設定
 */
@Mixin(value = ItemInstance.class, priority = 1500)
public interface ItemStackClientMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void customMaxCountClient(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (self instanceof ItemStack stack) {
            cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
            return;
        }

        ItemInstance stack = (ItemInstance) self;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack.typeHolder().value(), cir.getReturnValue()));
    }
}
