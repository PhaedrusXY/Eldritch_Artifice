# Eldritch Artifice 0.12.3-alpha - saved ritual names

Based on main's 0.12.2-alpha, including the Fray wording changes, with no spell or faction balance changes.

## Use

Hold a Practitioner's Pouch or Mithion's Magnificent Mbag and open its ordinary Mana and Artifice radial menu. Keep the radial-menu key held, hover an occupied slot, then hold Shift and right-click. Type a name and press Enter or Save. Reset (or saving a blank name) restores the original ritual name; Escape or Cancel leaves it unchanged. Saving closes the editor; reopen the normal radial menu to select a ritual.

Custom names appear on the existing radial-menu entry and in the pouch's selected-ritual item name. This is a general quality-of-life feature; no Eldritch faction membership is needed. Names are literal, bounded to 64 UTF-16 code units, and stored on the pouch, not on the player.

## Overwrite contract

The label is an optional `eldritchartifice:ritual_label` string INSIDE each `ritual_kit_data` entry. In the pinned Mana and Artifice 3.1.11 dependency, successful `storeRitual` constructs a fresh CompoundTag and replaces only the selected list entry, and assigns the normal item name. Consequently the overwritten entry's old label disappears, even for an identical ritual. Other saved entries and their labels remain untouched. Failed saves that never replace the entry do not erase its label.

Renaming changes only this new key and refreshes the selected display name; reagent data, destinations, rotation, patches and actual ritual selection are not edited. Old pouches without labels continue to work. No migration or modification of the Mana and Artifice jar is required.

## Multiplayer

Install the same updated Eldritch Artifice jar on client and server, replacing the old Eldritch Artifice jar. Do not install a second copy alongside it. The feature uses its own mandatory network channel. The server opens a bounded, two-minute edit session, validates the held pouch, ritual slot, saved-entry identity and snapshot, and then acknowledges saving. Delayed packets cannot rename a replaced ritual, including an equal replacement. Cancellation, logout and server stop discard sessions. The editor does not pause the world.

## Validation

The build includes regression tests for overwrite/reset, identical-ritual replacement, other-slot isolation, NBT persistence, independent copies, legacy data, malformed data, input bounds, expired/stale edits, and the actual pinned MnA bytecode's replacement operation. Release checks retain the existing legacy Unseen Eye resource and mod-identity checks, and check that client-only mixins remain client-only.

In-game acceptance checklist (not claimed completed by unit tests):
- Test both pouch types, main hand and offhand.
- Name two saved rituals and confirm the wheel tooltip, central text and selected item name.
- Select another slot and return; reconnect and confirm names persist.
- Overwrite one named slot with a different ritual, then with the same ritual: original ritual name returns, other labels remain.
- Attempt an invalid/failed ritual save: the existing label remains.
- Reset, save a blank name, and cancel an edit.
- Verify normal selection and actual ritual placement/reagents remain unchanged.
- Repeat on a dedicated multiplayer server.
