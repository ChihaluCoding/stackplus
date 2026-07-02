package chihalu.customstacklimit.mixin;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

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
}
