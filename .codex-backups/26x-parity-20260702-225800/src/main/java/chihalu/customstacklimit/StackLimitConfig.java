package chihalu.customstacklimit;

import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.Item;

/**
 * スタック数設定を管理するクラス
 * 通常アイテムへ常時1000個のスタック制限を適用します
 */
public class StackLimitConfig {
    // 通常アイテムのスタック制限
    public static final int STACK_LIMIT = 1000;

    /**
     * スタック数を取得
     */
    public static int getStackLimit() {
        return STACK_LIMIT;
    }

    /**
     * 装備、ツール、武器、盾、バンドル、シュルカーボックスなど、
     * vanillaで非スタック扱いのアイテムは元の上限を維持します。
     */
    public static int getAdjustedStackLimit(int originalLimit) {
        if (originalLimit <= 1) {
            return originalLimit;
        }

        return STACK_LIMIT;
    }

    /**
     * ベッドはvanillaでは1個までですが、このModでは通常アイテムと同じ上限にします。
     */
    public static int getAdjustedStackLimit(Item item, int originalLimit) {
        if (item instanceof BedItem) {
            return STACK_LIMIT;
        }

        return getAdjustedStackLimit(originalLimit);
    }
}
