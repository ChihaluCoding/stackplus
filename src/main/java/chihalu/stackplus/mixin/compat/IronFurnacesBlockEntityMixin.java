package chihalu.stackplus.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Iron Furnacesの精錬出力に残る64個固定の容量判定を拡張します。 */
@Pseudo
@Mixin(targets = "ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase", remap = false)
public abstract class IronFurnacesBlockEntityMixin {
    private static final int OUTPUT_SLOT = 2;

    @ModifyConstant(method = "canSmelt", constant = @Constant(intValue = 64), require = 0)
    private int stackplus$useOutputSlotLimit(int original) {
        ItemStack output = ((Inventory) this).getStack(OUTPUT_SLOT);
        return output.isEmpty() ? original : output.getMaxCount();
    }

    @ModifyConstant(method = "smeltItemMult", constant = @Constant(intValue = 64), require = 0)
    private int stackplus$useRecipeOutputLimit(int original, @Local(ordinal = 1) ItemStack recipeOutput) {
        return recipeOutput.getMaxCount();
    }

    @ModifyConstant(method = "smeltFactoryItemMult", constant = @Constant(intValue = 64), require = 0)
    private int stackplus$useFactoryRecipeOutputLimit(int original, @Local(ordinal = 1) ItemStack recipeOutput) {
        return recipeOutput.getMaxCount();
    }

    /** 直接搬入でも、使用後に容器が残る燃料は1個ずつ移します。 */
    @Redirect(
            method = "autoIO",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/ItemStack;getMaxCount()I",
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private int stackplus$limitTransferredContainerFuel(ItemStack stack) {
        return stack.getItem().hasRecipeRemainder() ? 1 : stack.getMaxCount();
    }

}
