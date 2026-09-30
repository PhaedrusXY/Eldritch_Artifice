# Eldritch Artifice 0.9.7

## Changes
- Rite of the Open Eye: The required signs are available by Tier 2. Ingredients and chalk layout are unchanged from 0.9.6.
- Joining requires at least Tier 2; Tier 2 casters must have completed their current progression (the same getTierProgress check used by the native Council ritual).
- Successful joining advances a Tier 2 caster to Tier 3 using M&A setTier. Existing Tier 3+ unaffiliated characters keep their tier, supporting migration after /eldritch leave. Existing foreign allegiance is still rejected. Joining grants 1 Permanent Warp as before.
- The ritual rechecks eligibility at completion. It remains a joining ritual, not a repeatable Tier 4/5 advancement ritual.
- Time Warp and Teleport (Tier 4) now use signs available at their intended tier. Displacement unchanged.

## Validation
Inspected exact M&A 3.1.11 weave recipe tiers, Council joining ritual, AncientCouncil tier advancement, and PlayerProgression. Compiled the changed RitualEffectOpenEye class for Java 17 using ECJ against the exact M&A runtime JAR, the released addon classes, and the existing Minecraft/Forge compile stubs. Repackaged 0.9.6 with that class, three recipe changes, and version metadata. This is not a full Forge build or an in-game test. Verified recipe tier bounds, reagent cells, ZIP integrity, and that all other compiled classes are byte-identical to 0.9.6.

## In-game tests
Replace the old addon JAR with 0.9.7 on both client and server and restart. Restart any interrupted ritual with the new sequence; the physical chalk/ingredient layout is unchanged.
1. Tier 2, completed progression, no faction: perform the rite; expect Eldritch faction, Tier 3, and +1 Permanent Warp.
2. Tier 2, incomplete progression (or Tier 1): expect rejection.
3. Existing Tier 3 Council character: /eldritch leave, then perform the rite; expect Eldritch, still Tier 3, and +1 Permanent Warp.
4. Existing faction member: rite must reject existing allegiance, including repeat joining as Eldritch.
5. Tier 4 Eldritch: verify Time Warp and Teleport acquisition with their revised sequences.
