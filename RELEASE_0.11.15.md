# Eldritch Artifice 0.11.15 prototype

Fixes item discovery and Codex recipe visibility.

- Lists all nine normal player items in the Tools & Utilities creative tab, which exposes them to creative search and JEI ingredient discovery. Internal door halves and staff anchor tokens are deliberately omitted.
- Adds individual Hood, Vestment, Wraps, and Steps of the Open Eye pages with Eldrin Altar recipe links. The armor overview and individual pages are in the Eldrin Altar category.
- Moves the staff recipe link out of the page sections into M&A's supported related_recipes field.
- Corrects the Caged Singularity page to use M&A's Sorcery category.
- Armor and staff crafting requirements remain tier 5 and Eldritch faction.

Validation: Java 17 compilation of the two changed/new classes; Forge 47.4.10 creative-event API inspection; all 19 guide pages checked for supported categories and section types; recipe links resolved against packaged resources. No live client or dedicated-server launch was available.

Install the matching jar on both client and server, replacing the old Eldritch Artifice jar. Restart the client so JEI rebuilds its ingredient list. At tier 5, search the Codex for Hood or browse Eldrin Altar. In JEI, search @eldritch or Hood and use R to open its recipe.
