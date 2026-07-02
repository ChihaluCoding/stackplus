package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * アイテム個数のGUI表示を設定された表示形式に整形します。
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
        if (stack == null || stack.isEmpty()) {
            return countLabel;
        }

        if (!shouldUseStackPlusLabel(stack.getCount())) {
            return countLabel;
        }

        return StackCountFormatter.format(stack.getCount());
    }

    private static boolean shouldUseStackPlusLabel(int count) {
        if (StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99) {
            return count >= 100;
        }

        return count >= 1000;
    }

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At("RETURN"))
    private void clearDecorationStack(Font font, ItemStack stack, int x, int y, String countLabel, CallbackInfo ci) {
        STACKPLUS_DECORATION_STACK.remove();
    }
}
