package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitMath;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.GiveCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * /give の maxStackSize * 100 が巨大スタック上限でオーバーフローするのを防ぎます。
 */
@Mixin(GiveCommand.class)
public class GiveCommandMixin {
    @ModifyExpressionValue(
            method = "execute",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getMaxCount()I")
    )
    private static int stackplus$preventGiveOverflow(int maxStackSize) {
        return StackLimitMath.safeGiveMaxStackSize(maxStackSize);
    }
}
