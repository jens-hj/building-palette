package dev.jensh.buildingpalette.core;

import java.util.*;

/** Pick the largest reusable piece that can exactly reconstruct the target cells. */
public final class ShapePicker {
    private static final List<Shape> ORDER = Arrays.stream(Shape.values()).filter(Shape::isMultipart)
        .sorted(Comparator.<Shape>comparingInt(shape -> shape.cost).reversed().thenComparingInt(Enum::ordinal)).toList();
    private ShapePicker() {}
    public static Shape closest(int mask) {
        if (mask == 0 || (mask & ~255) != 0) return Shape.CORNER;
        for (Shape shape : ORDER) if (canTile(mask, shape)) return shape;
        return Shape.CORNER;
    }
    public static boolean canTile(int mask, Shape shape) {
        if (!shape.isMultipart() || Integer.bitCount(mask) % shape.cost != 0) return false;
        var pieces = new HashSet<Integer>();
        for (int anchor = 0; anchor < 8; anchor++) for (int axis : new int[]{0, 2}) {
            int piece = Cells.mask(shape, anchor, axis);
            if ((piece & mask) == piece) pieces.add(piece);
        }
        return cover(mask, pieces);
    }
    private static boolean cover(int mask, Set<Integer> pieces) {
        if (mask == 0) return true;
        int first = Integer.lowestOneBit(mask);
        for (int piece : pieces) if ((piece & first) != 0 && (piece & mask) == piece && cover(mask ^ piece, pieces)) return true;
        return false;
    }
}
