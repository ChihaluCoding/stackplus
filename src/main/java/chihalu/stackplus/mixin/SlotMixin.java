package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * スロット投入時にコンテナ側の上限で99個に丸められないよう補正します。
 */
@Mixin(Slot.class)
public class SlotMixin {

    @Inject(method = "getMaxItemCount", at = @At("RETURN"), cancellable = true)
    private void customSlotMaxCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(), StackLimitConfig.getStackLimit()));
    }

    @Inject(method = "getMaxItemCount(Lnet/minecraft/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void customSlotMaxCountForStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            return;
        }

        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
    }
}
