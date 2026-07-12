package chihalu.stackplus.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** 地面に落ちたアイテム同士が合体する時の64個固定上限を拡張します。 */
@Mixin(ItemEntity.class)
public class ItemEntityMixin {
    @ModifyConstant(
            method = "merge(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ItemStack;)V",
            constant = @Constant(intValue = 64),
            require = 0
    )
    private static int stackplus$expandMergeLimit(int original) {
        return Integer.MAX_VALUE;
    }
}

