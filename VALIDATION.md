# 0.12.0-alpha validation

## Completed

- Compiled the complete source with Java 17, Gradle 8.6, ForgeGradle 6.0.54, official Minecraft 1.20.1 mappings, Forge 47.4.5, M&A 3.1.11, and Dimensional Doors 5.4.4. `build`, including `reobfJar`, succeeded. There are no Java unit tests in this repository; Gradle reports `test NO-SOURCE`.
- Ran `python3 scripts/check_release.py` against the shipped jar: valid archive/JSON; one mod declaration; Java 17 bytecode; preserved legacy spell/recipe IDs; all five sorcery entries; client-only mixin configuration; wheel visibility fix; no standalone bootstrap or removed interaction class.
- Compared the integration to the recovered 0.2.2-test source. Changes are limited to the module bootstrap, client event ownership, and moving the eye's guide entry into the main guide. Its gameplay/network code is preserved. A separate client-only mixin adds the newly requested reserve-mana visualization.
- Reviewed the direct Minecraft method-name conversion using Forge's SRG-to-official mappings. Reflection aliases are unchanged.
- Inspected M&A 3.1.11 HUD bytecode: the reserve mixin targets the single `getBarColor()` call before the spell-cost preview and the single `getMagicXP()` call before frame/text rendering. It uses the provider's geometry and actual reserve capacity; visual behavior still requires client testing.
- Launched an isolated Forge 47.4.5 dedicated-server installation with the combined jar, M&A, Dimensional Doors, Curios 5.14.1+1.20.1, GeckoLib 4.8.4, Architectury 9.2.14, and Cloth Config 11.1.136. It reached Minecraft's EULA prompt and exited. This confirms discovery only, **not** successful mod initialization or a world boot. M&A emitted warnings about its client-targeting mixins on the server before the prompt.

## Release gate: combined-build smoke test

Use a backup/test instance first. Install the same jar on client and server; remove both old jars. Keep the current pack dependencies. Do not change live worlds until these checks pass.

1. **Startup and upgrade:** complete a dedicated-server boot and join with the client. Check the log for registration failures, missing registry entries, or mixin exceptions. Confirm an existing Unseen Eye spell/book still resolves. Check old Eldritch items and spells remain available.
2. **Codex:** inspect Sorcery with sufficient progression. Verify Caged Singularity, Displacement, Teleport, Time Warp, and Unseen Eye appear alphabetically among M&A components. Open each entry and its recipe; confirm the eye has Tier 3 and the revised description.
3. **Casting restrictions:** cast the eye as Tier 3 Eldritch using Self, Touch, and Projectile. The caster should receive the eye each time. Confirm another faction or tier below 3 cannot cast it and channeling is rejected.
4. **Controls and appearance:** fly, ascend, descend, and look back at the body. The eye should be invisible, the first-person shield/hands hidden, and the body should not track the eye. Hold the M&A radial key (default Z): all options should be visible. Return should end scrying on release; Keep scrying and Escape should leave it active. Double-tapping Jump should not dismiss it.
5. **Magnitude:** magnitude 1 collides with walls; magnitude 2 passes through them. Neither version may open chests/doors, use blocks, pick up/toss items, attack, or cast another spell.
6. **Cleanup:** separately test damage to body/eye, duration expiry, low ordinary mana, disconnect/reconnect, and a normal Return. Check that the camera, movement, inventory, and mana behave normally afterward. Fly beyond the normal body view distance and return without a stuck camera or persistent eye.
7. **Shared portal:** hold the Staff of the Open Way in either hand when casting, move to a safe destination, and choose Open portal & return with at least 150 ordinary mana. Scrying should end immediately. Test another player traveling both ways during the 30-second lifetime. Without the staff at casting, the portal option should be absent. Insufficient mana should not create a portal. Test the eye itself through a same-dimension M&A portal. Cross-dimensional travel is not a supported feature.
8. **Mana reserve visualization:** with Eldritch allegiance, check full mana, slightly above half, exactly half, below half, and empty. The left/lower-value portion should be purple, darkening toward empty; the notch should stay at the reserve boundary. Crossing it by spending should agree with Warp generation. Check numeric text, held-spell cost preview, different GUI scales/HUD positions, and that another faction's mana bar is unchanged.
9. **Existing-content regression:** use the staff's ordinary anchor/portal action, a Wayfarer door, Lens of the Veil, and an existing Eldritch spell; equip the tier-5 armor. These exercise the pre-existing classes whose method names were corrected for the full build.

Operator shortcut: `/unseeneye start 600 1` for the base eye or `/unseeneye start 600 2` for phasing. Tier/faction and mana requirements still apply. `/unseeneye status` reports the active session; `/unseeneye stop` is the emergency exit. Command testing does not replace testing real spell delivery and saved spell compatibility.

## Publication and deployment

Candidate version: `0.12.0-alpha` (CurseForge **Alpha**, Minecraft **1.20.1**, **Forge**). Dependencies: Mana and Artifice and Dimensional Doors, plus their required dependencies. Use RELEASE_NOTES.md for the new version's changelog.

GitHub source, CurseForge publication, and live-server deployment are separate operations. Do not report them as complete based on a local build. GitHub source is being prepared on `release/0.12.0-unseen-eye` for review. CurseForge is signed out, and the live-server destinations/access are not available. CurseForge publication and live deployment remain gated on the combined-build smoke test and destination access.
