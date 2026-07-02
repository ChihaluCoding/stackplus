package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * スロット投入時にコンテナ側の上限で99個に丸められないよう補正します。
 */
@Mixin(Slot.class)
public class SlotMixin {

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void customSlotMaxCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(), StackLimitConfig.getStackLimit()));
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void customSlotMaxCountForStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            return;
        }

        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack.typeHolder().value(), cir.getReturnValue()));
    }
}
