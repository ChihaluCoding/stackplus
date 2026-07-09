package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.codec.StackPlusCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * ドロップアイテム保存データのItemStack CODECだけをStackPlus対応へ差し替えます。
 */
@Mixin(ItemEntity.class)
public class ItemEntitySaveDataMixin {
    @ModifyArg(
            method = "addAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueOutput;store(Ljava/lang/String;Lcom/mojang/serialization/Codec;Ljava/lang/Object;)V"),
            index = 1
    )
    private Codec<ItemStack> stackplus$useItemSaveCodec(Codec<ItemStack> original) {
        return StackPlusCodecs.ITEM_STACK_CODEC;
    }

    @ModifyArg(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueInput;read(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Ljava/util/Optional;", ordinal = 1),
            index = 1
    )
    private Codec<ItemStack> stackplus$useItemLoadCodec(Codec<ItemStack> original) {
        return StackPlusCodecs.ITEM_STACK_CODEC;
    }
}
