package chihalu.customstacklimit.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.commands.GiveCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * /giveコマンドの上限チェック maxStackSize * 100 が
 * StackPlusの大きなスタック上限でintオーバーフローするのを防ぎます。
 * getMaxStackSizeの戻り値を Integer.MAX_VALUE / 100 にクランプし、
 * 乗算結果がint範囲に収まるようにします。
 */
@Mixin(GiveCommand.class)
public class GiveCommandMixin {

    @ModifyExpressionValue(
            method = "giveItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int stackplus$preventGiveOverflow(int maxStackSize) {
        if (maxStackSize > Integer.MAX_VALUE / 100) {
            return Integer.MAX_VALUE / 100;
        }
        return maxStackSize;
    }
}
