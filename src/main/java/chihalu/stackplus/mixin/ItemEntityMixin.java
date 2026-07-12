package chihalu.stackplus.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.world.entity.item.ItemEntity;

/**
 * 地面に落ちたアイテム同士が合体する時の64個ハードコードを拡張します。
 * merge(ItemStack, ItemStack, int) 内部で Math.min(getMaxStackSize(), int) が使われるため、
 * 第3引数に大きな値を渡せばStackPlus補正済みのgetMaxStackSize()が効きます。
 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {

    @ModifyConstant(
            method = "merge(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 64),
            require = 0
    )
    private static int stackplus$expandMergeLimit(int original) {
        return Integer.MAX_VALUE;
    }
}

