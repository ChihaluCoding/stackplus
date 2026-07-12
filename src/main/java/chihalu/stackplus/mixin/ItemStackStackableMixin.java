package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 設定済みの耐久値付きアイテムだけ、ItemStack側のスタック不可判定を解除します。 */
@Mixin(ItemStack.class)
public class ItemStackStackableMixin {
    @Inject(method = "isStackable", at = @At("RETURN"), cancellable = true)
    private void customDamageableStackable(CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if ((!stack.isDamageableItem() || StackLimitConfig.hasConfiguredStackLimit(stack))
                && !StackLimitConfig.isItemStackingForbidden(stack.getItem())
                && stack.getMaxStackSize() > 1) {
            cir.setReturnValue(stack.getCount() < stack.getMaxStackSize());
        }
    }
}
