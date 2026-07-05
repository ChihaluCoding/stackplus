package chihalu.customstacklimit.mixin;

import chihalu.customstacklimit.StackLimitConfig;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * PlayerInventoryの操作に対応するMixin
 * 通常アイテムへ常時1000個のスタック制限を適用しているため、
 * プレイヤーコンテキストの設定は不要
 */
@Mixin(Inventory.class)
public class PlayerInventoryMixin {

    @Shadow
    @Final
    public Player player;

    /**
     * Inventory#getMaxStackSize(ItemStack) は Container の default メソッドを継承しており、
     * Mixin の @Inject では直接上書きできません。
     * そのため、Inventory 内で実際に上限を参照している箇所を @Redirect で置き換え、
     * 99 個で区切られないようにします。
     */
    @Redirect(
            method = {
                    "hasRemainingSpaceForItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z",
                    "addResource(ILnet/minecraft/world/item/ItemStack;)I"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I")
    )
    private int customInventoryMaxCountForStack(Inventory inventory, ItemStack stack) {
        if (stack.isEmpty()) {
            return inventory.getMaxStackSize(stack);
        }

        return StackLimitConfig.getAdjustedStackLimit(stack, stack.getMaxStackSize());
    }
}
