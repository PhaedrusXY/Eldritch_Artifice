# Eldritch Artifice 0.11.6 — Eternal Fluid in the Wayfarer Case

The tier 3 Eldritch Manaweaving Altar recipe for the Wayfarer's Doors now uses the Eternal Fluid Bucket from Dimensional Doors' Limbo (`dimdoors:eternal_fluid_bucket`). It joins the existing two dark oak doors, one ender pearl, four frayed filaments, and one stable fabric: nine items, the altar's maximum.

The crafting recipe now includes a guaranteed empty bucket byproduct. M&A clears the altar's inputs after crafting and puts recipe byproducts in its output area, so retrieve the bucket from the altar. Existing crafted cases, placed doors, and saved pairs keep working; the new ingredient applies to future crafting only. The Codex now describes Eternal Fluid binding the path to the waking world.

Replace 0.11.5 with 0.11.6 on both client and server for the new recipe to appear consistently. Recipe and Codex JSON parsed, the Dimensional Doors item ID was checked against the installed 5.4.4 JAR, and M&A 3.1.11's nine-item limit and `byproducts` parsing were checked against its actual classes. Version class compiled with Java 17 ECJ; archive comparison preserved all gameplay classes, including the startup crash guard and red doorway visual. No in-game crafting test has run here.
