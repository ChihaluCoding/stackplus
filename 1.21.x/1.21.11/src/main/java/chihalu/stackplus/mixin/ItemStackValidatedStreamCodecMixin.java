package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ItemStack.validatedStreamCodec が返す StreamCodec の検証を
 * 99個固定からStackPlus拡張後の上限へ差し替えます。
 * 匿名クラス（ItemStack$3）を直接参照せず、公開メソッド経由で差し替えます。
 */
@Mixin(ItemStack.class)
public class ItemStackValidatedStreamCodecMixin {

    @Inject(
            method = "validatedStreamCodec",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void stackplus$wrapValidatedStreamCodec(StreamCodec<RegistryFriendlyByteBuf, ItemStack> codec,
                                                            CallbackInfoReturnable<StreamCodec<RegistryFriendlyByteBuf, ItemStack>> cir) {
        cir.setReturnValue(new StreamCodec<>() {
            @Override
            public ItemStack decode(RegistryFriendlyByteBuf buffer) {
                ItemStack decoded = codec.decode(buffer);
                if (decoded.isEmpty()) {
                    return decoded;
                }
                return validateOrClampStack(decoded);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, ItemStack value) {
                codec.encode(buffer, value);
            }
        });
    }

    private static ItemStack validateOrClampStack(ItemStack stack) {
        DataResult<ItemStack> validation = ItemStack.validateStrict(stack);
        if (validation.error().isEmpty()) {
            return stack;
        }

        ItemStack clampedStack = StackLimitConfig.clampStackCount(stack);
        if (clampedStack.getCount() == stack.getCount()) {
            return stack;
        }
        DataResult<ItemStack> clampedValidation = ItemStack.validateStrict(clampedStack);
        clampedValidation.error().ifPresent(error -> {
            throw new DecoderException(error.message());
        });
        return clampedStack;
    }
}
