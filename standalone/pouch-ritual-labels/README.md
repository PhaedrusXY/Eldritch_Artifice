# Mana and Artifice: Ritual Pouch Labels

Standalone **Forge 1.20.1** addon for naming saved Mana and Artifice rituals in the **Practitioner's Pouch** and **Mithion's Magnificent Mbag**.

## Installation
- Minecraft 1.20.1, Forge 47.4.5+ (47.x), Mana and Artifice 3.1.11.
- Install `pouch-ritual-labels-0.1.0-alpha.jar` in `mods/`.
- Install on **both client and dedicated server** for multiplayer.
- **Do not use alongside Eldritch Artifice 0.12.3-alpha or later** while Eldritch Artifice includes its own ritual-name feature. Both modify the same Mana and Artifice screen and item behavior.

## Usage
Hold a pouch, open the normal Mana and Artifice ritual selection wheel, hover a saved ritual, and press **Shift + right-click**. Enter a name and press Enter or Save. Reset or save an empty name to restore the original ritual name. Cancel or Escape leaves it unchanged.

Names display on the existing radial menu entry and selected pouch item name.

## Features
- Per-saved-ritual names; no separate slot-label bookkeeping.
- **Overwriting a ritual removes its custom name**, even when saving the same ritual again. Other names remain.
- Names persist as part of the pouch's NBT and carry with the item.
- Server validates the held pouch, saved entry identity and snapshot, edit session, and request.
- Names are bounded and literal, with unsafe control/formatting characters removed.
- No changes to spell effects, ritual placements, recipes, or Mana and Artifice's storage behavior.
- No dependency on Eldritch Artifice or Dimensional Doors.

For compatibility with already-named pouches, this implementation intentionally retains the `eldritchartifice:ritual_label` NBT key. This is merely a saved-item data key, not a dependency.

## Build
This directory is an **independent Gradle project**, initially staged inside the Eldritch Artifice repository before moving to its own repository. Build using the existing repository's Gradle wrapper:
```sh
./gradlew -p standalone/pouch-ritual-labels build --no-daemon
python3 standalone/pouch-ritual-labels/scripts/check_release.py
```
When moving into a separate repository, include a Gradle wrapper there and run `./gradlew build --no-daemon`.

Build output: `build/libs/pouch-ritual-labels-0.1.0-alpha.jar`.

## Testing
Version 0.1.0-alpha. The original Eldritch Artifice implementation was tested in-game for naming and name removal on ritual replacement. **This extracted standalone build has not yet been tested in-game.** Automated checks and the Forge build must pass before publishing. Test both pouch types, main/offhand use, persistence after reconnect, ordinary ritual casting, ritual replacement, and dedicated multiplayer.

## Source and license
MIT License. Extracted from [Eldritch Artifice](https://github.com/PhaedrusXY/Eldritch_Artifice) v0.12.3-alpha.
