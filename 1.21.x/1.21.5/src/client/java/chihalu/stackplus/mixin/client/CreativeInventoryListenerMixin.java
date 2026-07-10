package chihalu.stackplus.mixin.client;

import chihalu.stackplus.CustomStackLimit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryListener;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.c2s.play.CreativeInventoryActionC2SPacket;
import net.minecraft.screen.ScreenHandler;
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
    private static int warningCount;

    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onSlotUpdate", at = @At("HEAD"), cancellable = true)
    private void stackplus$skipInvalidCreativeSlot(ScreenHandler handler, int slotId, ItemStack stack, CallbackInfo ci) {
        if (!canEncodeCreativeSlot(slotId, stack)) {
            if (warningCount < MAX_WARNINGS) {
                CustomStackLimit.LOGGER.warn("StackPlus: デコード不能なクリエイティブスロット同期をスキップしました。slot={}, item={}, count={}",
                        slotId, stack.getItem(), stack.getCount());
                warningCount++;
            }
            ci.cancel();
        }
    }

    private boolean canEncodeCreativeSlot(int slot, ItemStack stack) {
        if (client.getNetworkHandler() == null) {
            return true;
        }

        ByteBuf rawBuffer = Unpooled.buffer();
        try {
            RegistryByteBuf buffer = new RegistryByteBuf(rawBuffer, client.getNetworkHandler().getRegistryManager());
            CreativeInventoryActionC2SPacket.CODEC.encode(buffer, new CreativeInventoryActionC2SPacket(slot, stack));
            return true;
        } catch (RuntimeException exception) {
            return false;
        } finally {
            rawBuffer.release();
        }
    }
}
