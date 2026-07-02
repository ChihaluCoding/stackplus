package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * アイテム個数のGUI表示を1000個以上でK表記にします。
 */
@Mixin(DrawContext.class)
public class GuiGraphicsExtractorMixin {

    @Inject(method = "drawItemInSlot(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void customCountLabel(TextRenderer textRenderer, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        if (countLabel != null || stack.isEmpty() || stack.getCount() < 1000) {
            return;
        }

        ((DrawContext) (Object) this).drawItemInSlot(textRenderer, stack, x, y, StackCountFormatter.format(stack.getCount()));
        ci.cancel();
    }
}
