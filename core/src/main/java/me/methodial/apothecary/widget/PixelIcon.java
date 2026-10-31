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
 * A pixel-art icon, drawn in whole device pixels (no filtering), so it stays sharp in blocky designs such as
 * {@code OreUI}. Each icon is a grid of cells designed for a square frame of some number of cells (nine for the
 * kit's own set, eight for the Ore UI kit's); it is drawn at the largest whole cell size that fits the frame in the
 * box (default box = font-size), centered. Icons from the Ore UI kit cast its shadow, half-black down and right,
 * when drawn light. Color = {@code color}. Register more with {@link #register}. Type selector: {@code pixel-icon}.
 */
public class PixelIcon extends Node {
    public static final int GRID = 9;
    /** The Ore UI kit's icon frame: 8 cells, its icons 32 px at 4 px a cell. */
    public static final int KIT_FRAME = 8;

    /** An icon's cells, the frame they were designed in, and whether it casts the kit's shadow. */
    private record Art(String[] rows, int frame, boolean shadow) { }

    private static final Map<String, Art> ICONS = new LinkedHashMap<>();

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
        // The Ore UI kit's icons, cell for cell from its 32 px drawings; they replace the ones above.
        kit("chevron-left", "...#", "..#.", ".#..", "#...", ".#..", "..#.", "...#");
        kit("chevron-right", "#...", ".#..", "..#.", "...#", "..#.", ".#..", "#...");
        kit("chevron-up", "...#...", "..#.#..", ".#...#.", "#.....#");
        kit("chevron-down", "#.....#", ".#...#.", "..#.#..", "...#...");
        kit("close", "#.....#", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#.....#");
        kit("check", "......#", ".....#.", "#...#..", ".#.#...", "..#....");
        kit("edit", ".....##.", "....#..#", "...#.#.#", "..#...#.", ".#...#..", "#...#...", "##.#....", "###.....");
        kit("minus", "######");
        kit("plus", "..#..", "..#..", "#####", "..#..", "..#..");
        kit("search", ".####...", "#....#..", "#....#..", "#....#..", "#....#..", ".####.#.", ".......#");
        kit("category", "#########", "#..###..#", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", "#.......#", "#########");
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
        register(name, GRID, false, rows);
    }

    /** Registers an icon of any rectangular grid, designed for a square frame of {@code frame} cells. */
    public static void register(String name, int frame, boolean shadow, String... rows) {
        if (rows.length == 0 || frame < 1) throw new IllegalArgumentException("Pixel icon '" + name + "' is empty");
        for (String r : rows) {
            if (r.length() != rows[0].length() || !r.matches("[#.]+")) throw new IllegalArgumentException("Bad pixel icon row in '" + name + "': " + r);
        }
        ICONS.put(name, new Art(rows.clone(), frame, shadow));
    }

    /** An icon from the Ore UI kit: its 8-cell frame and its shadow. */
    private static void kit(String name, String... rows) { register(name, KIT_FRAME, true, rows); }

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
        Art art = ICONS.get(name);
        if (art == null || color.isTransparent()) return;
        String[] rows = art.rows();
        int cols = rows[0].length();
        Affine dev = c.deviceMatrix();
        float ps = dev.approxScale();
        int cell = Math.max(1, (int) Math.floor(Math.min(box.width(), box.height()) * ps / art.frame() + 1e-3f));
        int x0 = Math.round(dev.mapX(box.x(), box.y()) + (box.width() * ps - cell * cols) / 2f);
        int y0 = Math.round(dev.mapY(box.x(), box.y()) + (box.height() * ps - cell * rows.length) / 2f);
        // The kit's shadow, under light icons only: half-black, offset by most of a cell.
        boolean light = 0.2126f * color.r() + 0.7152f * color.g() + 0.0722f * color.b() > 0.5f;
        if (art.shadow() && light) {
            int off = Math.max(1, Math.round(cell * 0.7f));
            fill(c, dev, rows, x0 + off, y0 + off, cell, Color.rgba(0, 0, 0, 0.5f * color.a()));
        }
        fill(c, dev, rows, x0, y0, cell, color);
    }

    /** Fills the icon's cells from device position (x0, y0), {@code cell} device pixels each. */
    private static void fill(Canvas c, Affine dev, String[] rows, int x0, int y0, int cell, Color color) {
        int cols = rows[0].length();
        Affine inv = dev.invert();
        float[] rects = new float[cols * rows.length * 4];
        int[] colors = new int[cols * rows.length * 2];
        int n = 0;
        for (int y = 0; y < rows.length; y++) {
            // One rect per horizontal run of filled cells.
            for (int x = 0; x < cols; ) {
                if (rows[y].charAt(x) != '#') { x++; continue; }
                int end = x;
                while (end < cols && rows[y].charAt(end) == '#') end++;
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
