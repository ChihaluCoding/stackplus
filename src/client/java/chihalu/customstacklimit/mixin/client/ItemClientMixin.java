package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * クライアント側でItemのデフォルト最大スタック数を補正
 * 優先度を高く設定して他のMixinより先に実行
 */
@Mixin(value = Item.class, priority = 1500)
public class ItemClientMixin {

    @Inject(method = "getMaxCount", at = @At("RETURN"), cancellable = true)
    private void customMaxCountClient(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit((Item) (Object) this, cir.getReturnValue()));
    }
}
