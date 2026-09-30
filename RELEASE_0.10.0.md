# Eldritch Artifice 0.10.0 — The Unbound Threshold

Built from the uploaded Claude 0.9.9b source and working JAR. Claude's MnaSpellConfigRaceGuard classes are copied unchanged into this release. The rejected local 0.9.9b boot patch was not used.

## Install and designate the site

Replace the previous Eldritch Artifice JAR on both clients and server; keep only one copy in each mods folder. This build performs real block removal and restoration: first test on a copied world.

Build the surrounding structure as planned. Put a normal vanilla **iron door** at the intended center and stand within six blocks. Run:

`/eldritch riftanchor`

The nearest iron door becomes the one designated encounter site for this world (across dimensions). The designation survives restarts. This release does not generate a bastion or make the dormant iron door function as a Dimensional Doors portal. Its surrounding architecture remains yours to build.

## Activate and fight

The new **Rite of the Unbound Threshold** appears under Rituals in the M&A Codex at Tier 4. It uses the Open Eye chalk/offerings layout. Perform it while standing within 16 blocks of the designated door. It summons the boss without changing faction allegiance or progression.

For quick operator testing, `/eldritch riftopen` bypasses the ritual. `/eldritch shoggoth` is an alias. Ordinary players need no operator command or enrollment to fight after ritual activation.

- The door vanishes and an animated particle rift occupies its position. An invisible-to-the-renderer vanilla living shell provides a target compatible with ordinary attacks and spell damage. There is no custom refraction shader or creature model in this build.
- Boss bar: **The Unmoored Rift**, 850 HP, 14 armor, no knockback.
- Weak continuous suction within 14 blocks becomes stronger after a two-second warning, lasts four seconds, and repeats on an 18-second cycle.
- Players and other living creatures are pulled, regardless of invisibility or faction. Creative/Spectator players are excluded. Nearby pets and passive animals are affected too.
- Within eight blocks: Slowness I and M&A Gravity Well.
- Within three blocks: 8 raw damage per second, rising to 16 during strong Infall. Normal armor/damage handling still applies.
- Players must be within 16 blocks to damage it. Tracked projectiles must also have been launched within 16 blocks. Unsupported modded projectile types fall back to the attacker's position at impact. Damage without a player source is rejected.
- If no living Survival/Adventure player remains within 32 blocks for ten seconds, the encounter closes and resets. One retreating player cannot reset a fight while another stays. Nearby animals do not keep it active.
- Victory closes the rift. Both victory and abandonment restore the door and journaled terrain. Another summon begins at full health. Unique drops, Marks and faction advancement rewards remain future work; no vanilla golem loot/XP is awarded.

## Blocks and recovery

The rift selects up to 240 ordinary stone/earth blocks within ten blocks of the doorway, between two blocks below and six above the anchor. Strong Infall consumes up to eight of these each cycle. Temporary block displays spiral into the center; they do not drop items or settle into new blocks.

The initial whitelist includes stone, cobblestone, mossy cobblestone, stone-brick variants, deepslate variants, granite, diorite, andesite, dirt, grass, netherrack, blackstone, polished blackstone bricks and obsidian. Containers, machines, liquids, gravity blocks and other unlisted materials are not selected. This is intentionally bounded arena destruction, not unlimited terrain erosion.

Before changes start, block states are saved in the world folder at `data/eldritchartifice-rift.properties`. Restoration flushes chunk saves before completing the journal. On restart, an unfinished journal is restored; the fight is not resumed. The same recovery runs when the boss unloads or the server shuts down.

Normal breaking/placing and explosion block damage are blocked inside the active arena's 12-block protected radius. This is not universal protection against every mod's direct world edits, automation or administrator commands. Avoid editing that area during a fight. If recovery finds a container/machine at a recorded block position, it stops and retains the journal instead of overwriting its inventory. Remove the conflicting block and retry `/eldritch riftclear`. Do not delete a pending journal.

## Commands

All commands below require operator permission:

- `/eldritch riftanchor` — designate the nearest iron doorway.
- `/eldritch riftopen` — activate it for testing.
- `/eldritch riftstatus` — inspect health, phase, remaining consumable blocks, or pending recovery.
- `/eldritch riftclear` — close the encounter and restore the arena; retry pending recovery.

Old shoggoth commands remain aliases/status helpers. `shoggothjoin` no longer enrolls anyone, and `shoggothleave` does not make a nearby player immune: leave the outer boundary to retreat.

## Suggested test

1. Build a small doorway surrounded by recognizable stone blocks on a copied world. Mark it, open it and confirm the door disappears and a boss bar appears.
2. Check suction outside and inside the three-block core. Confirm invisibility does not stop it.
3. Compare arrows/spells fired inside and outside 16 blocks.
4. Let some blocks be consumed, then have every player leave 32 blocks. After ten seconds, confirm the exact doorway and terrain return.
5. Repeat with two players; one stays inside while the other leaves. It should remain active.
6. Defeat it and confirm closure, restoration and no golem loot. Reopen it and confirm full health.
7. On a disposable world, test restart recovery after consumption; confirm no debris remnants or persistent boss bar.
8. Verify the actual Tier 4 ritual and Codex recipe, then test your normal Displacement and other faction spells.

## Validation performed

Java 17 compilation of changed/new classes against the exact M&A 3.1.11 runtime and established stubs; Minecraft 1.20.1 reflection-name audit; recipe/chalk/reagent consistency and archive checks. A standalone test exercises the actual arena code using Minecraft stand-ins and covers journaling before mutation, recovery from disk state, failed-save retention, container protection, exact restoration and repeat recovery.

This is a selectively compiled prototype, not a full ForgeGradle rebuild or a live Minecraft/modpack playtest. Target versions remain Minecraft 1.20.1, Forge 47.x, M&A 3.1.11 and Dimensional Doors 5.4.4 Forge. Supplied server crash reports used Forge 47.4.5. Please send `latest.log` plus any crash report if activation/recovery fails.
