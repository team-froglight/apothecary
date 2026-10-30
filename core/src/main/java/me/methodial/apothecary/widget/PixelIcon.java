package me.methodial.apothecary.widget;

import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Props;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A pixel-art icon on a nine-cell grid, drawn in whole device pixels (no filtering), so it stays sharp in
 * blocky designs such as {@code OreUI}. Color = {@code color}; the drawn size is the largest whole multiple of
 * the grid that fits the box (default box = font-size). Register more with {@link #register}.
 * Type selector: {@code pixel-icon}.
 */
public class PixelIcon extends Node {
    public static final int GRID = 9;

    private static final Map<String, String[]> ICONS = new LinkedHashMap<>();

    static {
        register("check", ".........", ".........", ".......#.", "......##.", ".#...##..", ".##.##...", "..###....", "...#.....", ".........");
        register("arrow-left", ".........", "...#.....", "..##.....", ".##......", "########.", ".##......", "..##.....", "...#.....", ".........");
        register("arrow-right", ".........", ".....#...", ".....##..", "......##.", ".########", "......##.", ".....##..", ".....#...", ".........");
        register("chevron-left", ".........", ".....#...", "....##...", "...##....", "..##.....", "...##....", "....##...", ".....#...", ".........");
        register("chevron-right", ".........", "...#.....", "...##....", "....##...", ".....##..", "....##...", "...##....", "...#.....", ".........");
        register("chevron-up", ".........", ".........", "....#....", "...###...", "..##.##..", ".##...##.", ".#.....#.", ".........", ".........");
        register("chevron-down", ".........", ".........", ".#.....#.", ".##...##.", "..##.##..", "...###...", "....#....", ".........", ".........");
        register("calendar", "..#...#..", ".#######.", ".##...##.", ".#######.", ".#.....#.", ".#.#.#.#.", ".#.....#.", ".#######.", ".........");
        register("close", ".........", ".##...##.", "..##.##..", "...###...", "....#....", "...###...", "..##.##..", ".##...##.", ".........");
        register("plus", ".........", "....#....", "....#....", "....#....", ".#######.", "....#....", "....#....", "....#....", ".........");
        register("minus", ".........", ".........", ".........", ".........", ".#######.", ".........", ".........", ".........", ".........");
        register("search", "..####...", ".##..##..", ".#....#..", ".#....#..", ".##..##..", "..####...", ".....##..", "......##.", ".......#.");
        register("coin", "..#####..", ".##...##.", "##..#..##", "#..###..#", "#..#.#..#", "#..###..#", "##..#..##", ".##...##.", "..#####..");
        register("grid", ".........", ".###.###.", ".#.#.#.#.", ".###.###.", ".........", ".###.###.", ".#.#.#.#.", ".###.###.", ".........");
        register("category", ".........", ".###.....", ".#######.", ".#.....#.", ".#.....#.", ".#.....#.", ".#.....#.", ".#######.", ".........");
        register("edit", "......##.", ".....###.", "....###..", "...###...", "..###....", ".###.....", ".##......", ".#####...", ".........");
        register("trash", "...###...", ".#######.", "..#...#..", "..#.#.#..", "..#.#.#..", "..#.#.#..", "..#...#..", "..#####..", ".........");
        register("lock", ".........", "...###...", "..#...#..", "..#...#..", ".#######.", ".###.###.", ".###.###.", ".#######.", ".........");
        register("star", "....#....", "....#....", "...###...", "#########", ".#######.", "..#####..", "..##.##..", ".##...##.", ".........");
        register("info", "...###...", "..#####..", ".###.###.", ".#######.", ".###.###.", ".###.###.", "..#####..", "...###...", ".........");
        register("warning", "....#....", "...###...", "...#.#...", "..##.##..", "..##.##..", ".#######.", ".###.###.", "#########", ".........");
        register("menu", ".........", ".#######.", ".........", ".........", ".#######.", ".........", ".........", ".#######.", ".........");
    }

    private String name;

    public PixelIcon(String name) {
        super("pixel-icon");
        this.name = name;
    }

    public String name() { return name; }

    public PixelIcon name(String n) {
        this.name = n;
        return this;
    }

    /** Registers a 9x9 icon from rows of {@code '#'} (filled) and {@code '.'} (empty). */
    public static void register(String name, String... rows) {
        if (rows.length != GRID) throw new IllegalArgumentException("Pixel icon '" + name + "' needs " + GRID + " rows");
        for (String r : rows) {
            if (r.length() != GRID || !r.matches("[#.]+")) throw new IllegalArgumentException("Bad pixel icon row in '" + name + "': " + r);
        }
        ICONS.put(name, rows.clone());
    }

    public static Set<String> names() { return Collections.unmodifiableSet(ICONS.keySet()); }

    public static boolean has(String name) { return ICONS.containsKey(name); }

    @Override public boolean isLeaf() { return true; }

    @Override
    protected float[] measureContent(float availWidth, float availHeight) {
        float s = get(Props.FONT_SIZE);
        return new float[]{s, s};
    }

    @Override
    protected void paintContent(Canvas c, Rect r) {
        paint(c, name, r, computed().color());
    }

    /** Paints icon {@code name} centered in {@code box} at the largest whole-pixel cell size that fits. */
    public static void paint(Canvas c, String name, Rect box, Color color) {
        String[] rows = ICONS.get(name);
        if (rows == null || color.isTransparent()) return;
        Affine dev = c.deviceMatrix();
        float ps = dev.approxScale();
        int cell = Math.max(1, (int) Math.floor(Math.min(box.width(), box.height()) * ps / GRID + 1e-3f));
        int size = cell * GRID;
        int x0 = Math.round(dev.mapX(box.x(), box.y()) + (box.width() * ps - size) / 2f);
        int y0 = Math.round(dev.mapY(box.x(), box.y()) + (box.height() * ps - size) / 2f);
        Affine inv = dev.invert();
        float[] rects = new float[GRID * GRID * 4];
        int[] colors = new int[GRID * GRID * 2];
        int n = 0;
        for (int y = 0; y < GRID; y++) {
            // One rect per horizontal run of filled cells.
            for (int x = 0; x < GRID; ) {
                if (rows[y].charAt(x) != '#') { x++; continue; }
                int end = x;
                while (end < GRID && rows[y].charAt(end) == '#') end++;
                float ax = x0 + x * cell, ay = y0 + y * cell, bx = x0 + end * cell, by = ay + cell;
                rects[n * 4] = inv.mapX(ax, ay);
                rects[n * 4 + 1] = inv.mapY(ax, ay);
                rects[n * 4 + 2] = inv.mapX(bx, by);
                rects[n * 4 + 3] = inv.mapY(bx, by);
                colors[n * 2] = colors[n * 2 + 1] = color.argb();
                n++;
                x = end;
            }
        }
        c.fillRects(rects, colors, n);
    }
}
