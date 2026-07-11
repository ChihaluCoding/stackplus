package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * チェストなどのコンテナがItemStackを保存する時に99個へ丸めないよう補正します。
 */
@Mixin(Inventory.class)
public interface ContainerMixin {

    @Inject(method = "getMaxCountPerStack", at = @At("RETURN"), cancellable = true)
    private void customContainerMaxCount(CallbackInfoReturnable<Integer> cir) {
        if (!StackLimitConfig.areStackRulesEnabled()) {
            return;
        }
        cir.setReturnValue(Math.max(cir.getReturnValue(), StackLimitConfig.getStackLimit()));
    }

    @Inject(method = "getMaxCount(Lnet/minecraft/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void customContainerMaxCountForStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            return;
        }

        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
    }
}
