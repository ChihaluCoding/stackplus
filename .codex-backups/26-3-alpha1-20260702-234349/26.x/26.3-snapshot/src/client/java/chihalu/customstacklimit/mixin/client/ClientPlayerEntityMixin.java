package chihalu.customstacklimit.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * クライアント側でのプレイヤーエンティティ操作に対応するMixin
 * 通常アイテムへ常時1000個のスタック制限を適用しているため、
 * プレイヤーコンテキストの設定は不要
 */
@Mixin(LocalPlayer.class)
public class ClientPlayerEntityMixin {
}
