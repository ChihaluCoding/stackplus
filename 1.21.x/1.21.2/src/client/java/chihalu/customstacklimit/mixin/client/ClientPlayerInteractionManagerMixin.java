package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * クリエイティブ専用パケットでは、StackPlusの許可上限を超える個数だけ丸めます。
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @ModifyArg(
            method = "clickCreativeStack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/c2s/play/CreativeInventoryActionC2SPacket;<init>(ILnet/minecraft/item/ItemStack;)V"),
            index = 1
    )
    private ItemStack stackplus$limitCreativePacketStack(ItemStack stack) {
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
