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
     * 1.21系の途中で追加されたItem側のCODECフィールドへ依存せず、vanilla CODEC内の99個検証だけを拡張します。
     */
    @ModifyConstant(method = "<clinit>", constant = @Constant(intValue = 99), require = 0)
    private static int customCodecMaxCount(int maxCount) {
        return StackLimitConfig.getStackLimit();
    }

    /**
     * 1.21.xではItemStack.CODECのcount範囲が合成メソッド側で作られるため、クリエイティブスロット送信時の再検証も拡張します。
     */
    @ModifyConstant(method = "method_57371", constant = @Constant(intValue = 99), require = 1, remap = false)
    private static int customRecordCodecMaxCount(int maxCount) {
        return StackLimitConfig.getStackLimit();
    }

    /**
     * 元の上限が1個のアイテムは、バンドルや装備系を含めてスタック不可のまま維持します。
     */
    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    private void customMaxCount(CallbackInfoReturnable<Integer> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack.getItem(), cir.getReturnValue()));
    }
}
