package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Itemのデフォルト最大スタック数を差し替えるMixin
 */
@Mixin(Item.class)
public class ItemMixin {

    /**
     * 元の上限が1個のアイテムは、明示的な個別設定がない限りスタック不可のまま維持します。
     */
    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    private void customMaxCount(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit((Item) (Object) this, cir.getReturnValue()));
    }
}
