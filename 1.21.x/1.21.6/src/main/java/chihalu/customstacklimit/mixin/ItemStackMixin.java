package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemStackの最大スタック数を補正するMixin
 */
@Mixin(ItemStack.class)
public class ItemStackMixin {

    /**
     * CODECは起動時に構築されるため、ゲーム中に変更できる現在値ではなく設定可能な最大値まで許可します。
     */
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 99), require = 0)
    private static int customCodecMaxCount(int maxCount) {
        return StackLimitConfig.MAX_STACK_LIMIT;
    }

    /**
     * 1.21.xではItemStack.CODECのcount範囲が合成メソッド側で作られるため、こちらも最大許可値まで拡張します。
     */
    @ModifyConstant(method = "method_57371", constant = @Constant(intValue = 99), require = 1, remap = false)
    private static int customRecordCodecMaxCount(int maxCount) {
        return StackLimitConfig.MAX_STACK_LIMIT;
    }

    /**
     * 指定対象だけを例外化し、それ以外の最大1個アイテムはスタック不可のまま維持します。
     */
    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    private void customMaxCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
    }
}
