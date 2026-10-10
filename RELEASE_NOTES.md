# Eldritch Artifice 0.12.0 alpha

- Added a purple gradient to the reserve portion of the Eldritch mana bar, darkest near empty, and a small marker at the Warp threshold. The boundary uses the actual reserve capacity; M&A's spell-cost preview, bar placement, and numbers remain in place. Other factions' bars are unchanged.
- Integrated Unseen Eye 0.2.2 into Eldritch Artifice. The standalone test mod is no longer needed and must be removed from both client and server.
- Added the Tier 3 Eldritch-only Unseen Eye component and its Codex recipe/description: borrow the sight of a creature beyond the veil through a fragile, invisible flying eye.
- Preserved the tested M&A radial-menu controls, visible wheel options, hidden first-person hands, and stationary body pose. Z is the default M&A radial key; release it over Return to end the spell. Escape closes the wheel. Double-tap Jump dismissal is disabled.
- Magnitude above 1 allows wall phasing. Block/container/item interaction remains disabled. Self, Touch, and Projectile act on the caster; channeling is disallowed.
- No distance tether. Upkeep uses ordinary mana only. Same-dimension M&A portal travel is supported; cross-dimensional eye travel remains unsupported.
- Holding the Staff of the Open Way when casting adds Open portal & return: spend 150 ordinary mana to end scrying and leave a shared two-way portal open for 30 seconds.
- Alphabetized Caged Singularity, Displacement, Teleport, Time Warp, and Unseen Eye alongside M&A spell components in the Sorcery section. Existing entry IDs and the Unseen Eye spell/recipe IDs are preserved.
- Added the Gradle wrapper, a GitHub build workflow, and packaged-jar checks. Fixed existing obfuscated method names so the full source compiles with official mappings.
- Built against Forge 47.4.5 to retain compatibility with the pack's earlier Forge version. M&A integration targets 3.1.11.

**Upgrade:** stop active scrying; remove the old Eldritch Artifice and standalone Unseen Eye jars; install only the combined 0.12.0-alpha jar on both client and server. Back up the world first.

**Validation:** full Java 17/Gradle build and shipped-jar checks passed. The dedicated-server probe reached the EULA prompt only; full initialization, an upgraded world, and the integrated client still need the smoke tests in [VALIDATION.md](VALIDATION.md). The standalone eye's gameplay was previously tested by the pack author. This is a release candidate until the combined build passes those checks.

---

# Eldritch Artifice 0.11.16 alpha

- Removed written manaweaving shape sequences from historical release notes. The Codex has no remaining written shape instructions; visual recipe data is preserved.
- Caged Singularity now shows a pulsing dark sphere with a purple surface shimmer. Visuals update every eight ticks, with 32 particles per active field per update; field count and pull behavior remain bounded as before.
- Oculus advancement prompt: "Complete the Ritual of Gate and Key".

Validation: Java 17 compilation of changed classes against the prior JAR/M&A API; focused field lifecycle and particle-budget regression checks; JSON/resource and packaged-class checks. No live Minecraft visual inspection or dedicated-server boot was available.

Replace the old JAR on both client and server and restart. Check the sphere while casting from the Book of Rote, and the Oculus prompt below tier 5.
