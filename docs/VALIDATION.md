# Validation — 2026-11-06

## Automated checks

- `./gradlew build`: 414,333 model assertions and all 19 required Minecraft server GameTests passed in alpha.15.
- Actual server tests cover 512-unit shape round trips, persistent codecs/templates, reserved stair material, storage capacity, cursor/drag behavior, constrained slots, randomized unit transfers, dropped merging, repeated chest/barrel recipes, furnace fuel, native shape drops, eight-corner placement, connected-material mining/refunds, and multipart persistence.
- The isolated RPG graphical test passed with the copied client mod set, including Sodium, Iris, Lithium, Voxy, Controlify, Better End/Nether, Farmer's Delight, Macaw's blocks, and Wilder Wild. Crash Assistant was omitted from the test launch.
- RPG family validation passed for **238 supported families, including 133 modded families**. Every supported palette has unique shape choices and conserves a 512-unit balance.
- Held and inventory-hover Alt-scroll reached the integrated server and preserved material. A separate base-client regression also passed offhand Alt-scroll. Native 128-slab counts and generic 512-corner balances synchronized to the client. Multipart world models were nonempty and screenshot previews were captured.

The private-display runner requires fresh screenshots to count a graphical run as successful; a launcher exiting with code zero before running tests is not accepted.

## Harness-specific adjustments

The full copied RPG environment needs two test-only adjustments: suppress FrozenLib's early development log and keep fixture options instead of restoring Fabric's incomplete keybind snapshot. Xvfb supplies a separate OpenGL display; the development launch's desktop OpenGL initialization failed, and Voxy prevents Vulkan fallback. These changes are absent from the distributable mod and no original dependency jar was edited.

## Review artifacts

`build/release/` contains the mod, sources, SHA-256 checksums, and local Modrinth release metadata. `build/previews/rpg_*.png` contains the full-RPG previews. The fixture is under `build/rpg-instance/`; no live saves, account files, or launcher databases were copied.

## Limits

This is an alpha. Functional block entities and families without a full-block parent are excluded; fences and walls preserve native behavior. Multipart blocks cannot be pushed by pistons, and arbitrary ticking-block behavior, structure rotations, or bespoke third-party processing APIs are not guaranteed. Standard ingredient-based processing inherits native selected-shape matching and fractional guards; individual machine interfaces have not all been exhaustively exercised.

Keep the mod installed for worlds containing multipart blocks. There is no export/removal migration. No live RPG installation was performed. Subsequently, the alpha and source archives were uploaded to Modrinth and the project was submitted for public review; the source repository is public.

## GUI cache regression — alpha.3

The base-client regression first reproduced a shared GUI model identity between the full block and vertical step. Adding the material/shape selection to the model identity made the regression pass for every oak palette entry. The private-display client run also passed held, inventory, and offhand scrolling and captured fresh screenshots with distinct block, vertical-step, and corner icons. Alpha.3 and its sources were uploaded to Modrinth; both uploaded SHA-512 hashes match the local archives. The project gallery was refreshed.

## Hotbar selector — alpha.4

The isolated graphical client test passed after replacing the panel with the vertical hotbar strip. Fresh screenshots confirm the title is absent and five previews rise directly above the active slot. Held, inventory-hover, and offhand scrolling still preserve material on the server. Alpha.4 and its sources were uploaded and their SHA-512 hashes match the local archives. The Modrinth description and gallery were refreshed.

## Continuous slot dividers — alpha.5

The isolated graphical client test passed with the continuous, rotated vanilla hotbar background. The fresh preview shows thin internal dividers, with black borders only at the outside of the strip. Held, inventory-hover, and offhand scroll checks passed. Both uploaded archive SHA-512 hashes match the local release; the gallery was refreshed.

## Icon revision — alpha.6

The icon-only release built successfully. Both uploaded archives match local SHA-512 hashes, and the jar icon matches the new project icon bytes. No gameplay code changed. The image-generated icon replaces the old scripted sprite; the Modrinth AI disclosure remains concise and includes code, assets, and text.

## Coarse mixed-material icon — alpha.7

The icon-only release built successfully. Uploaded mod and source archive SHA-512 hashes match the local files; the packaged icon matches the new project asset. Gameplay code is unchanged.

## Icon edge refinement — alpha.8

The icon-only release built successfully. Both uploaded archive SHA-512 hashes match the local files, and the packaged icon matches the updated project asset. No gameplay code changed.

## Fabric Loader 0.19.5 compatibility — alpha.9

The build and runtime dependency minimum now target Loader 0.19.5. Under that loader, all 413,964 model assertions and all 11 Minecraft server GameTests passed. The isolated client GameTest also passed, including GUI model identity, held/inventory-hover/offhand Alt-scroll, material conservation on the integrated server, and screenshots. Both uploaded archive SHA-512 hashes match the local files; the packaged metadata requires Loader >=0.19.5. No live instance loader was changed.

## Multipart break particles — alpha.10

The client regression rejects break particles from the invisible carrier and requires normal oak and stone break effects to emit particles. The regression passed after disabling carrier terrain particles. The full build also passed all 413,964 model assertions and 11 server GameTests on Loader 0.19.5. The existing server mining effect still uses the removed material state. Uploaded mod and source archive SHA-512 hashes match local files.

## Placement, terrain lighting, and inventory controls — alpha.11

- Fifteen server GameTests passed on Fabric Loader 0.19.5, alongside 413,964 material/geometry assertions. New coverage includes native stair facing/halves, precise placement opt-in, native slab doubling consumption, reserved placement guards, all native stair corner drop values, miss/unsupported-hit rejection, and every oak-family source/destination shape pairing at four balances.
- A sealed-room lighting regression compared native slab roofs against multipart bottom slabs and mixed-material top slabs. Exposed sky measured 15; inside the room, both skylight and overhead block light measured 0 for all roof cases. An initial scheduled-tick hook attempted to refresh saved occlusion properties on chunk load; alpha.13 replaces it after actual saved-world tests showed that this hook ran too early.
- The graphical regression verifies terrain-model quad emission with AO, unique GUI model identity, centered fractional item bounds, held/offhand/inventory Alt-scroll, a full inventory palette cycle including corners, configuration defaults and per-player preference synchronization, rapid fractional cursor transfers, actual left/right mouse clicks, and cross-shape merging with a one-unit overflow.
- The complete server/client run passed using Sodium 0.9.2, Iris 1.11.7, and Photon v1.4 with Iris explicitly reporting an active shader pack. Fresh shader screenshots are stored as `build/previews/alpha11_photon_*.png`.

Photon's default spherical-harmonic skylight compute pass needs more shared memory than the private-display llvmpipe GPU provides (36 KB versus 32 KB). The shader fixture disables `SH_SKYLIGHT`, uses a 1024 shadow map and SSAO, and reduces other expensive options. Shadows, AO, and the terrain shader pipeline remain active. These are test-local shader settings; neither the user's shader files nor their live instance were changed.

The normalized inventory click buttons remain the vanilla 0/1. Physical input tests use SDL's left/right constants (1/3); the screen performs that normalization. Fractional stack equality is now reflexive, and cursor insertion publishes a copied final stack instead of mutating a shared prediction snapshot.

The final copied RPG client run also passed with shaders disabled, covering 238 supported material families (133 modded), the new terrain renderer, all inventory palette choices, both actual mouse buttons, cursor synchronization, cross-shape overflow merging, and live placement-setting synchronization.

## Material picking and partial mining — alpha.12

The base build and graphical run passed: 414,333 pure model assertions and 17 server GameTests. Geometry checks cover all 255 nonempty octant masks and the two requested compound-shape examples. Shift-click server tests cover chest-to-inventory, inventory-to-chest, partial destinations before empty slots, and source overflow when inventory is full. The screen test sends an explicit shifted mouse event because Fabric's generic mouse helper supplies zero modifier flags.

Client/server pick packets preserve the first available inventory shape and reshape the held material on the next pick, retaining 80 units across native stairs, diagonal steps, and rotated-stair vertical steps. Creative mining keeps the client block entity during prediction, leaves the other material visible/targetable, and removes the final block and collision on the next hit. A quartz/blackstone comparison scene places native touching vertical slabs beside a full mixed multipart cube under equal ceiling light; the combined cube is visibly lit in the standard renderer.

The exact-placement collision regression passes for both preference modes: an overlapping corner is rejected without spending material, a non-overlapping corner in the same occupied grid cell is allowed, every multipart shape (including full blocks) rejects player overlap in precise mode, and a bottom slab touching the player’s feet is allowed while a top slab intersecting their legs is rejected. Preview plans use the same collision check.

A real survival client with a Netherite pickaxe enchanted with Efficiency V and Haste II mines five mixed stone-slab/blackstone-step carriers at one-tick intervals. Each stone slab is instantly removed, all five remaining blackstone pieces stay visible/targetable, and a second pass removes every client/server block and collision. The regression caught stale server head rotation; mining and pick targeting now raycast using the latest received body rotation. The connected-mining server test also deliberately sets head rotation away from the new aim.

The copied RPG run additionally passed the equipped Traveler’s Backpack/Trinkets integration: the first pick moves the existing 65-unit slab stack from storage to the hotbar, preserves its shape, returns the displaced 64 dirt to storage, and a second pick reshapes that held balance to stairs.

The final Photon run passed all 18 server GameTests plus the complete graphical regression, including survival instant mining and real placement packets in both preference modes. Real-player checks reject native/precise full blocks and corners intersecting the player, allow a corner in the same grid cell without overlap, and verify client/server material balances. Fresh shader evidence is saved as `build/previews/alpha12_photon_*.png`; the reduced test preset described above remains in use.

The final copied RPG run passed with 238 supported families (133 modded), equipped-backpack/Trinkets two-stage picking, shift merging, survival Efficiency V/Haste II mining, and real player-collision placement packets in both placement modes. No live world, instance files, or shader settings were changed.

Alpha.12 and its sources were uploaded to Modrinth as version `c6iu74BW`; both remote SHA-512 hashes match the local archives. The six-image gallery and description were refreshed. The project remains in Modrinth’s processing queue with approval requested.

## Actual saved-world upgrades and terrain distance — alpha.13

A new test-only dedicated-server harness boots the exact SHA-512-verified published alpha.10, alpha.11, and alpha.12 jars to create real saved worlds. Each server stops cleanly; the harness copies its world directory, opens the copy with the alpha.13 release candidate, saves, and starts another JVM to reopen it. Original fixture saves retain identical file hashes. No live instance or world is opened.

Each source world contains all 255 occupied octant masks in single and mixed materials (510 carriers), 93 named shape/balance stacks in chests, 36 player inventory slots, offhand, 27 ender-chest slots, and three dropped stacks. Balances include 1, 65, and 512 units, covering incomplete native pieces and generic pieces. Checks preserve exact item identifiers, components, counts, material balances, mixed-material cells, collision volume, waterlogging, emission, and selected hotbar slot. The upgrade also mines one material from an old carrier, verifies the other remains and the refund is exact, adds a new corner, and verifies the edited block after restarting.

All three paths passed 2,535 upgrade checks and 2,530 reopen checks apiece. The machine-readable summary and tested jar hashes are in `UPGRADE-VALIDATION.json`. Reproduce with `nix develop --command python scripts/world-upgrade-test.py`; public release URLs and hashes are pinned in `scripts/world-upgrade-artifacts.json`. Test code stays outside the shipped mod jar.

The alpha.10→alpha.12 control run failed the old-block occlusion check, with Minecraft logging “Trying to schedule tick in not loaded position” during chunk loading. This exposes a gap in the earlier scheduled-tick migration described above. Alpha.13 queues loaded carriers until the end of the level tick instead. The saved slab-roof regression then measures zero skylight and overhead block light after upgrading. Already-correct loaded states skip notifications and neighbor updates; no continuous block-entity ticker is added.

The graphical distance regression places native blackstone slabs beside combined blackstone/quartz pieces 80 blocks from the camera, with entity distance at 50%. It checks that the far section is compiled and visible, multipart render shape is MODEL, the terrain model is active, and there is no registered block-entity renderer. The standard-renderer run passes; screenshot: `build/previews/alpha13_distance_indigo.png`. Render distance is 12 chunks and the narrow test FOV makes the distant comparison legible. This is a visibility regression, not an FPS benchmark.

The Sodium 0.9.2/Iris 1.11.7/Photon v1.4 run also passed the full graphical suite and the targeted 80-block compiled-section check, using the same reduced Photon test preset described above. Screenshot: `build/previews/alpha13_distance_photon.png`. The final build passed 414,333 model assertions and all 18 Minecraft server GameTests.

Alpha.13 is uploaded as Modrinth version `xTGL5N3x`. Its primary jar SHA-512 exactly matches the jar used for all three saved-world upgrades; both uploaded archive hashes match local files. The project remains in Modrinth’s processing queue.

## Step placement ownership — alpha.14

The server regression covers horizontal and vertical quarter steps in both placement modes, two horizontal axes, and single/mixed materials. It exercises actual CNM horizontal-step BlockItems, requires the first piece to create a Building Palette carrier, fills all four quarter positions, checks exact material assignments after each placement, and verifies a same-material assembly becomes a normal full block. Every assembly consumes exactly eight material units. Native stair facing/halves/corners retain the existing placement regression.

The base graphical run passed actual client/server use-item packets for both step directions, with precise placement off. It checks the first carrier, all four material masks, full-block conversion, and empty source stacks after consuming one block's worth of material. A further server regression seeds an old native step state directly, adds a different-material quarter through Building Palette, and checks the old material, new volume, and spent balance survive conversion.

The final copied RPG client run passed all step-placement packets, including four-piece mixed assemblies, alongside the existing controls and lighting checks. It validated 238 families (133 modded). The far terrain regression also passed through Continuity’s `EmissiveBlockStateModel` wrapper; it now checks actual emitted terrain geometry rather than assuming the outer model must be a `MultipartModel`. No production renderer change was needed. The build passed 414,333 pure assertions and all 19 server GameTests, including old native step conversion.

## Vanilla selection styling and optional hue — alpha.15

The base build passed 414,333 pure assertions, all 19 server GameTests, and the full graphical suite. A test-only LevelRenderer invoker captures actual vanilla render submissions through a recording collector. Multipart strokes match native strokes in colour, alpha, render type, line width, and translucency in both normal and high-contrast modes, while retaining only the aimed quarter-material shape. Opt-in 120°/240° hues change the primary multipart stroke, leave native blocks and the accessibility outer stroke alone, and 360° wraps to the same colour as 0°.

The settings test begins with the old two-field config file and requires highlighting to default off. It toggles highlighting, enables the hue slider, selects 270° through a slider click, reloads the saved file to verify both settings persist, then restores vanilla styling and requires the slider to disable. A live swatch previews the colour. No test-only invoker or recorder ships in the mod jar.

The final copied RPG run passed the complete suite, including the normal/high-contrast outline comparisons, hue-slider persistence, all quarter-step placement packets, and far terrain geometry. Iris/render-type wrappers are compared by pipeline location, blend/depth state, culling, polygon mode, and topology rather than Java object identity. The smaller Sodium/Iris fixture also passed. RPG family validation remained 238 supported families (133 modded).

Alpha.15 and sources are uploaded as Modrinth version `85oAfm1w`; both remote archive SHA-512 hashes match the local files. Test-only invokers and recorders are excluded from the primary jar.

## NeoForge port — alpha.17

- `./gradlew build :neoforge:runGameTestServer` built both loader jars, passed 414,333 shared model assertions, and passed the 19 server tests on each loader. The final NeoForge run passed all **20** tests; its additional resource test covers for all 9 shapes × 512 balances, including fractional-resource round trips and capacity.
- The Fabric graphical regression suite passed after the module split, including its existing synchronization, selection-outline, placement, rapid mining, and terrain-distance checks.
- The native NeoForge graphical suite passed with the vanilla renderer and with Sodium **0.9.3-beta.4**, plus Traveler’s Backpack **26.3-11.4.3**. It checks actual terrain model parts, AO flags, immutable model data, absence of a block-entity renderer, both shape selectors, fractional cursor movement, settings persistence, mixed quarter-step placement, partial mining leftovers, two-stage pick-block, and terrain visibility 80 blocks away with entity distance reduced.
- The backpack fixture checks a 65-unit fractional slab stack through real pick-block packets, preserved displaced hotbar items, a separate 512-corner stack, transaction rollback, and the second pick reshaping the held material. Resource description creation preserves the remainder; normalization is not an inventory split.
- Published Fabric alpha.10, alpha.11, alpha.12, and alpha.16 fixtures each seeded, upgraded to alpha.17, and reopened successfully. Each upgrade checked 2,535 conditions; each second reopen checked 2,530. Native NeoForge performed the same save/edit/reopen scenario. Fixtures cover 510 geometry/material combinations, containers, player inventories/ender chests, item drops, partial stacks, waterlogging, emitted light, roof occlusion, and editing saved combined blocks. Original fixture saves retained their hashes. See `UPGRADE-VALIDATION.json`.
- Binary inspection found no wrong-loader API references, bundled dependency implementations, or test classes in either release jar. Loader services and metadata are distinct; content IDs, component IDs, and saved-cell schema remain unchanged.

NeoForge shader validation could not run with the published renderer combination. Stable Sodium 0.9.2 fails on NeoForge 26.3.0.102 before Building Palette's scene loads. Sodium 0.9.3-beta.4 works but rejects published Iris 1.11.7, requiring Iris 1.11.8+. No bypasses were used. This is recorded as unverified NeoForge shader compatibility, not a passing shader result. Existing Fabric shader results remain applicable to the Fabric implementation.

All current port tests ran against disposable fixtures. The user's live RPG instance and worlds were not modified. The old-loader dependency error window during the first migration attempt was a harness defect; the harness now pins historical dependencies, disables Fabric GUI errors, and runs headlessly.

## Animated flat icon — alpha.18

The icon-only release built both loader jars and passed all 19 server GameTests. Both jars contain the still icon from `docs/assets/icon.png`, and the Modrinth project icon is the animated `docs/assets/icon.gif`. Fabric `l6j9WNeU` and NeoForge `b23fAxiy` uploaded with `client_and_server`; all four remote archive SHA-512 hashes match the local files. No gameplay code changed.

## Wider NeoForge range — alpha.19

The NeoForge minimum is now 26.3.0.76. Built against NeoForge 26.3.0.76 and 26.3.0.102; all 20 NeoForge server GameTests pass on both, and all 19 Fabric GameTests pass. Fabric `atAqmHLb` and NeoForge `JPumPd2h` uploaded with `client_and_server`; remote archive SHA-512 hashes match the local files.

## Native single pieces, pick refill — alpha.20

`./gradlew build` passed, including the pure model assertions. All 22 Fabric and 23 NeoForge server GameTests passed. New tests check that a placed upper step is stored as the native Clutter No More step and that Offgrid's `Blocklogging` accepts a fence running under it. They also check that adding another material converts it back to a combined block, and that mining a mixed block down to one slab stores that slab natively. Pick refill is covered for the same shape and for a larger target shape. The tests also check that inventory storage is used before the hotbar, that the stack fills to capacity, that stacks with enough material stay untouched, and that the selected slot is kept. A chest shift-click test moves native full blocks into partial corner, step, slab, and stair stacks for planks and stone bricks.

The Fabric and NeoForge graphical suites passed. The Fabric suite now includes a real shift-click from a chest screen into a partial corner stack. The Fabric graphical suite also passed against a fixture copied from the 311-mod Fabric pack, including Mouse Tweaks and Traveler's Backpack, with Fabric Loader 0.20.1 for that run only. All published saved-world upgrade paths (alpha.10, .11, .12, .16 to the new build) passed upgrade and reopen. Existing combined blocks are not rewritten on load.

Fabric `1XzuM1I7` and NeoForge `OnefzYdK` uploaded with `client_and_server`; all four remote archive SHA-512 hashes match the local files.

## Offgrid posts in steps — alpha.21

A non-required mixin skips Offgrid's run-along-the-step and notch checks for step hosts only. The method signatures match in Offgrid 1.0.4, 1.0.5 and 1.0.6 on both loaders. The server tests check a bare post and a crossing fence against a placed upper step: each fits, and its blocklogged outline does not overlap the step. They also check that stairs still reject a bare post. A real fence `useOn` against the step's underside creates one Offgrid overlay, keeps the step, and spends one fence. Without the mixin the bare-post assertion fails. All 22 Fabric and 23 NeoForge server tests passed. Both graphical suites and the 311-mod Fabric pack fixture also passed. Fabric `7ZZ7QbfH` and NeoForge `gYIZSfLE` uploaded with `client_and_server`; remote SHA-512 hashes match the local files.

## Fence-first steps and Clutter No More drops — alpha.22

New server tests place a fence first and then add a step by clicking the ceiling's underside, in both placement modes. The step becomes the host, the fence becomes one Offgrid overlay, and exactly one piece is spent. A second step that leaves room for the fence joins the host without removing it. A lone corner is refused and leaves both the fence and the material. Breaking a stone-brick step with a pickaxe returns 2 units, both with its fence inside and after the fence was removed. Clutter No More steps, vertical slabs and vertical steps of oak and stone bricks each drop their volume. Before the fix these drop tests returned 0, and fence-first placement failed. All 23 Fabric and 24 NeoForge server tests passed. Both graphical suites, the 311-mod Fabric pack fixture, and every published saved-world upgrade path also passed. Fabric `EB5a2NnA` and NeoForge `ODAefHvx` uploaded with `client_and_server`; remote SHA-512 hashes match the local files.

## Overlapping pieces, any-shape Offgrid hosts and breaking cracks — alpha.23

New server tests lay a step, then a perpendicular step over it: they merge into an L of three cells and spend one unit. The same step again, a slab covering the whole L, and an overlapping step of another material are refused. A fence keeps its place when a lone corner and then a corner of a second material are added. The resulting carrier is an Offgrid host. A fence is also blocklogged into a carrier missing one corner, and the carrier keeps its cells. A full block is refused and leaves the fence and the material. Offgrid accepts a lone post and a pane in a stair, and a wall in a bottom slab. Shift-clicking into the hotbar and into the main inventory, both from the player inventory and from a chest, merges into partial stacks of every shape. The crack hook was checked to apply on each loader: with the hook made mandatory, both graphical suites passed. All 24 Fabric and 25 NeoForge server tests passed, as did both graphical suites, the Fabric pack fixture, and every published saved-world upgrade path on both loaders.

## Abstract storage and OmniVault — alpha.24

`./gradlew build` passed with 26 Fabric server tests; NeoForge passed 26, and 27 with OmniVault 2.0.4 on the runtime (`-PneoOmniVault`). Both client suites passed.

A new server test takes a stack of 20 units (three blocks, the last incomplete) and treats it the way a storage mod does: a one-item description, rebuilt at other counts. The whole stack and two merged stacks come back exact; taking items one at a time, uneven splits, and splitting a merged pool never exceed the original units. `split` takes whole pieces and leaves the leftover behind, shrinking keeps it, and two half blocks merged by count stay two halves. Corners reshaped to a block equal ordinary planks.

The OmniVault test builds a real `NetworkStorage` over an in-memory cell. Five steps become one block and two corners with no step entry. A half block (**0+**) adds four corners and no block. A 20-unit partial stack rolls the corners over into blocks, 34 units in and 34 stored. Six more corners complete a block and remove the corner entry, and whole blocks pass through untouched. Simulated inserts store nothing.

Fabric dev and test runs now use Fabric Loader 0.20.1, which OmniVault requires; the mod's own minimum is unchanged.
