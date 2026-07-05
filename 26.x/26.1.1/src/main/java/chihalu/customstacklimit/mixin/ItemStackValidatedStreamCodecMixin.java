package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import com.mojang.serialization.DataResult;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * ネットワーク受信時のItemStack検証を、99個固定のCODEC検証から拡張後の上限検証へ差し替えます。
 */
@Mixin(targets = "net.minecraft.world.item.ItemStack$3")
public class ItemStackValidatedStreamCodecMixin {

    @Shadow
    @Final
    private StreamCodec<RegistryFriendlyByteBuf, ItemStack> val$codec;

    /**
     * @author chihalu
     * @reason StackPlusで拡張したスタック数を、vanillaの99個固定検証で拒否させないため。
     */
    @Overwrite
    public ItemStack decode(RegistryFriendlyByteBuf buffer) {
        ItemStack stack = val$codec.decode(buffer);
        if (!stack.isEmpty()) {
            stack = validateOrClampStack(stack);
        }

        return stack;
    }

    private static ItemStack validateOrClampStack(ItemStack stack) {
        DataResult<ItemStack> validation = ItemStack.validateStrict(stack);
        if (validation.error().isEmpty()) {
            return stack;
        }

        int safeLimit = Math.max(1, Math.min(StackLimitConfig.MAX_STACK_LIMIT, StackLimitConfig.getAdjustedStackLimit(stack, stack.getMaxStackSize())));
        if (stack.getCount() <= safeLimit) {
            return stack;
        }

        ItemStack clampedStack = stack.copy();
        clampedStack.setCount(safeLimit);
        DataResult<ItemStack> clampedValidation = ItemStack.validateStrict(clampedStack);
        clampedValidation.error().ifPresent(error -> {
            throw new DecoderException(error.message());
        });
        return clampedStack;
    }
}
