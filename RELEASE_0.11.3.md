# Eldritch Artifice 0.11.3 — The Eye in the Traveling Case

Minecraft 1.20.1 / Forge 47.x / Mana and Artifice 3.1.11 / Dimensional Doors 5.4.4.

## What changed
- The combined Wayfarer's Doors item is now a traveling briefcase with a violet eye and vertical slit pupil. Packed A/B endpoints still appear as individual doors; the placed doors have a matching eye on their upper half. Existing placed doors adopt the new look on client and server after updating.
- The Codex now includes: “You'll owlways have a way home.”
- The tier 3 Eldritch Manaweaving Altar recipe now yields the case. Ingredients: two dark oak doors, one ender pearl, four Dimensional Doors Frayed Filaments and one Stable Fabric. The installed Dimensional Doors ID is `dimdoors:frayed_filament` (there is no `frayed_fabric` item).
- Each player can activate and own one door pair. Another crafted but unused case cannot create a second pair or another live portal. Others can still travel through a placed pair; only its keeper may place or pack it. A pre-existing unowned pair is assigned to the first eligible player who uses or packs it. Its persisted identity remains unchanged.
- If a packed end is lost, crouch and use a *new unused case* to recover the same pair. This consumes the new case, reissues only its packed ends and invalidates older packed copies. It cannot replace a currently placed end. The same player can keep moving their original pair indefinitely.
- Previously combined 0.11.2 cases use the original item ID. They remain functional, but show the old door icon until split and recombined. Existing A/B endpoints and world saves remain compatible.

## Install and test
Replace 0.11.2 on both client and server. Back up the server world before changing mod versions. Have a tier 3 Eldritch mage craft a case at the Manaweaving Altar, unfold it and place both doors. Check the new textures, passage in both directions, packing into the briefcase, and reusing the same pair. A second raw case should refuse to create another pair. Test an existing pair by packing one end, then reuniting both. If a packed end is deliberately lost, crouch and use a spare raw case to restore that end without creating a new portal pair.

## Validation and limits
Production classes compiled for Java 17 with ECJ against M&A 3.1.11, prior release and signature stubs. Ledger tests passed for legacy ownership assignment, second-pair rejection, token recovery, cross-dimensional save/reload, duplicate token rejection, and file failure rollback. Exit geometry tests passed. All JSON parsed, all 32 blockstate variants resolve to local models, and each model resolves to a real vanilla parent. Dimensional Doors item IDs were checked against the actual 5.4.4 JAR. Selective archive comparison preserved unrelated classes including MnaSpellConfigRaceGuard and prior performance fixes. A full ForgeGradle or in-game integration test has not run here.

The tier 3 altar recipe can still produce additional unused cases; the server prevents activating more than one pair for the same keeper. The current ownership rule is tied to the player UUID and persists with the world. Existing portals with no recorded keeper remain in place until an eligible player uses or packs them. Resource packs that replace vanilla dark oak doors do not automatically recolor the custom eye-textured door.
