package me.methodial.apothecary.node;

/**
 * Memoizes size computations for one node. Flex and grid measure children several times with the
 * same constraints; caching keeps layout linear-ish, and clean subtrees skip work across frames.
 */
final class LayoutCache {
    private static final int SIZE = 8;
    private final float[][] keys = new float[SIZE][];
    private final float[][] values = new float[SIZE][];
    private int next;
    /** Constraints of the last committed layout; equal + clean = nothing to redo. */
    float[] committedKey;

    float[] get(float[] key) {
        for (int i = 0; i < SIZE; i++) {
            if (keys[i] != null && same(keys[i], key)) return values[i];
        }
        return null;
    }

    void put(float[] key, float[] value) {
        keys[next] = key;
        values[next] = value;
        next = (next + 1) % SIZE;
    }

    void clear() {
        for (int i = 0; i < SIZE; i++) { keys[i] = null; values[i] = null; }
        committedKey = null;
    }

    static boolean same(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            float x = a[i], y = b[i];
            if (Float.isNaN(x) != Float.isNaN(y)) return false;
            if (!Float.isNaN(x) && Math.abs(x - y) > 0.001f) return false;
        }
        return true;
    }
}
