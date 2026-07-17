package chihalu.stackplus.mixin.compat;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Iron Furnacesの燃料スロットでは、使用後に容器が残る燃料を1個ずつ扱います。 */
@Pseudo
@Mixin(targets = "ironfurnaces.container.slots.SlotIronFurnaceFuel", remap = false)
public abstract class IronFurnacesFuelSlotMixin {

    @Inject(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("HEAD"),
            cancellable = true, require = 0, remap = true)
    private void stackplus$limitContainerFuel(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getCraftingRemainder() != null) {
            cir.setReturnValue(1);
        }
    }
}
