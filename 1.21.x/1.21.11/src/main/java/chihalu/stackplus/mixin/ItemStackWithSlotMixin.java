package chihalu.stackplus.mixin;

import chihalu.stackplus.codec.StackPlusCodecs;
import com.mojang.serialization.Codec;
import net.minecraft.world.ItemStackWithSlot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * コンテナ保存・読み込み用のItemStackWithSlot CODECを1000個対応へ差し替えます。
 */
@Mixin(ItemStackWithSlot.class)
public class ItemStackWithSlotMixin {

    @Shadow
    @Final
    @Mutable
    public static Codec<ItemStackWithSlot> CODEC;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void customCodec(CallbackInfo ci) {
        CODEC = StackPlusCodecs.ITEM_STACK_WITH_SLOT_CODEC;
    }
}
