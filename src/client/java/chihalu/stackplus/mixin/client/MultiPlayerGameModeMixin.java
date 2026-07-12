package chihalu.stackplus.mixin.client;

import chihalu.stackplus.StackLimitConfig;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * クリエイティブ専用パケットでは、StackPlusの許可上限を超える個数だけ丸めます。
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @ModifyArg(
            method = "handleCreativeModeItemAdd",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;<init>(ILnet/minecraft/world/item/ItemStack;)V"),
            index = 1
    )
    private ItemStack stackplus$limitCreativeAddPacketStack(ItemStack stack) {
        return limitCreativePacketStack(stack);
    }

    @ModifyArg(
            method = "handleCreativeModeItemDrop",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundSetCreativeModeSlotPacket;<init>(ILnet/minecraft/world/item/ItemStack;)V"),
            index = 1
    )
    private ItemStack stackplus$limitCreativeDropPacketStack(ItemStack stack) {
        return limitCreativePacketStack(stack);
    }

    private static ItemStack limitCreativePacketStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }

        return StackLimitConfig.clampStackCount(stack);
    }
}

