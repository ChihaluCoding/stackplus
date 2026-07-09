package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.codec.StackPlusCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ItemStackWithSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 一般コンテナ保存データのItemStackWithSlot CODECだけをStackPlus対応へ差し替えます。
 */
@Mixin(ContainerHelper.class)
public class ContainerHelperSaveDataMixin {
    @ModifyArg(
            method = "saveAllItems(Lnet/minecraft/world/level/storage/ValueOutput;Lnet/minecraft/core/NonNullList;Z)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueOutput;list(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;"),
            index = 1
    )
    private static Codec<ItemStackWithSlot> stackplus$useItemsSaveCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }

    @ModifyArg(
            method = "loadAllItems",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueInput;listOrEmpty(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;"),
            index = 1
    )
    private static Codec<ItemStackWithSlot> stackplus$useItemsLoadCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }
}
