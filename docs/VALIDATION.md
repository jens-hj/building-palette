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
