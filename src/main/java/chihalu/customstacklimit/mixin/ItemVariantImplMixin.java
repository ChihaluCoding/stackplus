package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric Transfer APIのItemVariant最大スタック数をStackPlusの上限へ補正します。
 * ホッパー系ModがTransfer API経由で搬入する際に99個で止まる問題を防ぎます。
 */
@Mixin(targets = "net.fabricmc.fabric.impl.transfer.item.ItemVariantImpl")
public class ItemVariantImplMixin {

    @Inject(method = "getMaxStackSize(Lnet/fabricmc/fabric/api/transfer/v1/item/ItemVariant;)I", at = @At("RETURN"), cancellable = true)
    private static void customMaxCount(ItemVariant variant, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(variant.getItem(), cir.getReturnValue()));
    }
}