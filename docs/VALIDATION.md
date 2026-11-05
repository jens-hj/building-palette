# Validation — 2026-11-05

## Automated checks

- `./gradlew build`: 413,964 model assertions and all 11 required Minecraft server GameTests passed.
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
