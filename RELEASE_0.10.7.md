# Eldritch Artifice 0.10.7 — Grimoire and first faction artifact

## Grimoire of the Unseen (tier 4)
The Eldritch faction now returns its own registered grimoire, eldritchartifice:grimoire_eldritch, instead of the former Undead-book placeholder. It uses M&A's native Grimoire class and player-backed spell inventory, menu, spell selection and casting. Lost or destroyed covers can be summoned again to access the same stored spells. Multiple covers access the same collection, rather than making copies of its contents; a different player accesses their own collection. A replacement cover need not preserve its selected spell slot or cosmetic item data.

The tier split is unchanged:
- Tier 3: ordinary Summon Grimoire cantrip. This remains wholly controlled by M&A, and casting this tier-3 cantrip is eligible for its existing tier-3 cantrip task.
- Tier 4: Summon Faction Grimoire cantrip. For Eldritch members, this now returns the Grimoire of the Unseen.

Player-customized cantrip patterns still apply. No cantrip registrations, unlock tiers or effect callbacks are replaced. If the ordinary tier-3 summon itself still does nothing, this release does not independently fix that symptom; capture the matching client/server log to diagnose it.

The grimoire has an original static 3D item model: violet covers, pale pages, gold fittings and an amethyst slit-pupil eye. It uses vanilla material textures and ordinary item rendering, without a new client renderer or animation dependency.

## Lens of the Veil (tier 3)
This is the first craftable Eldritch faction artifact, eldritchartifice:lens_of_the_veil. Hold and use it to inspect your own Warp, Pressure and Exposure through in-world descriptions. Crouch and use for numerical readings of fleeting traces (Transient Warp), lingering stains (Lingering Warp), lasting scars (Permanent Warp), pressure and attention (Exposure).

Use requires Eldritch allegiance and tier 3 or higher. It is reusable, has a two-second use cooldown and costs no mana. It does not remove Warp, reduce attention, detect nearby entities or provide combat protection. Its model is a gold-rimmed dark lens with an amethyst eye and a short handle.

Craft at the Manaweaving Altar with one of each:
- Glass
- Eye of Ender
- Amethyst Shard
- Gold Ingot
- Stable Fabric (Dimensional Doors)

The recipe requires tier 3 and the Eldritch faction. Consult its visual recipe for the required weaves.

Recipe ID: eldritchartifice:manaweaving/lens_of_the_veil. Its requiredFaction is eldritchartifice:eldritch. M&A's altar compares the recipe faction with the crafter's faction and passes that match to its native manaweave_altar_craft trigger. This is the normal route for mna:tier_3/craft_faction_item; no advancement is manually granted and no base-game advancement is overwritten. Craft it yourself at the altar to test credit; merely obtaining it via a command does not demonstrate crafting.

## Codex and compatibility
Two new in-character Artifice entries describe the items. The Lens entry includes its native Manaweaving Altar recipe link. Existing Codex entries, rituals, boss, spells, Warp mechanics and Claude's startup guards are unchanged. The 0.10.6 entity-inspection compatibility fix remains included.

## Installation and in-game checks
Replace the previous addon JAR on both server and clients, keeping only one version, then restart. Targets remain Minecraft 1.20.1 / Forge 47.x / M&A 3.1.11 / Dimensional Doors 5.4.4 Forge.

1. At tier 3, cast the ordinary grimoire cantrip and check the existing tier-3 cantrip task in the Oculus.
2. As a tier-3 Eldritch mage, craft the Lens at the altar and check the faction-artifact task. Test ordinary use and crouch-use; repeated readings should not reduce or increase Warp.
3. At tier 4, summon the faction grimoire. Add identifiable test spells, close its menu, then lose/destroy the cover and summon another. Check that the same spells are available and cast correctly. Repeat after logout/server restart.
4. Have another player use a cover and confirm it opens their own collection. Verify no duplication of stored spell contents across covers.
5. Check item appearances in inventory and both hands, and the two Codex entries. Other factions should still summon their own native books.

## Validation performed
Five changed/new Java sources compiled with ECJ for Java 17 against M&A 3.1.11, addon 0.10.6 and signature-only compile stubs. An isolated Lens test checks clean and marked states, distinct Warp kinds, attention persisting when pressure subsides, and 1,000 non-mutating reads. Native M&A grimoire inventory, cantrip tiers, altar faction-match trigger and weave tiers were inspected in the target dependency.

Archive checks validate JSON, the Codex recipe link and recipe faction/tier/patterns; preserve all previous recipes and unrelated class bytes; and exclude compile stubs and test doubles from the playable JAR. This was a selective compilation, not a complete Forge Gradle build or live client/server test. The tests above still require in-game verification.
