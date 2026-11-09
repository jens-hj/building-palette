package dev.jensh.buildingpalette.core;

/** Costs are exact eighth-block units, independent of inventory representation. */
public enum Shape {
    BLOCK(8), SLAB(4), VERTICAL_SLAB(4), STAIR(6), STEP(2), VERTICAL_STEP(2), CORNER(1), FENCE(8), WALL(8);

    public final int cost;
    Shape(int cost) { this.cost = cost; }
    public boolean isMultipart() { return this != FENCE && this != WALL; }
}
