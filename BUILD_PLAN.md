# Eldritch Artifice — detailed build plan

## Design invariants

1. Warp exists on every player, not only Eldritch faction members.
2. Attention is not a separate meter: drawing Attention adds Warp and Warp Pressure.
3. Faction membership gates knowledge/control/benefits; Warp gates perception and consequences.
4. The Collector never needs provenance tracking. Eldritch content remains unsafe because the content/use hook itself draws Attention.
5. Dimensional Doors is a hard dependency and existing DD mechanics are reused rather than duplicated.
6. Silence is the default audio state. Azathoth audio is event/context-triggered, not an always-on music loop.

## Phase 0 — dependency and API proof

Goal: prove the development environment can load all three mods together.

- Forge 1.20.1 dev workspace.
- Compile against M&A 3.1.11.
- Compile against DD 5.4.4 Forge.
- Confirm M&A faction registry registration succeeds.
- Confirm DD classes such as `ModDimensions` and `MonolithEntity` are visible to the addon.

Exit criterion: dedicated server and client both reach title/server-ready without mixins.

## Phase 1 — Warp/Attention vertical slice

Goal: prove the persistent mechanic before content.

Data per player:
- transientWarp
- lingeringWarp
- permanentWarp
- pressure

Operations:
- add transient / lingering / permanent
- clear/set for debugging
- save/load NBT
- clone on death
- sync on login/respawn/dimension change and every mutation

Attention API:
- `drawAttention(player, amount, source)` -> transient Warp + pressure
- `useEldritchMagic(player, outsiderCost, intrinsicCost)`
- outsider cost applies only when M&A allied faction != Eldritch
- intrinsic cost applies to everyone

UI/content proof:
- tag-driven tooltip for `draws_attention`
- test focus item
- debug commands

Exit criterion: Warp survives restart and is correct on a dedicated server/client pair.

## Phase 2 — pressure and event scheduler

Goal: make Warp behave like a system rather than a number.

- Pressure decays separately from persistent Warp.
- Newly acquired Warp raises pressure nonlinearly by source/severity.
- Event checks occur at coarse intervals (not every tick).
- Event scheduler uses weighted bands by total Warp + pressure + context.
- Cooldowns prevent spam.
- First event types are harmless: whispers, particles, fake footsteps, short piping stings.
- Debug command can force a specific event.

Exit criterion: repeated attention causes bursts of activity, then quiet periods return naturally.

## Phase 3 — M&A ire bridge

Goal: make Eldritch foreign-faction use obey our rules without rewriting normal M&A ire.

Research first:
- decompile/search M&A 3.1.11 jar for `ire`, faction item/component checks, Collector safety logic, and player progression methods.
- prefer public event/API hook.
- otherwise use one narrow mixin at the point where Eldritch ire would be applied.

Desired behavior:
- Council/Fey/Demon/Undead remain untouched.
- Eldritch content says `Draws Attention`.
- Collector-acquired Eldritch content still draws Attention when used.
- routine Eldritch content can be free for members but dangerous for outsiders.
- selected dangerous content can have intrinsic Warp for everyone.

Exit criterion: same Eldritch component used by member vs outsider produces configured Warp behavior; normal M&A faction ire remains unchanged.

## Phase 4 — selective manifestations

Goal: prove multiplayer player-specific horror.

Prototype:
- spawn vanilla placeholder entity owned by a player UUID.
- server AI targets owner only.
- damage from unrelated players is canceled.
- manifestation cannot target unrelated players.
- client rendering is suppressed for non-owner clients.

Start with an Enderman-like Watcher; do not make custom models yet.

Exit criterion: two players stand together; only the Warped player sees/interacts with the manifestation.

## Phase 5 — Displacement

Spell version:
- apply a temporary charge-bearing state to target.
- intercept qualifying direct damage.
- find safe random destination.
- only cancel damage when teleport succeeds.
- consume charge.

Tier-5 armor version:
- self-only automatic proc.
- one dodge.
- approximately 6-10 block safe teleport.
- approximately 15 s cooldown.
- same shared teleport service as spell component.

Exit criterion: melee/projectile hit produces reliable Enderman-style evade without void/environmental immunity exploits.

## Phase 6 — spatial progression

- Enhanced same-dimension Recall with normal range cap removed.
- Cross-dimensional Recall later with Warp cost.
- Rift detection/manipulation using DD APIs.
- Pocket/Limbo travel hooks.

Exit criterion: no duplicate dimension/teleporter implementation exists in the addon.

## Phase 7 — Dimensional Doors / Azathoth

- Detect Limbo by DD dimension helper/key.
- Observe Monolith aggro/teleport transitions.
- Add Exposure separately from Warp.
- Make high Warp alter susceptibility, not necessarily raw Monolith mechanics at first.
- Add Azathoth client sky rendering only for qualifying players.
- Tie visibility to Warp/Exposure thresholds.

Audio event families:
- pipe stings
- choir/drone swells
- whisper/throng events
- notice/gaze events

Trigger examples:
- entering Limbo
- first perception threshold
- Monolith attention spike
- Monolith teleport
- significant Warp acquisition
- severe Warp event
- The Gaze

No persistent Azathoth music loop.

## Phase 8 — Time Warp and Tier-5 armor

Spell:
- temporary AoE temporal field.
- ally acceleration / hostile dilation.
- radius, duration, magnitude modifiers.

Armor:
- passive weak moving field.
- Spatial Displacement cooldown proc.
- optional later activated strong field.

Exit criterion: armor feels like space-time distortion rather than simple tank stats.

## Phase 9 — content and polish

Only after architecture is stable:
- custom Watcher/Elder Thing/Shoggoth models and animations
- final armor art
- faction advancement/tasks
- rituals and recipes
- Azathoth final renderer
- final CC0/CC-BY sound editing and attribution file
- configs and balancing
- save migration/versioning

## Testing discipline

Every major system gets:
- single-player test
- dedicated server test
- two-player visibility/interaction test where relevant
- logout/login persistence test
- death/respawn test
- dimension-change test
- failure-path test (teleport destination unavailable, dependency feature disabled, etc.)

Keep experimental hooks behind small service classes so a M&A/DD update changes one integration layer rather than the whole mod.
