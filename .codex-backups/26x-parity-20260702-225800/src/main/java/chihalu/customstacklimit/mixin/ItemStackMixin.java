package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.world.item.ItemInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemInstanceの最大スタック数を補正するMixin
 */
@Mixin(ItemInstance.class)
public interface ItemStackMixin {

    /**
     * 元の上限が1個のアイテムは、バンドルや装備系を含めてスタック不可のまま維持します。
     */
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void customMaxCount(CallbackInfoReturnable<Integer> cir) {
        ItemInstance stack = (ItemInstance) (Object) this;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack.typeHolder().value(), cir.getReturnValue()));
    }
}
