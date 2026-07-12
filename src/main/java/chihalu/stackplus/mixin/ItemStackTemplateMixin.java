package chihalu.stackplus.mixin;

import chihalu.stackplus.StackLimitConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.serialization.Codec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStackTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * ItemStackTemplateのCODECに残る99個制限を拡張します。
 * バンドル内容物などの保存・検証経路で99個超のスタックが弾かれるのを防ぎます。
 */
@Mixin(ItemStackTemplate.class)
public class ItemStackTemplateMixin {

    @ModifyExpressionValue(method = "lambda$static$0", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/ExtraCodecs;intRange(II)Lcom/mojang/serialization/Codec;"))
    private static Codec<Integer> stackplus$expandTemplateStackCount(Codec<Integer> original) {
        return ExtraCodecs.intRange(1, StackLimitConfig.MAX_STACK_LIMIT);
    }
}
