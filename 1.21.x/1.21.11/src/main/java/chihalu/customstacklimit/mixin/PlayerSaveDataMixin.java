package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.codec.StackPlusCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * プレイヤー保存データのItemStackWithSlot CODECだけをStackPlus対応へ差し替えます。
 */
@Mixin(Player.class)
public class PlayerSaveDataMixin {
    @ModifyArg(
            method = "addAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueOutput;list(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;", ordinal = 0),
            index = 1
    )
    private Codec<ItemStackWithSlot> stackplus$useInventorySaveCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }

    @ModifyArg(
            method = "addAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueOutput;list(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;", ordinal = 1),
            index = 1
    )
    private Codec<ItemStackWithSlot> stackplus$useEnderItemsSaveCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }

    @ModifyArg(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueInput;listOrEmpty(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;", ordinal = 0),
            index = 1
    )
    private Codec<ItemStackWithSlot> stackplus$useInventoryLoadCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }

    @ModifyArg(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueInput;listOrEmpty(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;", ordinal = 1),
            index = 1
    )
    private Codec<ItemStackWithSlot> stackplus$useEnderItemsLoadCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }
}
