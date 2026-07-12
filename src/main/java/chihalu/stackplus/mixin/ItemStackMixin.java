package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
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
     * 元の上限が1個のアイテムは、明示的な個別設定がない限りスタック不可のまま維持します。
     */
    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void customMaxCount(CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (self instanceof ItemStack stack) {
            cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
            return;
        }

        ItemInstance stack = (ItemInstance) self;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack.typeHolder().value(), cir.getReturnValue()));
    }

}
