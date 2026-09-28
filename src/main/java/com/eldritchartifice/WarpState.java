package com.eldritchartifice;

/**
 * Mutable server-authoritative player state for the prototype.
 */
final class WarpState {
    int transientWarp;
    int overcastRemainderMicro;
    int lingeringWarp;
    int permanentWarp;
    int pressure;
    int exposure;
    long localTick;
    long lastWarpTick;
    long nextEventTick;
    int perceptionStage;
    String dimensionId = "";

    int totalWarp() {
        return transientWarp + lingeringWarp + permanentWarp;
    }

    void clamp() {
        transientWarp = Math.max(0, transientWarp);
        overcastRemainderMicro = Math.max(0, Math.min(999_999, overcastRemainderMicro));
        lingeringWarp = Math.max(0, lingeringWarp);
        permanentWarp = Math.max(0, permanentWarp);
        pressure = Math.max(0, Math.min(10_000, pressure));
        exposure = Math.max(0, Math.min(10_000, exposure));
        perceptionStage = Math.max(0, Math.min(4, perceptionStage));
    }

    void clear() {
        transientWarp = 0;
        overcastRemainderMicro = 0;
        lingeringWarp = 0;
        permanentWarp = 0;
        pressure = 0;
        exposure = 0;
        lastWarpTick = localTick;
        nextEventTick = localTick;
        perceptionStage = 0;
    }
}
