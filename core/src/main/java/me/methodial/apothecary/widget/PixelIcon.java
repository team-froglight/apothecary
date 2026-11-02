package me.methodial.apothecary.widget;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.ViewBox;
import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Props;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A pixel-art icon from an SVG drawn on a grid of cells, rendered with JSVG in whole device pixels (no filtering), so
 * it stays sharp in blocky designs such as {@code OreUI}. The SVG's view box is its grid; it is designed for a square
 * frame of some number of cells (eight for the built-in set, the Ore UI kit's) and drawn at the largest whole cell size
 * that fits the frame in the box (default box = font-size), centered. Icons in the kit's style cast its shadow,
 * half-black down and right, when drawn light. The SVG's shapes give the icon's form; its color is
 * {@code color}. Register more with {@link #register}. Type selector: {@code pixel-icon}.
 */
public class PixelIcon extends Node {
    /** The Ore UI kit's icon frame: 8 cells, its icons 32 px at 4 px a cell. */
    public static final int KIT_FRAME = 8;

    /** An icon's drawing, its grid in cells, the frame it was designed in, and whether it casts the kit's shadow. */
    private record Art(SVGDocument svg, int cols, int rows, int frame, boolean shadow) { }

    private static final Map<String, Art> ICONS = new LinkedHashMap<>();
    /** Each icon's filled device pixels at a cell size, as runs: x, y, width, height (rows merged where they repeat). */
    private static final Map<String, int[]> RASTERS = new ConcurrentHashMap<>();
    private static final SVGLoader LOADER = new SVGLoader();

    static {
        // One set in the Ore UI kit's style: its own icons cell for cell from its 32 px drawings, and the rest drawn to
        // match them (one-cell strokes, at most 7x7 in its 8-cell frame, its shadow).
        for (String name : List.of("check", "arrow-left", "arrow-right", "chevron-left", "chevron-right", "chevron-up",
                "chevron-down", "calendar", "close", "plus", "minus", "search", "coin", "grid", "category", "edit", "trash",
                "lock", "star", "info", "warning", "menu")) {
            register(name, KIT_FRAME, true, PixelIcon.class.getResource("/assets/apothecary/icons/kit/" + name + ".svg"));
        }
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

    /**
     * Registers an icon from an SVG file: its view box is the icon's grid of cells, shapes on whole cells; it is
     * designed for a square frame of {@code frame} cells and, with {@code shadow}, casts the Ore UI kit's shadow.
     */
    public static void register(String name, int frame, boolean shadow, URL svg) {
        if (svg == null) throw new IllegalArgumentException("Pixel icon '" + name + "' has no SVG");
        add(name, frame, shadow, LOADER.load(svg));
    }

    /** As {@link #register(String, int, boolean, URL)}, from the SVG's text. */
    public static void register(String name, int frame, boolean shadow, String svg) {
        add(name, frame, shadow, LOADER.load(new java.io.ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)), null,
            com.github.weisj.jsvg.parser.LoaderContext.createDefault()));
    }

    private static void add(String name, int frame, boolean shadow, SVGDocument svg) {
        if (svg == null || frame < 1) throw new IllegalArgumentException("Pixel icon '" + name + "' could not be read");
        ViewBox box = svg.viewBox();
        int cols = Math.round(box.width), rows = Math.round(box.height);
        if (cols < 1 || rows < 1) throw new IllegalArgumentException("Pixel icon '" + name + "' is empty");
        ICONS.put(name, new Art(svg, cols, rows, frame, shadow));
        RASTERS.keySet().removeIf(key -> key.startsWith(name + "@"));
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
        Art art = ICONS.get(name);
        if (art == null || color.isTransparent()) return;
        Affine dev = c.deviceMatrix();
        float ps = dev.approxScale();
        int cell = Math.max(1, (int) Math.floor(Math.min(box.width(), box.height()) * ps / art.frame() + 1e-3f));
        int[] runs = RASTERS.computeIfAbsent(name + "@" + cell, key -> raster(art, cell));
        int x0 = Math.round(dev.mapX(box.x(), box.y()) + (box.width() * ps - cell * art.cols()) / 2f);
        int y0 = Math.round(dev.mapY(box.x(), box.y()) + (box.height() * ps - cell * art.rows()) / 2f);
        // The kit's shadow, under solid light icons only (on faint ones it reads as a smudge): half-black, offset
        // by most of a cell.
        boolean light = 0.2126f * color.r() + 0.7152f * color.g() + 0.0722f * color.b() > 0.5f;
        if (art.shadow() && light && color.a() > 0.9f) {
            int off = Math.max(1, Math.round(cell * 0.7f));
            fill(c, dev, runs, x0 + off, y0 + off, Color.rgba(0, 0, 0, 0.5f * color.a()));
        }
        fill(c, dev, runs, x0, y0, color);
    }

    /**
     * Renders the icon's SVG at {@code cell} device pixels a cell, without antialiasing, and keeps its filled pixels
     * as rectangles: one per run of pixels in a row, grown down over the rows that repeat it.
     */
    private static int[] raster(Art art, int cell) {
        int w = art.cols() * cell, h = art.rows() * cell;
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            art.svg().render((java.awt.Component) null, g, new ViewBox(0, 0, w, h));
        } finally {
            g.dispose();
        }
        List<int[]> rects = new ArrayList<>();
        int[] previous = new int[0];
        List<int[]> open = new ArrayList<>();
        for (int y = 0; y < h; y++) {
            int[] spans = spans(image, y, w);
            if (Arrays.equals(spans, previous)) {
                for (int[] rect : open) rect[3]++;
                continue;
            }
            open.clear();
            for (int i = 0; i < spans.length; i += 2) {
                int[] rect = {spans[i], y, spans[i + 1] - spans[i], 1};
                rects.add(rect);
                open.add(rect);
            }
            previous = spans;
        }
        int[] runs = new int[rects.size() * 4];
        for (int i = 0; i < rects.size(); i++) System.arraycopy(rects.get(i), 0, runs, i * 4, 4);
        return runs;
    }

    /** The start and end of each run of mostly opaque pixels in row {@code y}. */
    private static int[] spans(BufferedImage image, int y, int w) {
        int[] spans = new int[w + 1];
        int n = 0;
        for (int x = 0; x < w; ) {
            if ((image.getRGB(x, y) >>> 24) < 128) { x++; continue; }
            int end = x;
            while (end < w && (image.getRGB(end, y) >>> 24) >= 128) end++;
            spans[n++] = x;
            spans[n++] = end;
            x = end;
        }
        return Arrays.copyOf(spans, n);
    }

    /** Fills the icon's pixel rectangles from device position (x0, y0). */
    private static void fill(Canvas c, Affine dev, int[] runs, int x0, int y0, Color color) {
        Affine inv = dev.invert();
        int n = runs.length / 4;
        float[] rects = new float[n * 4];
        int[] colors = new int[n * 2];
        for (int i = 0; i < n; i++) {
            float ax = x0 + runs[i * 4], ay = y0 + runs[i * 4 + 1];
            float bx = ax + runs[i * 4 + 2], by = ay + runs[i * 4 + 3];
            rects[i * 4] = inv.mapX(ax, ay);
            rects[i * 4 + 1] = inv.mapY(ax, ay);
            rects[i * 4 + 2] = inv.mapX(bx, by);
            rects[i * 4 + 3] = inv.mapY(bx, by);
            colors[i * 2] = colors[i * 2 + 1] = color.argb();
        }
        c.fillRects(rects, colors, n);
    }
}
