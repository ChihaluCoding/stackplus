package chihalu.stackplus;

import java.nio.file.Path;

public final class StackLimitMathTest {
    public static void main(String[] args) {
        assertEquals(1_000, StackLimitMath.safeGiveMaxStackSize(1_000));
        assertEquals(Integer.MAX_VALUE / 100, StackLimitMath.safeGiveMaxStackSize(1_000_000_000));
        assertEquals(128, StackLimitMath.effectiveStackLimit(true, 64, 128, 100));
        assertEquals(150, StackLimitMath.effectiveStackLimit(true, 64, 128, 150));
        assertEquals(64, StackLimitMath.effectiveStackLimit(false, 64, 64, 150));
        assertEquals("999", StackCountFormatter.formatCompact(999));
        assertEquals("1K", StackCountFormatter.formatCompact(1_000));
        assertEquals("1M", StackCountFormatter.formatCompact(1_000_000));
        assertEquals("1B", StackCountFormatter.formatCompact(1_000_000_000));

        Path gameDirectory = Path.of("minecraft");
        assertEquals(gameDirectory.resolve("stackplus").resolve("stackplus-issue-report.txt"),
                StackPlusIssueReportCommand.getReportPath(gameDirectory));
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(Path expected, Path actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected=" + expected + ", actual=" + actual);
        }
    }

    private StackLimitMathTest() {
    }
}
