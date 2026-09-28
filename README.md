# Eldritch Artifice

An experimental Eldritch faction addon for **Minecraft 1.20.1**, **Forge 47.4.10**, **Mana and Artifice 3.1.11**, and **Dimensional Doors 5.4.4**.

**Current source and jar version:** `0.11.14-prototype`. Adds a tier 5 Staff of the Open Way recipe, an Eldritch Mark, and a Caged Singularity spell thesis dropped by Yog-Sothoth. See [release notes](RELEASE_NOTES.md).

The repository tracks the editable Java sources, recipes, codex pages, textures, and Gradle build definition. It does not bundle the copyrighted Minecraft, Forge, M&A, or Dimensional Doors jars. Forge and the two mods are required to play.

## Build

Install Java 17 and a compatible Gradle 8 release. From this repository run `gradle build`. Gradle retrieves Forge and the mod dependencies listed in `build.gradle`; the resulting jar is under `build/libs/`. A packaged jar is available separately in ChatGPT's project files. The 0.11.14 patch was compiled against the prior jar and M&A API with local development stubs; a full clean Gradle build and dedicated-server boot have not been verified here.

## Test

Install the same Eldritch Artifice jar on both server and client. Back up the world before testing a new version. Check the Staff of the Open Way altar recipe in the Codex Arcana. At the anchored bastion, defeat the aspect for Marks and a Caged Singularity spell thesis; check that it is usable from the Book of Rote. The `/eldritch` test commands require operator permission.

## License

This project's original code and assets are offered under the [MIT License](LICENSE). Minecraft, Mana and Artifice, Dimensional Doors, and other dependencies retain their own rights and licenses.

`BUILD_PLAN.md` is an early project plan; the current source and release notes describe the implemented version.
