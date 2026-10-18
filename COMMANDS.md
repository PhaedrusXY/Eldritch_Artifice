# Eldritch Artifice — command reference

Verified against `EldritchCommands.java` and `EyeSessions.java` for **Minecraft 1.20.1 / Forge**. The 0.12.4-alpha Watcher hotfix has the same commands as 0.12.3-alpha. This is a **testing/operator reference**, not a list of ordinary survival features.

## Permissions and safety

- Every `/eldritch ...` command requires **permission level 2 (operator)**. Commands are bound to the **player executing them**; they accept no player-name, selector, or coordinates argument. Use them **in-game as an OP**, not from a bare server console.
- `/unseeneye start` requires **permission level 2**. `/unseeneye status` and `/unseeneye stop` are accessible without OP, but act only on the issuing player.
- **Back up worlds and use test characters** before progression-changing, permanent-Fray, arena, rift, or boss commands. Many actions cannot be undone simply by reversing the command.
- `shoggothjoin`, `shoggothleave`, `timewarpstatus`, and `timewarpclear` are registered but have the limited behavior noted below. This documentation reflects **actual implementation**, not planned features.

## Commands: `/eldritch ...`

Append any command in the tables to `/eldritch `. Example: `/eldritch status`.

### Diagnostics and read-only checks

| Command | Function |
| --- | --- |
| `/eldritch status` | Show current Fray (transient, lingering, permanent), pressure, exposure, and dimension. |
| `/eldritch faction` | Show the registered Eldritch M&A faction and the player's current M&A allegiance. |
| `/eldritch reserve` | Show current casting-resource amount and, when Eldritch, ordinary/reserve mana breakdown. |
| `/eldritch ddstatus` | Show whether the player is in Dimensional Doors Limbo or a pocket dimension. |
| `/eldritch armorstatus` | Show the player's Tier-5 Eldritch armor test state. |

### Fray, pressure, and consequences

| Command | Function |
| --- | --- |
| `/eldritch attention5` | Add **5 transient Fray** (and associated pressure); despite the historical 'Attention' command name, this modifies Fray. |
| `/eldritch attention25` | Add **25 transient Fray** and pressure. |
| `/eldritch lingering5` | Add 5 lingering Fray and pressure. |
| `/eldritch permanent1` | Add 1 permanent Fray and pressure; use only with a disposable/test character. |
| `/eldritch forceevent` | Force a Fray consequence immediately; may produce an in-world effect. |
| `/eldritch cleartransient` | Clear transient Fray and pressure; **preserves lingering and permanent Fray**. Also resets the next-event timer. |
| `/eldritch clear` | Reset the player's Fray/pressure/exposure state, **including persistent categories**; not an undo command. |

### M&A faction, advancement, and test equipment

| Command | Function |
| --- | --- |
| `/eldritch devsetup` | Attempt a full development setup: M&A Tier 5, magic level 75, progression complete, Eldritch allegiance, all rote/mastery, and Staff of the Open Way. **Changes progression permanently; test characters only.** |
| `/eldritch join` | Set the player's M&A allegiance to Eldritch. Leaving a different faction may accrue **betrayal ire** with that faction. |
| `/eldritch leave` | Clear the player's M&A faction allegiance; **does not automatically rejoin the former faction**. |
| `/eldritch overcasttest` | Force a 5-mana reserve dip for Eldritch overcasting tests; changes current mana. |
| `/eldritch armorset` | Give the complete Eldritch Tier-5 armor set (helmet, chestplate, leggings, boots). Does not equip it automatically. |
| `/eldritch staff` | Give one Staff of the Open Way. |

### Watcher testing

| Command | Function |
| --- | --- |
| `/eldritch watcher` | Summon a prototype Watcher for the issuing player (outside Limbo). |
| `/eldritch watcherstatus` | Show Watcher activation, attention/phase, remaining lifetime and entity information. |
| `/eldritch watcherfocus` | Set an active Watcher's attention near maximum; may quickly lead to its Limbo-collection behavior. |
| `/eldritch watcherclear` | Remove the player's active prototype Watcher. |

### Displacement testing

| Command | Function |
| --- | --- |
| `/eldritch displacement` | Apply Displacement with 1 charge lasting 15 seconds. |
| `/eldritch displacement3` | Apply Displacement with 3 charges lasting 25 seconds. |
| `/eldritch displacementstatus` | Show whether Displacement is active, charge count, remaining ticks, and range. |
| `/eldritch displacementconfig` | Print the M&A Displacement spell's configured duration min/default/max/step. |
| `/eldritch displacementclear` | Clear the player's debug Displacement effect. |

### Time Warp testing

| Command | Function |
| --- | --- |
| `/eldritch timewarp` | Trigger a debug Time Warp burst (30 seconds, radius 7, magnitude 2) at the player. |
| `/eldritch timewarpstatus` | Prints a generic 'mode=burst' statement; **does not show remaining time or a live instance**. |
| `/eldritch timewarpclear` | Registered, but currently **does not clear already-applied Time Warp status effects** (no persistent field to clear). |

### Rift encounter / Yog-Sothoth testing

| Command | Function |
| --- | --- |
| `/eldritch riftanchor` | Set the rift arena anchor at a nearby **iron door** (within roughly 6 blocks horizontally and 3 vertically). Persistent arena journal location is recorded. |
| `/eldritch riftopen` | Open the anchored Yog-Sothoth rift encounter when within 16 blocks of the intact anchor. **Changes nearby blocks and spawns a boss; test world recommended**. |
| `/eldritch shoggoth` | Alias for the same boss-spawn action as `riftopen`; it does not summon a different monster. |
| `/eldritch riftaudience` | Trigger a brief ceremonial eye/audience scene. **Not the actual combat boss**. |
| `/eldritch riftstatus` | Show rift encounter HP/phase and restoration status. |
| `/eldritch shoggothstatus` | Same status routine as `riftstatus`. |
| `/eldritch riftclear` | End the encounter, remove its boss and restore the tracked rift blocks. Back up before testing terrain changes. |
| `/eldritch shoggothclear` | Same cleanup routine as `riftclear`, with different confirmation text. |
| `/eldritch shoggothjoin` | Currently **informational only**; prints a message and does not enroll a player into a separate raid party. |
| `/eldritch shoggothleave` | Currently **no-op** for participation; the command advises moving more than 32 blocks away. |

## Unseen Eye commands

| Command | Permission | Function |
| --- | --- | --- |
| `/unseeneye start` | OP level 2 | Start Unseen Eye for **600 seconds**, without wall phasing. |
| `/unseeneye start <seconds>` | OP level 2 | Start with a duration from **30 to 3600 seconds**, without phasing. |
| `/unseeneye start <seconds> <augment>` | OP level 2 | Augment is **0, 1 or 2**. Only **2** turns on wall phasing; 0/1 do not. Example: `/unseeneye start 600 2`. |
| `/unseeneye status` | Any player | Display own active eye's time remaining, health and upkeep, or 'No Unseen Eye active'. |
| `/unseeneye stop` | Any player | End own active Unseen Eye session and return control to the body. |

Starting Unseen Eye still checks **Tier 3 and Eldritch allegiance**, a safe grounded spawn point and no conflicting possession/remote-view effect. It costs **20 ordinary mana** to open and **1 mana per second** afterward; casting isn't a bypass of normal resource requirements. Holding the Staff of the Open Way when starting enables the shared portal option through the M&A radial wheel.

## Related commands not supplied by Eldritch Artifice

Mana & Artifice itself supplies commands such as `mna progression tier 5`, `mna progression level 75`, `mna progression complete`, `mna rote all`, and `mna mastery all`. Eldritch Artifice's `/eldritch devsetup` **attempts to execute** these on the issuing test player. They are not `/eldritch` subcommands, and their availability depends on the installed M&A version.

Likewise, modpack-specific KubeJS commands such as `/tfworm` or the faction debugging scripts belong to **Stranger Aeons Reborn**, not the standalone Eldritch Artifice mod.

## Tested registration vs functionality

This document verifies what the source registers and the behavior its handlers implement. It does **not** certify that every command has passed full gameplay testing in a current client/server world. Admin commands can report an error and direct the operator to `latest.log`. Report the Minecraft/Forge, M&A, Dimensional Doors and Eldritch Artifice versions with any reproducible failure.

**Inventory:** 40 distinct `/eldritch` subcommands and 3 distinct `/unseeneye` actions (`start`, `status`, `stop`; with parameterized `start` variants).
