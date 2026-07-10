package chihalu.stackplus;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * GUIに表示するスタック個数を短く整形します。
 */
public final class StackCountFormatter {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);

    public static String format(int count) {
        if (StackLimitConfig.getDisplayMode() == StackLimitConfig.DisplayMode.PLUS_99) {
            return count >= 100 ? "99+" : String.valueOf(count);
        }

        if (count < 1000) {
            return String.valueOf(count);
        }

        if (count >= 1_000_000_000) {
            return formatUnit(count, 1_000_000_000, "B");
        }
        if (count >= 1_000_000) {
            return formatUnit(count, 1_000_000, "M");
        }
        return formatUnit(count, 1_000, "K");
    }

    public static String formatExact(int count) {
        return NUMBER_FORMAT.format(count);
    }

    private StackCountFormatter() {
    }

    private static String formatUnit(int count, int unitValue, String suffix) {
        return String.format(Locale.ROOT, "%d%s", count / unitValue, suffix);
    }
}
