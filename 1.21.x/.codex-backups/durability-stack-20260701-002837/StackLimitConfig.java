package chihalu.customstacklimit;

import net.minecraft.item.BedItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

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
     * 対象外の非スタックアイテムは元の上限を維持します。
     */
    public static int getAdjustedStackLimit(int originalLimit) {
        if (originalLimit <= 1) {
            return originalLimit;
        }

        return STACK_LIMIT;
    }

    /**
     * ベッドと指定対象の非スタックアイテムは、このModでは通常アイテムと同じ上限にします。
     */
    public static int getAdjustedStackLimit(Item item, int originalLimit) {
        if (isForcedStackableItem(item)) {
            return STACK_LIMIT;
        }

        return getAdjustedStackLimit(originalLimit);
    }

    /**
     * ダメージ済みの対象耐久アイテムは、耐久状態を混ぜないため単体扱いにします。
     */
    public static int getAdjustedStackLimit(ItemStack stack, int originalLimit) {
        if (isDurabilityIsolatedItem(stack.getItem()) && stack.isDamaged()) {
            return 1;
        }

        return getAdjustedStackLimit(stack.getItem(), originalLimit);
    }

    /**
     * スタック使用時に1個だけ分離して耐久を減らす必要があるアイテムです。
     */
    public static boolean shouldSplitBeforeDurabilityLoss(ItemStack stack) {
        return stack.getCount() > 1 && isDurabilityIsolatedItem(stack.getItem());
    }

    private static boolean isForcedStackableItem(Item item) {
        return item instanceof BedItem || isRequestedSingleStackItem(item);
    }

    private static boolean isDurabilityIsolatedItem(Item item) {
        return getItemPath(item).equals("flint_and_steel");
    }

    private static boolean isRequestedSingleStackItem(Item item) {
        String path = getItemPath(item);

        return path.endsWith("_banner_pattern")
                || path.equals("enchanted_book")
                || path.equals("mushroom_stew")
                || path.equals("rabbit_stew")
                || path.equals("suspicious_stew")
                || path.equals("potion")
                || path.equals("splash_potion")
                || path.equals("lingering_potion")
                || path.equals("bucket")
                || path.endsWith("_bucket")
                || path.equals("totem_of_undying")
                || path.equals("flint_and_steel")
                || path.endsWith("_boat")
                || path.endsWith("_chest_boat")
                || path.equals("bamboo_raft")
                || path.equals("bamboo_chest_raft")
                || path.equals("goat_horn")
                || path.startsWith("music_disc_")
                || path.endsWith("_harness");
    }

    private static String getItemPath(Item item) {
        return Registries.ITEM.getId(item).getPath();
    }
}
