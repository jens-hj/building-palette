# Changelog

## 0.1.0-alpha.5

- Draw the vertical selector as one continuous vanilla hotbar, retaining its thin internal dividers instead of repeating black slot borders.

## 0.1.0-alpha.4

- Replace the floating palette panel with a vertical, looping shape strip rising from the active hotbar slot.
- Use Minecraft's hotbar sprites and selection frame, including resource-pack overrides, with no title.
- Anchor the strip to the hovered inventory slot or offhand slot when those are being changed.

## 0.1.0-alpha.3

- Fix shared GUI atlas entries making the full block and vertical step look like extra corners in the shape palette.
- Give generated shape icons a distinct cache identity for their material and shape.
- Add a client regression checking that every palette shape has a distinct GUI model identity.

## 0.1.0-alpha.2

- Replace the vector-style icon with an original 16×16 pixel-art sprite inspired by the other projects.
- Include the new icon in the mod jar and on the Modrinth page; gameplay is unchanged.

## 0.1.0-alpha.1

- Add Alt-scroll building palette with conserved material balances and up to 512 corners per slot.
- Add half-block-grid multipart placement, mixed materials, explicit stairs, and connected-material mining.
- Add selected-shape ingredient matching, reserved fractional material, and proportional furnace fuel.
- Add native storage, cursor, drag, shift-transfer, hopper, and dropped-stack integration.
- Add model, Minecraft server, and graphical client tests.
