package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 短縮表示では分からない正確な個数をツールチップへ追加します。
 */
@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void appendExactStackCount(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty() || !shouldAppendExactStackCount(stack.getCount())) {
            return;
        }

        cir.getReturnValue().add(Component.translatable("tooltip.stackplus.count", StackCountFormatter.formatExact(stack.getCount())).withStyle(ChatFormatting.GRAY));
    }

    private static boolean shouldAppendExactStackCount(int count) {
        if (count >= 1000) {
            return true;
        }
        if (StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99 && count >= 100) {
            return true;
        }

        return count > StackLimitConfig.getStackLimit();
    }
}
