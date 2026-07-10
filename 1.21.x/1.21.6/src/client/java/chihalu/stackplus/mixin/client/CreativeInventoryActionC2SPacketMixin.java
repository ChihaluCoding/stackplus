package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * クリエイティブスロット同期パケットでStackPlusの許可上限を超える個数だけ丸めます。
 */
@Mixin(CreativeInventoryActionC2SPacket.class)
public class CreativeInventoryActionC2SPacketMixin {
    @Shadow
    @Final
    @Mutable
    private ItemStack stack;

    @Inject(method = "<init>(SLnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void stackplus$limitPacketStack(short slot, ItemStack stack, CallbackInfo ci) {
        this.stack = limitCreativePacketStack(stack);
    }

    @Inject(method = "<init>(ILnet/minecraft/item/ItemStack;)V", at = @At("RETURN"))
    private void stackplus$limitPacketStack(int slot, ItemStack stack, CallbackInfo ci) {
        this.stack = limitCreativePacketStack(stack);
    }

    @Inject(method = "stack", at = @At("HEAD"), cancellable = true)
    private void stackplus$returnLimitedPacketStack(CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(limitCreativePacketStack(stack));
    }

    private static ItemStack limitCreativePacketStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        int safeLimit = Math.max(1, Math.min(StackLimitConfig.MAX_STACK_LIMIT, StackLimitConfig.getAdjustedStackLimit(stack, stack.getMaxCount())));
        if (stack.getCount() <= safeLimit) {
            return stack;
        }

        ItemStack packetStack = stack.copy();
        packetStack.setCount(safeLimit);
        return packetStack;
    }
}
