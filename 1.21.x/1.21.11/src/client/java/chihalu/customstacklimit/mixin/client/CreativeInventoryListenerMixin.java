package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.CustomStackLimit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * クリエイティブインベントリを開いた時に、壊れたItemStackを同期して切断される問題を防ぎます。
 */
@Mixin(CreativeInventoryListener.class)
public class CreativeInventoryListenerMixin {
    private static final int MAX_WARNINGS = 5;
    private int warningCount;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "slotChanged", at = @At("HEAD"), cancellable = true)
    private void stackplus$skipInvalidCreativeSlot(AbstractContainerMenu containerToSend, int slotInd, ItemStack stack, CallbackInfo ci) {
        if (!canEncodeCreativeSlot(slotInd, stack)) {
            if (warningCount < MAX_WARNINGS) {
                CustomStackLimit.LOGGER.warn("StackPlus: デコード不能なクリエイティブスロット同期をスキップしました。slot={}, item={}, count={}",
                        slotInd, stack.getItem(), stack.getCount());
                warningCount++;
            }
            ci.cancel();
        }
    }

    private boolean canEncodeCreativeSlot(int slot, ItemStack stack) {
        if (minecraft.getConnection() == null) {
            return true;
        }

        ByteBuf rawBuffer = Unpooled.buffer();
        try {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(rawBuffer, minecraft.getConnection().registryAccess());
            ServerboundSetCreativeModeSlotPacket.STREAM_CODEC.encode(buffer, new ServerboundSetCreativeModeSlotPacket(slot, stack));
            return true;
        } catch (RuntimeException exception) {
            return false;
        } finally {
            rawBuffer.release();
        }
    }
}
