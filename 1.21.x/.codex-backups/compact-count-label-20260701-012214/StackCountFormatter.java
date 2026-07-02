package chihalu.customstacklimit;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * GUIに表示するスタック個数を短く整形します。
 */
public final class StackCountFormatter {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance(Locale.US);
    private static final Unit[] UNITS = {
            new Unit(1_000_000_000, "B"),
            new Unit(1_000_000, "M"),
            new Unit(1_000, "K")
    };

    public static String format(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        }

        for (Unit unit : UNITS) {
            if (count >= unit.value()) {
                return formatUnit(count, unit);
            }
        }

        return String.valueOf(count);
    }

    public static String formatExact(int count) {
        return NUMBER_FORMAT.format(count);
    }

    private StackCountFormatter() {
    }

    private static String formatUnit(int count, Unit unit) {
        if (count % unit.value() == 0) {
            return count / unit.value() + unit.suffix();
        }

        return String.format(Locale.ROOT, "%.1f%s", count / (double) unit.value(), unit.suffix());
    }

    private record Unit(int value, String suffix) {
    }
}
