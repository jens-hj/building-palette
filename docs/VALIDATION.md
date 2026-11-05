# Validation — 2026-11-06

## Automated checks

- `./gradlew build`: 414,333 model assertions and all 18 required Minecraft server GameTests passed in alpha.12.
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
- A sealed-room lighting regression compared native slab roofs against multipart bottom slabs and mixed-material top slabs. Exposed sky measured 15; inside the room, both skylight and overhead block light measured 0 for all roof cases. Existing saved multipart entities refresh their new occlusion property via a scheduled tick on chunk load.
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
