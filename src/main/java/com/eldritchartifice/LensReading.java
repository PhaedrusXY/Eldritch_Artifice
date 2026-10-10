package com.eldritchartifice;

/** Read-only descriptions of existing Warp state; no new resource or cleansing. */
final class LensReading {
    private LensReading() {}

    static String describe(WarpState state, boolean detailed) {
        if (detailed) {
            return "Fray within your reflection: " + state.transientWarp + " fleeting frayed threads, "
                    + state.lingeringWarp + " lingering loose threads, and " + state.permanentWarp
                    + " permanently severed threads.\nPressure upon the veil: " + state.pressure
                    + ". Attention beyond it: " + state.exposure + ".";
        }
        String warp = state.totalWarp() == 0 ? "Your reflection is solid, firmly tethered to reality."
                : state.totalWarp() < 20 ? "Fine frayed threads trail from your reflection; your outline is less solid."
                : state.totalWarp() < 50 ? "Your reflection flickers, its tether to reality wearing thin."
                : "Your reflection is scarcely solid; something beyond reality watches through its unraveling threads.";
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
