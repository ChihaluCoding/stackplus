package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ホッパー移送の満杯判定とマージ容量計算にStackPlusの上限を適用します。
 * Redirectを使わず戻り値だけを広げ、他MODの同じ呼び出し差し替えと競合しにくくします。
 */
@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin {

    @ModifyExpressionValue(
            method = "inventoryFull",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private int customInventoryFullMaxStackSize(int original) {
        return getStackPlusMaxStackSize(original);
    }

    @ModifyExpressionValue(
            method = "isFullContainer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int customIsFullContainerMaxStackSize(int original) {
        return getStackPlusMaxStackSize(original);
    }

    @ModifyExpressionValue(
            method = "canMergeItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int customCanMergeItemsMaxStackSize(int original) {
        return getStackPlusMaxStackSize(original);
    }

    @ModifyExpressionValue(
            method = "tryMoveInItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int customTryMoveInItemMaxStackSize(int original) {
        return getStackPlusMaxStackSize(original);
    }

    private static int getStackPlusMaxStackSize(int originalLimit) {
        if (originalLimit <= 1) {
            return originalLimit;
        }

        return Math.max(originalLimit, StackLimitConfig.getAdjustedStackLimit(originalLimit));
    }
}
