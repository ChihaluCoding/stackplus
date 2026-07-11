package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitMath;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.commands.GiveCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * /give の maxStackSize * 100 が巨大スタック上限でオーバーフローするのを防ぎます。
 */
@Mixin(GiveCommand.class)
public class GiveCommandMixin {
    @ModifyExpressionValue(
            method = "giveItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getMaxStackSize()I")
    )
    private static int stackplus$preventGiveOverflow(int maxStackSize) {
        return StackLimitMath.safeGiveMaxStackSize(maxStackSize);
    }
}
