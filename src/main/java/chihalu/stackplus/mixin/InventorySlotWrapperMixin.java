package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric Transfer API経由の搬送でも、StackPlusのアイテム別上限を適用します。 */
@Mixin(targets = "net.fabricmc.fabric.impl.transfer.item.InventorySlotWrapper", remap = false)
public class InventorySlotWrapperMixin {

    @Inject(method = "getCapacity", at = @At("RETURN"), cancellable = true)
    private void stackplus$adjustTransferStackLimit(ItemVariant variant, CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = variant.toStack();
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
    }
}
