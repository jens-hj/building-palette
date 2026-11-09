package dev.jensh.buildingpalette.core;

import java.util.Arrays;
import java.util.Objects;

/** Immutable 2x2x2 occupancy. Indices are x | y<<1 | z<<2. Null denotes air. */
public final class Cells {
    private final String[] material;
    public Cells() { material = new String[8]; }
    public Cells(String[] cells) {
        if (cells.length != 8) throw new IllegalArgumentException("Expected eight cells");
        material = cells.clone();
    }
    public String at(int index) { return material[index]; }
    public String[] copy() { return material.clone(); }
    public int occupied() {
        int mask = 0;
        for (int i = 0; i < 8; i++) if (material[i] != null) mask |= 1 << i;
        return mask;
    }
    public Cells place(int mask, String value) {
        Objects.requireNonNull(value);
        if (mask == 0 || (mask & ~255) != 0 || (occupied() & mask) != 0) throw new IllegalArgumentException("Overlapping or empty piece");
        String[] result = copy();
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0) result[i] = value;
        return new Cells(result);
    }
    public int connected(int start) {
        if (material[start] == null) return 0;
        int result = 1 << start, previous;
        do {
            previous = result;
            for (int i = 0; i < 8; i++) if ((result & 1 << i) != 0) {
                for (int axis : new int[]{1, 2, 4}) {
                    int next = i ^ axis;
                    if (Objects.equals(material[start], material[next])) result |= 1 << next;
                }
            }
        } while (result != previous);
        return result;
    }
    public Cells remove(int mask) {
        String[] result = copy();
        for (int i = 0; i < 8; i++) if ((mask & 1 << i) != 0) result[i] = null;
        return new Cells(result);
    }
    public String fullMaterial() {
        if (occupied() != 255) return null;
        return Arrays.stream(material).allMatch(material[0]::equals) ? material[0] : null;
    }
    /** Generate a piece anchored at an octant. horizontalAxis is 0 (X) or 2 (Z). */
    public static int mask(Shape shape, int anchor, int horizontalAxis) {
        int mask = 0;
        for (int i = 0; i < 8; i++) {
            boolean y = (i & 2) == (anchor & 2);
            int h = horizontalAxis == 0 ? 1 : 4;
            boolean side = (i & h) == (anchor & h);
            boolean include = switch (shape) {
                case BLOCK -> true;
                case SLAB -> y;
                case VERTICAL_SLAB -> side;
                case STAIR -> y || side;
                case STEP -> y && side;
                case VERTICAL_STEP -> (i & 5) == (anchor & 5);
                case CORNER -> i == anchor;
                default -> false;
            };
            if (include) mask |= 1 << i;
        }
        return mask;
    }
}
