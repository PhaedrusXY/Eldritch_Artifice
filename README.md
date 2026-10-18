# Eldritch Artifice

An experimental Eldritch faction addon for **Minecraft 1.20.1**, **Forge 47.4.5 or newer in the 47.x series**, **Mana and Artifice 3.1.11**, and **Dimensional Doors 5.4.4**. Install the dependencies required by M&A and Dimensional Doors as well.

**Current source and jar version:** `0.12.2-alpha`. Renames the instability mechanic to Fray: contact with the beyond makes the caster less solid and less tethered to reality. Existing mechanics and save identifiers are preserved. Integrates the tested Unseen Eye 0.2.2 companion into the main mod and alphabetizes the five Eldritch spell components alongside M&A components in the Codex. The mana bar now shades the Fray-generating reserve purple, with a boundary marker. See [release notes](RELEASE_NOTES.md) and [validation and smoke tests](VALIDATION.md).

This remains an Alpha release while the planned sanctum work is incomplete. The repository includes editable Java sources, recipes, codex pages, textures, and the Gradle build definition. Dependencies are downloaded for building and are not included in the released jar.

## Administrator and testing commands

See the [complete command reference](COMMANDS.md) for all 40 `/eldritch` subcommands and the `/unseeneye` start, status, and stop commands, including arguments, permission requirements, and safety notes. **`/eldritch` commands are operator-only** and several alter character progression or world state. Test on backed-up worlds.

## Build

Install Java 17, then run:

```sh
./gradlew build --no-daemon
python3 scripts/check_release.py
```

On Windows, use `gradlew.bat build --no-daemon`. The wrapper uses Gradle 8.6. The packaging check requires Python 3.11 or newer. The installable, reobfuscated jar is `build/libs/eldritchartifice-0.12.2-alpha.jar`. GitHub Actions runs the build and packaging checks on pushes and pull requests.

The corrected 0.12.1 build has passed an isolated dedicated-server startup, world save, and clean shutdown. It supersedes the startup-broken 0.12.0 candidate; client gameplay and upgraded-world tests remain pending.

This version builds the complete source against the actual Forge/M&A/Dimensional Doors dependencies. Existing direct Minecraft calls have been converted from SRG names to official development mappings; Gradle restores runtime names when packaging. Reflection aliases used by the existing integration code remain intact.

## Upgrade and test

1. End any active Unseen Eye session (`/unseeneye stop`), stop the game/server, and back up the existing world.
2. Remove the previous Eldritch Artifice jar **and** any standalone `eldritch-unseen-eye-*-test.jar` from both client and server.
3. Install the same `eldritchartifice-0.12.2-alpha.jar` on both sides and restart. Do not install both the combined and standalone mods.
4. Follow the focused tests in [VALIDATION.md](VALIDATION.md) before updating the live servers or publishing the CurseForge file.

The integrated spell keeps `eldritcheye:unseen_eye`, its recipe ID, and the existing codex entry IDs to preserve crafted spells and guide progress. Forge may ask for confirmation when the standalone mod disappears from an existing world's mod list; test a backup first. No world reset is required by this change.

## Unseen Eye

A Tier 3, Eldritch-only possession spell grants the viewpoint of a fragile invisible flying eye. Self, Touch, and Projectile act on the caster. Hold the existing M&A radial key (Z by default), select an option, and release to act. Double-tap Jump does not dismiss the spell; Escape closes the wheel.

The eye cannot interact with blocks, containers, items, or enemies. Magnitude above 1 permits travel through walls. It has no distance tether, consumes 1 ordinary mana per second, and ends when its duration expires, mana runs out, or the body or eye is damaged. Cross-dimensional eye travel is not supported; same-dimension M&A portals are supported.

Holding the Staff of the Open Way in either hand when casting adds **Open portal & return**. It costs 150 ordinary mana, ends scrying, and leaves the staff's shared two-way portal open for 30 seconds.

## License

Original code and assets are offered under the [MIT License](LICENSE). Minecraft, Mana and Artifice, Dimensional Doors, and other dependencies retain their own rights and licenses.

`BUILD_PLAN.md` is an early project plan; current source and release notes describe the implemented version.
