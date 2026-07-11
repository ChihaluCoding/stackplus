package chihalu.stackplus;

public final class StackLimitMath {
    public static int safeGiveMaxStackSize(int maxStackSize) {
        return Math.min(maxStackSize, Integer.MAX_VALUE / 100);
    }

    // Preserve oversized stacks only while StackPlus rules are authoritative.
    public static int effectiveStackLimit(
            boolean stackRulesEnabled, int originalLimit, int adjustedLimit, int stackCount) {
        return stackRulesEnabled ? Math.max(adjustedLimit, stackCount) : originalLimit;
    }

    private StackLimitMath() {
    }
}
