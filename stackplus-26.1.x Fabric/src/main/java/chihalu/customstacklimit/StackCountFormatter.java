package chihalu.customstacklimit;

/**
 * GUIに表示するスタック個数を短く整形します。
 */
public final class StackCountFormatter {

    public static String format(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        }

        if (count % 1000 == 0) {
            return count / 1000 + "K";
        }

        return String.format(java.util.Locale.ROOT, "%.1fK", count / 1000.0);
    }

    private StackCountFormatter() {
    }
}
