import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/**
 * Layout grid: reads the boxes collect.jsnip dumped and the frame it grabbed, and shows what's uneven.
 *
 *   java LayoutGrid.java <gameDir> [focusX0 focusX1]
 *
 * Writes <gameDir>/layout-grid/grid.png: every box outlined (colour by depth), the edges most boxes share drawn as
 * guides, and each finding marked in red with its number. Prints the findings:
 *   EDGE    a left/right edge 1-12 px off an edge most siblings-of-the-column share (near misses, not deliberate insets)
 *   HEIGHT  boxes on one row (same parent, overlapping vertically) with different heights
 *   GAP     a column whose gaps between consecutive children differ
 *   OVER    a box sticking out of its parent (when the parent doesn't scroll)
 *   TEXT    a text box wider than its parent's content (cut off or overflowing)
 */
public class LayoutGrid {
    record Box(int id, int parent, int depth, float x0, float y0, float x1, float y1, boolean scrolls, String type, String text) {
        float w() { return x1 - x0; }
        float h() { return y1 - y0; }
        String name() { return "#" + id + " " + type + (text.isEmpty() ? "" : " \"" + (text.length() > 24 ? text.substring(0, 24) + "…" : text) + "\""); }
    }

    record Finding(String kind, Box box, String what) {}

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        List<String> lines = Files.readAllLines(new File(dir, "layout-grid/boxes.tsv").toPath());
        String[] head = lines.get(0).split("\t");
        Map<Integer, Box> boxes = new LinkedHashMap<>();
        for (String l : lines.subList(1, lines.size())) {
            String[] c = l.split("\t", -1);
            if (c.length < 9) continue;
            Box b = new Box(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), Float.parseFloat(c[3]),
                    Float.parseFloat(c[4]), Float.parseFloat(c[5]), Float.parseFloat(c[6]), Boolean.parseBoolean(c[7]), c[8],
                    c.length > 9 ? c[9] : "");
            if (b.w() > 0.5f && b.h() > 0.5f) boxes.put(b.id, b);
        }
        Map<Integer, List<Box>> kids = new HashMap<>();
        for (Box b : boxes.values()) kids.computeIfAbsent(b.parent, k -> new ArrayList<>()).add(b);

        List<Finding> findings = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        // EDGE: within each scrolling list / card (any box with >= 4 descendants), the left and right edges its
        // full-width-ish descendants use. An edge 1..12 px off the most common one is a near miss.
        for (Box container : boxes.values()) {
            List<Box> desc = new ArrayList<>();
            // siblings only: an inset one level down (a card's padding) is a decision, not a miss
            collect(container, kids, desc, 0, 1);
            List<Box> wide = desc.stream().filter(d -> d.w() >= container.w() * 0.55f).toList();
            if (wide.size() < 3) continue;
            for (boolean left : new boolean[]{true, false}) {
                Map<Integer, Integer> votes = new HashMap<>();
                for (Box d : wide) votes.merge(Math.round(left ? d.x0 : d.x1), 1, Integer::sum);
                int common = votes.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
                if (votes.get(common) < 3) continue;
                for (Box d : wide) {
                    int e = Math.round(left ? d.x0 : d.x1), off = e - common;
                    if (Math.abs(off) >= 1 && Math.abs(off) <= 12 && seen.add(d.id + (left ? "L" : "R"))) {
                        findings.add(new Finding("EDGE", d, (left ? "left " : "right ") + (off > 0 ? "+" : "") + off
                                + "px from " + common + " (in " + container.name() + ")"));
                    }
                }
            }
        }
        // HEIGHT, GAP, OVER, TEXT
        for (Box p : boxes.values()) {
            List<Box> ks = kids.getOrDefault(p.id, List.of());
            List<Box> sorted = new ArrayList<>(ks);
            sorted.sort(Comparator.comparing(Box::y0));
            // rows: children overlapping vertically
            for (int i = 0; i < ks.size(); i++) for (int j = i + 1; j < ks.size(); j++) {
                Box a = ks.get(i), b = ks.get(j);
                boolean sameRow = Math.abs(a.y0 - b.y0) < 2 || Math.abs(a.y1 - b.y1) < 2;
                if (sameRow && Math.abs(a.x0 - b.x0) > 1 && a.h() > 6 && b.h() > 6 && Math.abs(a.h() - b.h()) >= 1.5f
                        && !a.type.startsWith("text") && !b.type.startsWith("text")) {
                    findings.add(new Finding("HEIGHT", b, String.format("%.0f tall beside %s at %.0f", b.h(), a.name(), a.h())));
                }
            }
            // column gaps
            if (sorted.size() >= 3) {
                List<Integer> gaps = new ArrayList<>();
                boolean column = true;
                for (int i = 1; i < sorted.size(); i++) {
                    float g = sorted.get(i).y0 - sorted.get(i - 1).y1;
                    if (g < -0.5f) { column = false; break; }
                    gaps.add(Math.round(g));
                }
                if (column && new HashSet<>(gaps).size() > 2) {
                    findings.add(new Finding("GAP", p, "gaps " + gaps));
                }
            }
            if (!p.scrolls) for (Box k : ks) {
                if (k.x0 < p.x0 - 0.5f || k.x1 > p.x1 + 0.5f || k.y0 < p.y0 - 0.5f || k.y1 > p.y1 + 0.5f) {
                    findings.add(new Finding("OVER", k, String.format("sticks out of %s by l%.0f r%.0f t%.0f b%.0f", p.name(),
                            Math.max(0, p.x0 - k.x0), Math.max(0, k.x1 - p.x1), Math.max(0, p.y0 - k.y0), Math.max(0, k.y1 - p.y1))));
                }
            }
        }

        // the picture: the root box spans the window, which gives UI units to image pixels
        BufferedImage shot = ImageIO.read(new File(dir, "screenshots/layout-grid.png"));
        Box rootBox = boxes.values().iterator().next();
        float scale = shot.getWidth() / rootBox.x1();
        Graphics2D g = shot.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color[] depth = {new Color(0x40C4FF), new Color(0x69F0AE), new Color(0xFFD740), new Color(0xE040FB), new Color(0xFF6E40)};
        for (Box b : boxes.values()) {
            g.setColor(withAlpha(depth[b.depth % depth.length], 110));
            g.drawRect(Math.round(b.x0 * scale), Math.round(b.y0 * scale), Math.round(b.w() * scale) - 1, Math.round(b.h() * scale) - 1);
        }
        g.setFont(new Font(Font.MONOSPACED, Font.BOLD, Math.max(11, Math.round(scale * 4))));
        int n = 1;
        for (Finding f : findings) {
            Box b = f.box;
            g.setColor(new Color(255, 40, 40, 220));
            g.setStroke(new BasicStroke(2));
            g.drawRect(Math.round(b.x0 * scale), Math.round(b.y0 * scale), Math.round(b.w() * scale) - 1, Math.round(b.h() * scale) - 1);
            g.drawString(Integer.toString(n), Math.round(b.x1 * scale) + 2, Math.round(b.y0 * scale) + 12);
            System.out.printf("%2d %-6s %s: %s%n", n++, f.kind, b.name(), f.what);
        }
        ImageIO.write(shot, "png", new File(dir, "layout-grid/grid.png"));
        System.out.println(findings.size() + " findings, " + boxes.size() + " boxes -> " + new File(dir, "layout-grid/grid.png"));
    }

    private static void collect(Box b, Map<Integer, List<Box>> kids, List<Box> out, int depth, int max) {
        if (depth >= max) return;
        for (Box k : kids.getOrDefault(b.id, List.of())) {
            out.add(k);
            collect(k, kids, out, depth + 1, max);
        }
    }

    /** A wrapper whose only child carries the same edge is one decision, not two. */
    private static boolean kidsShareEdge(Box b, Map<Integer, List<Box>> kids, int edge, boolean left) {
        return false;
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
    }
}
