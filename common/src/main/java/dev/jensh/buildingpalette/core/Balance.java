package dev.jensh.buildingpalette.core;

/** A stack has one material and at most 64 blocks' worth, even when its icon changes. */
public record Balance(int units) {
    public static final int CAPACITY = 512;
    public Balance {
        if (units < 0 || units > CAPACITY) throw new IllegalArgumentException("Material units: " + units);
    }
    public int pieces(Shape shape) { return units / shape.cost; }
    public int remainder(Shape shape) { return units % shape.cost; }
    public int slots(Shape shape) { return (units + shape.cost - 1) / shape.cost; }
    public int missing(Shape shape) { return slots(shape) * shape.cost - units; }
    public Balance spend(Shape shape, int count) {
        if (count < 0 || count > pieces(shape)) throw new IllegalArgumentException("Insufficient material");
        return new Balance(units - shape.cost * count);
    }
    public static Balance fromSlots(int count, int missing, Shape shape) {
        if (count < 0 || missing < 0 || missing >= shape.cost || (count == 0 && missing != 0)) {
            throw new IllegalArgumentException("Invalid stack balance");
        }
        return new Balance(Math.subtractExact(Math.multiplyExact(count, shape.cost), missing));
    }
}
