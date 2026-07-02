package chihalu.customstacklimit.mixin.client;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;

/**
 * インベントリ画面描画に対応するMixin
 * 通常アイテムへ常時1000個のスタック制限を適用しているため、
 * プレイヤーコンテキストの設定は不要
 */
@Mixin(AbstractContainerScreen.class)
public class HandledScreenMixin {
}
