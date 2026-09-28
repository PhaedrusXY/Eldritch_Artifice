package com.eldritchartifice;

/** Native M&A guide extension. This handler is installed on the client only. */
final class EldritchCodex {
    private EldritchCodex() {}

    static void register(Object event) {
        try {
            Object registry = event.getClass().getMethod("getRegistry").invoke(event);
            Class<?> location = Class.forName("net.minecraft.resources.ResourceLocation");
            Object path = location.getConstructor(String.class, String.class)
                    .newInstance(EldritchArtifice.MOD_ID, "guide");
            Class.forName("com.mna.api.guidebook.IGuideBookRegistry")
                    .getMethod("addGuidebookPath", location).invoke(registry, path);
            RuntimeLog.info("Registered Eldritch Codex pages.");
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Could not register Eldritch Codex pages", ex);
        }
    }
}
