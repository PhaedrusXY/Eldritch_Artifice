# Eldritch Artifice 0.11.0 — Wayfarer's Doors prototype

Minecraft 1.20.1 / Forge 47.x / Mana and Artifice 3.1.11 / Dimensional Doors 5.4.4.

## Use
- Craft Wayfarer's Doors at the Manaweaving Altar as a tier-3 Eldritch mage. Recipe: two dark oak doors, two ender pearls, one gold ingot, one stable fabric.
- For testing: `/give @s eldritchartifice:wayfarer_door`.
- Right-click the crafted item in the air to unfold a matched pair of items, A and B. Their short pairing labels match. Creation and placement require tier 3 and Eldritch membership.
- Place each on solid ground with two clear air spaces above. Open either door normally and walk into its doorway to travel to the other.
- Crouch-right-click either standing door with an empty hand to fold it into an item. Mining either half also packs the door. Its partner stays standing but cannot transport anyone until both ends are placed again.
- Either end can move independently, including to another dimension. There is no designated home end, travel fuel, or long cooldown. A short transit delay prevents immediate bouncing between doors.
- Other players can use the placed doors; this is not an ownership/locking system.

## Implementation and current scope
- Pairing is owned by this addon, not by the DD rift registry. Moving a door creates no DD detached rift or pocket dungeon. Existing teleport logic uses M&A's teleport helper and posts the Forge teleport cancellation event.
- Initial artwork uses vanilla dark oak door models and the door inventory icon. There is no folding animation, briefcase model, or DD portal-window effect yet.
- Travel currently supports unmounted server players. Mobs, projectiles, vehicles, and passengers are not transported.
- Both exit sides are checked for collision, footing, fluids, common damaging blocks and the world border. An unavailable, missing, or blocked partner refuses travel. This is not a guarantee against every modded environmental hazard.
- Placing posts Forge's multi-block placement hook. Packing runs at LOWEST event priority without receiving already-canceled events. Specific claim-mod behavior still needs in-game verification.
- Piston movement is blocked. Native block removal rotates the endpoint token and attempts to drop its paired item. The door has high blast resistance.
- Packed items contain the pair/side/token. The server rejects a second active copy of an endpoint; packing rotates its token so old copies cannot deploy afterward.
- Pair data is kept in the world's `data/eldritchartifice-wayfarer-doors.properties` using atomic replacement when supported. The file belongs with the world backup. It is written on pairing/placement/pickup, not every tick. A malformed ledger disables this feature instead of overwriting the file.
- Destination chunks are accessed for travel, not permanently forced. Player ticks perform a location lookup only while doors exist; there are no scans across living entities or every placed door.

## Validation performed
- Changed production classes compiled for Java 17 using ECJ, M&A 3.1.11, the 0.10.9 baseline and signature stubs. This is a selective patch build, not a full ForgeGradle build.
- Mojang 1.20.1 and Forge MCP mappings were checked for new remapped accessors. Forge 47.4.10 source was inspected for placement snapshots, cancellation, item-use capture, and the piston extension.
- Executable ledger tests passed: movement of either/both ends, cross-dimensional coordinates through reload, duplicate placed-token rejection, stale token invalidation, occupied destination rejection, invalid identities, placement rollback, failed-write rollback, and corrupt-version rejection.
- All resource JSON parses; all 32 door-facing/hinge/half/open model variants are supplied.
- Archive checks exclude test classes and compile stubs and preserve the existing startup race guard and 0.10.9 performance-fix classes.
- Not yet tested in a live Minecraft client/server. Actual rendering, doorway travel, Forge claim integrations and world restart behavior need the playtest below. The ledger tests simulate saved data reload, not a full server restart.

## First playtest
1. Replace the old addon JAR on both client and server. Start with a test copy of the world for this new block/transport prototype.
2. Give yourself the item and unfold it. Confirm both A and B appear, including with a nearly full inventory.
3. Place the pair roughly twenty paces apart. Open A and walk through; walk back through B after the short transit delay.
4. Pack A, confirm B refuses travel, move A and test both directions. Repeat with B. Restart and retest.
5. Test a pair spanning two dimensions, with the distant end initially unloaded. Block both exit sides and confirm travel is refused.
6. Test normal pickup from both halves, removing support, and your claim protection. Send any error log if a step fails.

No Gravity Well or Caged Singularity changes are included in this release.
