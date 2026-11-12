package dev.jensh.buildingpalette;

/** Implemented on ItemStack: sets the count without the remainder bookkeeping of setCount. */
public interface RawStack {
    void palette$rawCount(int count);
}
