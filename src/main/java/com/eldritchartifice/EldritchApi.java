package com.eldritchartifice;

/**
 * Public server-side hooks for later M&A and Dimensional Doors integration.
 */
public final class EldritchApi {
    private EldritchApi() {
    }

    public static void drawAttention(Object serverPlayer, int amount, String source) {
        WarpService.addWarp(serverPlayer, WarpService.WarpKind.TRANSIENT, amount, source);
    }

    public static void addLingeringWarp(Object serverPlayer, int amount, String source) {
        WarpService.addWarp(serverPlayer, WarpService.WarpKind.LINGERING, amount, source);
    }

    public static void addPermanentWarp(Object serverPlayer, int amount, String source) {
        WarpService.addWarp(serverPlayer, WarpService.WarpKind.PERMANENT, amount, source);
    }
}
