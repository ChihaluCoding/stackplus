package chihalu.customstacklimit.mixin;

import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ネットワーク受信時のItemStack検証を、99個固定のCODEC検証から拡張後の上限検証へ差し替えます。
 */
@Mixin(targets = "net.minecraft.world.item.ItemStack$3")
public class ItemStackValidatedStreamCodecMixin {

    @Shadow
    @Final
    private StreamCodec<RegistryFriendlyByteBuf, ItemStack> val$codec;

    @Inject(method = "decode(Lnet/minecraft/network/RegistryFriendlyByteBuf;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private void customDecode(RegistryFriendlyByteBuf buffer, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = val$codec.decode(buffer);
        if (!stack.isEmpty()) {
            DataResult<ItemStack> validation = ItemStack.validateStrict(stack);
            validation.error().ifPresent(error -> {
                throw new DecoderException(error.message());
            });
        }

        cir.setReturnValue(stack);
    }
}
