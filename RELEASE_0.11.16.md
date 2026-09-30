# Eldritch Artifice 0.11.16 alpha

- Removed written manaweaving shape sequences from historical release notes. The Codex has no remaining written shape instructions; visual recipe data is preserved.
- Caged Singularity now shows a pulsing dark sphere with a purple surface shimmer. Visuals update every eight ticks, with 32 particles per active field per update; field count and pull behavior remain bounded as before.
- Oculus advancement prompt: "Complete the Ritual of Gate and Key".

Validation: Java 17 compilation of changed classes against the prior JAR/M&A API; focused field lifecycle and particle-budget regression checks; JSON/resource and packaged-class checks. No live Minecraft visual inspection or dedicated-server boot was available.

Replace the old JAR on both client and server and restart. Check the sphere while casting from the Book of Rote, and the Oculus prompt below tier 5.
