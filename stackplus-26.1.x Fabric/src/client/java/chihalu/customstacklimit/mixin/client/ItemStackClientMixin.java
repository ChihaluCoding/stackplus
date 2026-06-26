package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.world.item.ItemInstance;
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
        ItemInstance stack = (ItemInstance) (Object) this;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack.typeHolder().value(), cir.getReturnValue()));
    }
}
