# Eldritch Artifice

An experimental Eldritch faction addon for **Minecraft 1.20.1**, **Forge 47.4.10**, **Mana and Artifice 3.1.11**, and **Dimensional Doors 5.4.4**.

**Current source and jar version:** `0.11.13-prototype`. The `beta0.10` label is a project milestone, not the jar's version number. The 0.11.13 update adds four tier 5 Eldrin Altar armor recipes and their Codex Arcana entries. See [release notes](RELEASE_NOTES.md).

The repository tracks the editable Java sources, recipes, codex pages, textures, and Gradle build definition. It does not bundle the copyrighted Minecraft, Forge, M&A, or Dimensional Doors jars. Forge and the two mods are required to play.

## Build

Install Java 17 and a compatible Gradle 8 release. From this repository run `gradle build`. Gradle retrieves Forge and the mod dependencies listed in `build.gradle`; the resulting jar is under `build/libs/`. The prototype also has a packaged jar available separately in ChatGPT's project files. The source package was assembled from the 0.11.13 prototype; a full clean Gradle build has not been verified here.

## Test

Install the same Eldritch Artifice jar on both server and client. Back up the world before testing a new version. Check the Eldrin Altar and the Codex Arcana's *Vestments of the Open Eye* entry. Use `/eldritch status` to inspect warp; the `/eldritch` test commands require operator permission.

## License

This project's original code and assets are offered under the [MIT License](LICENSE). Minecraft, Mana and Artifice, Dimensional Doors, and other dependencies retain their own rights and licenses.

`BUILD_PLAN.md` in this repository is an early project plan; the current source and release notes describe the implemented version.
