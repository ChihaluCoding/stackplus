package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.FurnaceFuelSlot;
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
    private static final String IRON_FURNACES_FUEL_SLOT =
            "ironfurnaces.container.slots.SlotIronFurnaceFuel";

    @Inject(method = "getMaxStackSize", at = @At("RETURN"), cancellable = true)
    private void customSlotMaxCount(CallbackInfoReturnable<Integer> cir) {
        int original = cir.getReturnValue();
        if (original <= 1) {
            return;
        }
        cir.setReturnValue(Math.max(original, StackLimitConfig.getStackLimit()));
    }

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"), cancellable = true)
    private void customSlotMaxCountForStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            return;
        }
        if (((Object) this instanceof FurnaceFuelSlot || getClass().getName().equals(IRON_FURNACES_FUEL_SLOT))
                && stack.get(DataComponents.USE_REMAINDER) != null) {
            cir.setReturnValue(1);
            return;
        }

        cir.setReturnValue(StackLimitConfig.getAdjustedStackLimit(stack, cir.getReturnValue()));
    }
}
