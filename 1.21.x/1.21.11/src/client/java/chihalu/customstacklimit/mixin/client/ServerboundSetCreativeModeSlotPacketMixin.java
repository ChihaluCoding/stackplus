package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * クリエイティブスロット同期パケットでStackPlusの許可上限を超える個数だけ丸めます。
 */
@Mixin(ServerboundSetCreativeModeSlotPacket.class)
public class ServerboundSetCreativeModeSlotPacketMixin {
    @Shadow
    @Final
    @Mutable
    private ItemStack itemStack;

    @Inject(method = "<init>(SLnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void stackplus$limitPacketStack(short slotNum, ItemStack itemStack, CallbackInfo ci) {
        this.itemStack = limitCreativePacketStack(itemStack);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/item/ItemStack;)V", at = @At("RETURN"))
    private void stackplus$limitPacketStack(int slotNum, ItemStack itemStack, CallbackInfo ci) {
        this.itemStack = limitCreativePacketStack(itemStack);
    }

    @Inject(method = "itemStack", at = @At("HEAD"), cancellable = true)
    private void stackplus$returnLimitedPacketStack(CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(limitCreativePacketStack(itemStack));
    }

    private static ItemStack limitCreativePacketStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        return StackLimitConfig.clampStackCount(stack);
    }
}
