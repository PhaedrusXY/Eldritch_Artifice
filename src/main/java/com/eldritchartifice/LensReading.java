package com.eldritchartifice;

/** Read-only descriptions of existing Warp state; no new resource or cleansing. */
final class LensReading {
    private LensReading() {}

    static String describe(WarpState state, boolean detailed) {
        if (detailed) {
            return "Within your reflection: " + state.transientWarp + " fleeting traces, "
                    + state.lingeringWarp + " lingering stains, and " + state.permanentWarp
                    + " lasting scars.\nPressure upon the veil: " + state.pressure
                    + ". Attention beyond it: " + state.exposure + ".";
        }
        String warp = state.totalWarp() == 0 ? "Your reflection is unmarked."
                : state.totalWarp() < 20 ? "Fine distortions cling to your reflection."
                : state.totalWarp() < 50 ? "Your reflection no longer follows you faithfully."
                : "Something unfamiliar looks back through your reflection.";
        String pressure = state.pressure == 0 ? "The veil lies still."
                : state.pressure < 50 ? "A faint strain troubles the veil."
                : state.pressure < 120 ? "The veil trembles under gathering strain."
                : "The veil strains as though it might tear.";
        String attention = state.exposure == 0 ? "No answering gaze stirs within the lens."
                : state.exposure < 20 ? "A distant gaze brushes past you."
                : state.exposure < 60 ? "Something beyond the veil searches for you."
                : "An intent gaze holds the shape of you.";
        return warp + " " + pressure + " " + attention;
    }
}
