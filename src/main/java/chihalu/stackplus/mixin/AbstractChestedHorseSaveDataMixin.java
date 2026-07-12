package chihalu.stackplus.mixin;

import chihalu.stackplus.codec.StackPlusCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.animal.equine.AbstractChestedHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * チェスト付き馬系エンティティの保存データのCODECだけをStackPlus対応へ差し替えます。
 */
@Mixin(AbstractChestedHorse.class)
public class AbstractChestedHorseSaveDataMixin {
    @ModifyArg(
            method = "addAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueOutput;list(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueOutput$TypedOutputList;"),
            index = 1
    )
    private Codec<ItemStackWithSlot> stackplus$useItemsSaveCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }

    @ModifyArg(
            method = "readAdditionalSaveData",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueInput;listOrEmpty(Ljava/lang/String;Lcom/mojang/serialization/Codec;)Lnet/minecraft/world/level/storage/ValueInput$TypedInputList;"),
            index = 1
    )
    private Codec<ItemStackWithSlot> stackplus$useItemsLoadCodec(Codec<ItemStackWithSlot> original) {
        return StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }
}

