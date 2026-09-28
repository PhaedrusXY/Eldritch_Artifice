package com.eldritchartifice;

/**
 * Pure calculations for Eldritch reserve usage.
 */
final class OvercastMath {
    private OvercastMath() {
    }

    static float reserveDebt(float ordinaryCapacity, float amount) {
        if (ordinaryCapacity <= 0.0F) {
            return 0.0F;
        }
        return Math.max(0.0F, Math.min(ordinaryCapacity, ordinaryCapacity - amount));
    }

    static float reserveSpent(float ordinaryCapacity, float before, float after) {
        return Math.max(
                0.0F,
                reserveDebt(ordinaryCapacity, after) - reserveDebt(ordinaryCapacity, before));
    }

    static double warpForSpend(float ordinaryCapacity, float before, float after) {
        float spent = reserveSpent(ordinaryCapacity, before, after);
        if (spent <= 0.0F || ordinaryCapacity <= 0.0F) {
            return 0;
        }

        float depth = reserveDebt(ordinaryCapacity, after) / ordinaryCapacity;
        float tenPercentChunks = spent / Math.max(1.0F, ordinaryCapacity * 0.10F);
        double multiplier = 1.0D + 2.5D * depth * depth;
        return tenPercentChunks * multiplier;
    }
}
