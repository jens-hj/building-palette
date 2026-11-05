package dev.jensh.buildingpalette;

import dev.jensh.buildingpalette.core.*;
import java.util.Random;

public final class ModelTest {
    public static void main(String[] args) {
        int checks = 0;
        for (int units = 0; units <= 512; units++) for (Shape shape : Shape.values()) {
            Balance b = new Balance(units);
            assert Balance.fromSlots(b.slots(shape), b.missing(shape), shape).equals(b);
            assert b.pieces(shape) * shape.cost + b.remainder(shape) == units;
            assert b.spend(shape, b.pieces(shape)).units() == b.remainder(shape);
            checks += 3;
        }
        for (Shape shape : Shape.values()) if (shape.isMultipart()) for (int anchor = 0; anchor < 8; anchor++) for (int axis : new int[]{0, 2}) {
            assert Integer.bitCount(Cells.mask(shape, anchor, axis)) == shape.cost;
            checks++;
        }
        Cells quarters = new Cells();
        for (int a : new int[]{0, 1, 2, 3}) quarters = quarters.place(Cells.mask(Shape.STEP, a, 0), "oak");
        assert quarters.occupied() == 255;
        assert quarters.connected(0) == 255;
        assert "oak".equals(quarters.fullMaterial());
        Cells mixed = new Cells().place(1, "oak").place(2, "stone").place(8, "oak");
        assert mixed.connected(0) == 1; // diagonal does not connect
        Cells l = new Cells().place(7, "oak").place(128, "stone");
        assert l.connected(0) == 7;
        assert l.remove(l.connected(0)).occupied() == 128;
        Random random = new Random(20261105);
        for (int trial = 0; trial < 10000; trial++) {
            int units = random.nextInt(513);
            for (int iteration = 0; iteration < 20; iteration++) {
                Shape shape = Shape.values()[random.nextInt(Shape.values().length)];
                Balance b = new Balance(units);
                int used = random.nextInt(b.pieces(shape) + 1);
                assert b.spend(shape, used).units() + used * shape.cost == units;
                assert Balance.fromSlots(b.slots(shape), b.missing(shape), shape).units() == units;
                checks += 2;
            }
        }
        try { new Cells().place(1, "oak").place(1, "stone"); throw new AssertionError("Overlap accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
        System.out.println("Passed " + checks + " material and geometry assertions.");
    }
}
