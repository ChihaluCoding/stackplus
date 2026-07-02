package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * アイテム個数のGUI表示を1000個以上でK表記にします。
 */
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {
    private static final ThreadLocal<ItemStack> STACKPLUS_DECORATION_STACK = new ThreadLocal<>();

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("HEAD"))
    private void captureDecorationStack(Font font, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        STACKPLUS_DECORATION_STACK.set(stack);
    }

    @ModifyArg(
            method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;itemCount(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"),
            index = 4
    )
    private String customCountLabel(String countLabel) {
        if (countLabel != null) {
            return countLabel;
        }

        ItemStack stack = STACKPLUS_DECORATION_STACK.get();
        if (stack == null || stack.isEmpty() || stack.getCount() < 1000) {
            return countLabel;
        }

        return StackCountFormatter.format(stack.getCount());
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void clearDecorationStack(Font font, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        STACKPLUS_DECORATION_STACK.remove();
    }
}
