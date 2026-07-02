package chihalu.customstacklimit.mixin.client;

import chihalu.customstacklimit.StackCountFormatter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * スタックされたアイテムの正確な個数をツールチップへ追加します。
 */
@Mixin(ItemStack.class)
public class ItemStackTooltipMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"))
    private void appendExactStackCount(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (stack.isEmpty() || !shouldAppendExactStackCount(stack.getCount())) {
            return;
        }

        cir.getReturnValue().add(Text.translatable("tooltip.stackplus.count", StackCountFormatter.formatExact(stack.getCount())).formatted(Formatting.GRAY));
    }

    private static boolean shouldAppendExactStackCount(int count) {
        return count > 1;
    }
}
