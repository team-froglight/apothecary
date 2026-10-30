package me.methodial.apothecary.node;

import me.methodial.apothecary.geom.Edges;
import me.methodial.apothecary.style.Align;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Display;
import me.methodial.apothecary.style.GridAreas;
import me.methodial.apothecary.style.GridAutoFlow;
import me.methodial.apothecary.style.GridPlacement;
import me.methodial.apothecary.style.Justify;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Position;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.TrackSize;
import me.methodial.apothecary.style.TrackEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * CSS grid: explicit/implicit tracks (px, %, fr, auto, min-/max-content, minmax(), fit-content(),
 * repeat(n | auto-fill | auto-fit)), template areas, line/span/area placement, sparse and dense
 * auto-placement in either flow direction, and item/content alignment on both axes.
 */
final class GridLayout {
    private GridLayout() { }

    private static final float NAN = Float.NaN;

    private static final class Item {
        Node node;
        ComputedStyle cs;
        int r0 = -1, r1, c0 = -1, c1;   // track indices, end exclusive
        int rowSpan = 1, colSpan = 1;
        Edges margin;
        float minW, maxW, w, h;
    }

    static float[] layout(Node n, ComputedStyle cs, float innerW, float innerH, float availW, float availH,
                          boolean commit, float offX, float offY) {
        float gapC = cs.get(Props.COLUMN_GAP).resolveOr(innerW, 0), gapR = cs.get(Props.ROW_GAP).resolveOr(innerH, 0);
        GridAreas areas = cs.get(Props.GRID_TEMPLATE_AREAS);
        float spaceW = !Float.isNaN(innerW) ? innerW : availW, spaceH = !Float.isNaN(innerH) ? innerH : availH;
        List<TrackSize> cols = expand(cs.get(Props.GRID_TEMPLATE_COLUMNS), spaceW, gapC);
        List<TrackSize> rows = expand(cs.get(Props.GRID_TEMPLATE_ROWS), spaceH, gapR);
        int explicitCols = Math.max(cols.size(), areas.columnCount());
        int explicitRows = Math.max(rows.size(), areas.rowCount());

        List<Item> items = new ArrayList<>();
        for (Node ch : n.children()) {
            ComputedStyle ccs = ch.computed();
            if (ccs == null) continue;
            if (ccs.display() == Display.NONE) { if (commit) ch.setLayoutSize(0, 0); continue; }
            if (ccs.get(Props.POSITION) == Position.ABSOLUTE) continue;
            Item it = new Item();
            it.node = ch;
            it.cs = ccs;
            it.margin = ccs.margin().resolve(innerW);
            int[] r = resolve(ccs.get(Props.GRID_ROW), explicitRows, areas, true);
            int[] c = resolve(ccs.get(Props.GRID_COLUMN), explicitCols, areas, false);
            if (r[0] >= 0) { it.r0 = r[0]; it.r1 = r[1]; } else it.rowSpan = r[1];
            if (c[0] >= 0) { it.c0 = c[0]; it.c1 = c[1]; } else it.colSpan = c[1];
            if (it.r0 >= 0) it.rowSpan = it.r1 - it.r0;
            if (it.c0 >= 0) it.colSpan = it.c1 - it.c0;
            items.add(it);
        }
        items.sort((a, b) -> Integer.compare(a.cs.get(Props.ORDER), b.cs.get(Props.ORDER)));

        // ---- placement
        GridAutoFlow flow = cs.get(Props.GRID_AUTO_FLOW);
        int[] dims = place(items, explicitRows, explicitCols, flow);
        int nRows = dims[0], nCols = dims[1];
        List<TrackSize> autoCols = cs.get(Props.GRID_AUTO_COLUMNS), autoRows = cs.get(Props.GRID_AUTO_ROWS);
        List<TrackSize> colTracks = new ArrayList<>(cols), rowTracks = new ArrayList<>(rows);
        for (int i = colTracks.size(); i < nCols; i++) colTracks.add(autoCols.isEmpty() ? TrackSize.AUTO : autoCols.get((i - cols.size()) % autoCols.size()));
        for (int i = rowTracks.size(); i < nRows; i++) rowTracks.add(autoRows.isEmpty() ? TrackSize.AUTO : autoRows.get((i - rows.size()) % autoRows.size()));

        // auto-fit: collapse empty repeated tracks
        collapseAutoFit(cs.get(Props.GRID_TEMPLATE_COLUMNS), colTracks, items, false);
        collapseAutoFit(cs.get(Props.GRID_TEMPLATE_ROWS), rowTracks, items, true);

        // ---- size columns
        float cbW = innerW, cbH = innerH;
        Justify jc = cs.get(Props.JUSTIFY_CONTENT), ac = cs.get(Props.ALIGN_CONTENT);
        float[] colSizes = sizeTracks(colTracks, innerW, gapC, items, false, cbW, cbH, null,
                jc == Justify.STRETCH || jc == Justify.START);
        // ---- size rows (item heights depend on their column span widths)
        float[] rowSizes = sizeTracks(rowTracks, innerH, gapR, items, true, cbW, cbH, colSizes,
                ac == Justify.STRETCH);

        float[] colPos = positions(colSizes, gapC, innerW, jc);
        float[] rowPos = positions(rowSizes, gapR, innerH, ac);
        float totalW = total(colSizes, gapC), totalH = total(rowSizes, gapR);

        if (commit) {
            Align ji = cs.get(Props.JUSTIFY_ITEMS), ai = cs.get(Props.ALIGN_ITEMS);
            for (Item it : items) {
                float ax = colPos[it.c0], ay = rowPos[it.r0];
                float aw = span(colSizes, it.c0, it.c1, gapC), ah = span(rowSizes, it.r0, it.r1, gapR);
                Edges m = it.margin;
                Align js = it.cs.get(Props.JUSTIFY_SELF), as = it.cs.get(Props.ALIGN_SELF);
                if (js == Align.AUTO) js = ji;
                if (as == Align.AUTO) as = ai;
                float fw = it.cs.width().resolve(cbW), fh = it.cs.height().resolve(cbH);
                float boxW = Math.max(0, aw - m.horizontal()), boxH = Math.max(0, ah - m.vertical());
                if (Float.isNaN(fw) && js == Align.STRETCH) fw = boxW;
                if (Float.isNaN(fh) && as == Align.STRETCH) fh = boxH;
                float[] s = Layout.size(it.node, fw, fh, boxW, boxH, cbW, cbH, false);
                float w = s[0], h = s[1];
                float x = ax + m.left() + switch (js) {
                    case END -> boxW - w;
                    case CENTER -> (boxW - w) / 2;
                    default -> 0;
                };
                float y = ay + m.top() + switch (as) {
                    case END -> boxH - h;
                    case CENTER -> (boxH - h) / 2;
                    default -> 0;
                };
                it.node.setLayoutPosition(offX + x, offY + y);
                Layout.size(it.node, w, h, w, h, cbW, cbH, true);
            }
        }
        float contentW = !Float.isNaN(innerW) ? Math.max(innerW, totalW) : totalW;
        float contentH = !Float.isNaN(innerH) ? Math.max(innerH, totalH) : totalH;
        return new float[]{Float.isNaN(innerW) ? totalW : contentW, Float.isNaN(innerH) ? totalH : contentH};
    }

    // ------------------------------------------------------------------ templates

    /** Expands repeat() groups; auto-fill/auto-fit repeat as many times as fit into {@code space}. */
    static List<TrackSize> expand(List<TrackEntry> entries, float space, float gap) {
        List<TrackSize> out = new ArrayList<>();
        float fixedOthers = 0;
        int otherCount = 0;
        for (TrackEntry e : entries) {
            if (e instanceof TrackSize s) { fixedOthers += fixedSize(s, space); otherCount++; }
            else if (e instanceof TrackSize.Repeat r && r.count() > 0) {
                for (TrackSize t : r.tracks()) fixedOthers += fixedSize(t, space) * r.count();
                otherCount += r.tracks().size() * r.count();
            }
        }
        for (TrackEntry e : entries) {
            if (e instanceof TrackSize s) out.add(s);
            else if (e instanceof TrackSize.Repeat r) {
                int count = r.count();
                if (count <= 0) {
                    float rep = 0;
                    for (TrackSize t : r.tracks()) rep += Math.max(1, fixedSize(t, space));
                    rep += gap * r.tracks().size();
                    float free = Float.isNaN(space) ? 0 : space - fixedOthers - gap * otherCount + gap;
                    count = Float.isNaN(space) ? 1 : Math.max(1, (int) Math.floor((free + 0.001f) / rep));
                }
                for (int i = 0; i < count; i++) out.addAll(r.tracks());
            }
        }
        return out;
    }

    /** Size used when counting auto-fill repetitions: the fixed min or max of a track. */
    private static float fixedSize(TrackSize t, float space) {
        if (t instanceof TrackSize.Fixed f) return f.length().resolveOr(space, 0);
        if (t instanceof TrackSize.MinMax mm) {
            float a = fixedSize(mm.min(), space), b = fixedSize(mm.max(), space);
            return a > 0 ? a : b;
        }
        if (t instanceof TrackSize.FitContent fc) return fc.limit().resolveOr(space, 0);
        return 0;
    }

    private static void collapseAutoFit(List<TrackEntry> entries, List<TrackSize> tracks, List<Item> items, boolean rowsAxis) {
        boolean fit = false;
        for (TrackEntry e : entries) if (e instanceof TrackSize.Repeat r && r.count() == TrackSize.Repeat.AUTO_FIT) fit = true;
        if (!fit) return;
        for (int i = 0; i < tracks.size(); i++) {
            boolean used = false;
            for (Item it : items) {
                int a = rowsAxis ? it.r0 : it.c0, b = rowsAxis ? it.r1 : it.c1;
                if (i >= a && i < b) { used = true; break; }
            }
            if (!used) tracks.set(i, new TrackSize.Fixed(Length.ZERO));
        }
    }

    // ------------------------------------------------------------------ placement

    /** {start, end} for a definite placement, or {-1, span} for auto placement. */
    private static int[] resolve(GridPlacement p, int explicit, GridAreas areas, boolean rowsAxis) {
        GridPlacement.Line s = p.start(), e = p.end();
        int start = lineIndex(s, explicit, areas, rowsAxis, true);
        int end = lineIndex(e, explicit, areas, rowsAxis, false);
        int spanS = s instanceof GridPlacement.Line.Span sp ? sp.span() : 0;
        int spanE = e instanceof GridPlacement.Line.Span sp ? sp.span() : 0;
        if (start >= 0 && end >= 0) return new int[]{Math.min(start, end), Math.max(Math.max(start, end), Math.min(start, end) + 1)};
        if (start >= 0) return new int[]{start, start + Math.max(1, spanE)};
        if (end >= 0) {
            int sp = Math.max(1, spanS);
            return new int[]{Math.max(0, end - sp), Math.max(end, sp)};
        }
        return new int[]{-1, Math.max(1, Math.max(spanS, spanE))};
    }

    private static int lineIndex(GridPlacement.Line l, int explicit, GridAreas areas, boolean rowsAxis, boolean isStart) {
        if (l instanceof GridPlacement.Line.Index ix) {
            int line = ix.index() > 0 ? ix.index() : explicit + 2 + ix.index();
            return Math.max(0, line - 1);
        }
        if (l instanceof GridPlacement.Line.Area a) {
            int[] f = areas.find(a.name());
            if (f == null) return -1;
            return rowsAxis ? (isStart ? f[0] : f[2]) : (isStart ? f[1] : f[3]);
        }
        return -1;
    }

    /** Places items into the occupancy grid. Returns {rows, cols}. */
    private static int[] place(List<Item> items, int explicitRows, int explicitCols, GridAutoFlow flow) {
        boolean colFlow = flow.isColumn();
        boolean dense = flow.isDense();
        // Work in (major, minor): ROW flow fills rows (major=row), COLUMN flow fills columns.
        int minorCount = colFlow ? explicitRows : explicitCols;
        for (Item it : items) {
            int minorEnd = colFlow ? (it.r0 >= 0 ? it.r1 : it.rowSpan) : (it.c0 >= 0 ? it.c1 : it.colSpan);
            minorCount = Math.max(minorCount, minorEnd);
        }
        minorCount = Math.max(1, minorCount);
        Occupancy occ = new Occupancy(minorCount);

        // 1. fully definite
        for (Item it : items) if (it.r0 >= 0 && it.c0 >= 0) occ.mark(maj0(it, colFlow), maj1(it, colFlow), min0(it, colFlow), min1(it, colFlow));
        // 2. definite major axis only
        for (Item it : items) {
            boolean majDef = colFlow ? it.c0 >= 0 : it.r0 >= 0;
            boolean minDef = colFlow ? it.r0 >= 0 : it.c0 >= 0;
            if (!majDef || minDef) continue;
            int span = colFlow ? it.rowSpan : it.colSpan;
            int a0 = maj0(it, colFlow), a1 = maj1(it, colFlow);
            int b = 0;
            while (b + span <= minorCount && !occ.free(a0, a1, b, b + span)) b++;
            if (b + span > minorCount) b = 0;
            setMinor(it, colFlow, b, b + span);
            occ.mark(a0, a1, b, b + span);
        }
        // 3. auto major axis
        int curA = 0, curB = 0;
        for (Item it : items) {
            boolean majDef = colFlow ? it.c0 >= 0 : it.r0 >= 0;
            if (majDef) continue;
            boolean minDef = colFlow ? it.r0 >= 0 : it.c0 >= 0;
            int majSpan = colFlow ? it.colSpan : it.rowSpan;
            int minSpan = colFlow ? it.rowSpan : it.colSpan;
            if (dense) { curA = 0; curB = 0; }
            if (minDef) {
                int b0 = min0(it, colFlow), b1 = min1(it, colFlow);
                if (!dense && b0 < curB) curA++;
                int a = curA;
                while (!occ.free(a, a + majSpan, b0, b1)) a++;
                setMajor(it, colFlow, a, a + majSpan);
                occ.mark(a, a + majSpan, b0, b1);
                curA = a;
                curB = b1;
            } else {
                int a = curA, b = curB;
                while (true) {
                    if (b + minSpan > minorCount) { a++; b = 0; continue; }
                    if (occ.free(a, a + majSpan, b, b + minSpan)) break;
                    b++;
                }
                setMajor(it, colFlow, a, a + majSpan);
                setMinor(it, colFlow, b, b + minSpan);
                occ.mark(a, a + majSpan, b, b + minSpan);
                curA = a;
                curB = b + minSpan;
            }
        }
        int majorCount = Math.max(colFlow ? explicitCols : explicitRows, occ.rows());
        for (Item it : items) majorCount = Math.max(majorCount, maj1(it, colFlow));
        return colFlow ? new int[]{minorCount, majorCount} : new int[]{majorCount, minorCount};
    }

    private static int maj0(Item it, boolean colFlow) { return colFlow ? it.c0 : it.r0; }
    private static int maj1(Item it, boolean colFlow) { return colFlow ? it.c1 : it.r1; }
    private static int min0(Item it, boolean colFlow) { return colFlow ? it.r0 : it.c0; }
    private static int min1(Item it, boolean colFlow) { return colFlow ? it.r1 : it.c1; }

    private static void setMajor(Item it, boolean colFlow, int a, int b) {
        if (colFlow) { it.c0 = a; it.c1 = b; } else { it.r0 = a; it.r1 = b; }
    }

    private static void setMinor(Item it, boolean colFlow, int a, int b) {
        if (colFlow) { it.r0 = a; it.r1 = b; } else { it.c0 = a; it.c1 = b; }
    }

    /** Growable boolean matrix [major][minor]. */
    private static final class Occupancy {
        private final List<boolean[]> rows = new ArrayList<>();
        private final int minor;

        Occupancy(int minor) { this.minor = minor; }

        int rows() { return rows.size(); }

        boolean free(int a0, int a1, int b0, int b1) {
            if (b1 > minor) return false;
            for (int a = a0; a < a1; a++) {
                if (a >= rows.size()) continue;
                boolean[] r = rows.get(a);
                for (int b = b0; b < b1; b++) if (r[b]) return false;
            }
            return true;
        }

        void mark(int a0, int a1, int b0, int b1) {
            while (rows.size() < a1) rows.add(new boolean[minor]);
            for (int a = a0; a < a1; a++) for (int b = b0; b < Math.min(b1, minor); b++) rows.get(a)[b] = true;
        }
    }

    // ------------------------------------------------------------------ track sizing

    private static float[] sizeTracks(List<TrackSize> tracks, float available, float gap, List<Item> items, boolean rowsAxis,
                                      float cbW, float cbH, float[] colSizes, boolean stretchAuto) {
        int n = tracks.size();
        float[] base = new float[n], limit = new float[n], contentLimit = new float[n];
        boolean[] contentMin = new boolean[n], contentMax = new boolean[n];
        for (int i = 0; i < n; i++) {
            TrackSize mn = tracks.get(i).minSizing(), mx = tracks.get(i).maxSizing();
            float fmin = mn instanceof TrackSize.Fixed f ? f.length().resolve(available) : NAN;
            contentMin[i] = Float.isNaN(fmin);
            base[i] = Float.isNaN(fmin) ? 0 : fmin;
            float fmax = mx instanceof TrackSize.Fixed f ? f.length().resolve(available) : NAN;
            contentMax[i] = Float.isNaN(fmax) && !(mx instanceof TrackSize.Flex);
            limit[i] = mx instanceof TrackSize.Flex ? Float.POSITIVE_INFINITY : (Float.isNaN(fmax) ? 0 : fmax);
        }

        // Content contributions.
        float[][] contrib = new float[items.size()][];
        for (int k = 0; k < items.size(); k++) {
            Item it = items.get(k);
            int a = rowsAxis ? it.r0 : it.c0, b = rowsAxis ? it.r1 : it.c1;
            boolean needs = false;
            for (int i = a; i < b && i < n; i++) needs |= contentMin[i] || contentMax[i] || tracks.get(i).isFlexible();
            if (!needs) continue;
            contrib[k] = contribution(it, rowsAxis, cbW, cbH, colSizes, gap);
        }
        // Single-span items first, then spanning items distribute what's missing.
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < items.size(); k++) {
                if (contrib[k] == null) continue;
                Item it = items.get(k);
                int a = rowsAxis ? it.r0 : it.c0, b = Math.min(n, rowsAxis ? it.r1 : it.c1);
                int span = b - a;
                if (span <= 0 || (pass == 0) != (span == 1)) continue;
                float minC = contrib[k][0], maxC = contrib[k][1];
                if (span == 1) {
                    TrackSize t = tracks.get(a);
                    if (contentMin[a]) base[a] = Math.max(base[a], t.minSizing() instanceof TrackSize.MaxContent ? maxC : minC);
                    if (contentMax[a]) {
                        TrackSize mx = t.maxSizing();
                        float want = mx instanceof TrackSize.MinContent ? minC
                                : mx instanceof TrackSize.FitContent fc ? Math.max(minC, Math.min(maxC, fc.limit().resolveOr(available, maxC))) : maxC;
                        contentLimit[a] = Math.max(contentLimit[a], want);
                    }
                    if (tracks.get(a).isFlexible()) contentLimit[a] = Math.max(contentLimit[a], maxC);
                } else {
                    float have = gap * (span - 1);
                    int flexibleTargets = 0;
                    for (int i = a; i < b; i++) { have += base[i]; if (contentMin[i]) flexibleTargets++; }
                    if (minC > have && flexibleTargets > 0) {
                        float add = (minC - have) / flexibleTargets;
                        for (int i = a; i < b; i++) if (contentMin[i]) base[i] += add;
                    }
                    float haveMax = gap * (span - 1);
                    int maxTargets = 0;
                    for (int i = a; i < b; i++) { haveMax += Math.max(base[i], contentLimit[i]); if (contentMax[i]) maxTargets++; }
                    if (maxC > haveMax && maxTargets > 0) {
                        float add = (maxC - haveMax) / maxTargets;
                        for (int i = a; i < b; i++) if (contentMax[i]) contentLimit[i] = Math.max(base[i], contentLimit[i]) + add;
                    }
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (contentMax[i]) limit[i] = contentLimit[i];
            if (limit[i] < base[i]) limit[i] = base[i];
        }

        float gaps = gap * Math.max(0, n - 1);
        boolean anyFlex = false;
        for (TrackSize t : tracks) anyFlex |= t.isFlexible();

        // Maximize: grow non-flexible tracks toward their limits.
        if (!Float.isNaN(available)) {
            float free = available - gaps;
            for (float b : base) free -= b;
            distribute(base, limit, tracks, free, false);
        } else {
            for (int i = 0; i < n; i++) if (!tracks.get(i).isFlexible() && Float.isFinite(limit[i])) base[i] = limit[i];
        }

        if (anyFlex) {
            float frSize;
            if (!Float.isNaN(available)) {
                boolean[] inflexible = new boolean[n];
                frSize = 0;
                for (int iter = 0; iter < n + 1; iter++) {
                    float leftover = available - gaps, frSum = 0;
                    for (int i = 0; i < n; i++) {
                        if (!tracks.get(i).isFlexible() || inflexible[i]) leftover -= base[i];
                        else frSum += tracks.get(i).flexFactor();
                    }
                    frSize = Math.max(0, leftover) / Math.max(1, frSum);
                    boolean changed = false;
                    for (int i = 0; i < n; i++) {
                        if (tracks.get(i).isFlexible() && !inflexible[i] && base[i] > frSize * tracks.get(i).flexFactor()) {
                            inflexible[i] = true;
                            changed = true;
                        }
                    }
                    if (!changed) break;
                }
                for (int i = 0; i < n; i++) {
                    if (tracks.get(i).isFlexible() && !inflexible[i]) base[i] = Math.max(base[i], frSize * tracks.get(i).flexFactor());
                }
            } else {
                frSize = 0;
                for (int i = 0; i < n; i++) {
                    TrackSize t = tracks.get(i);
                    if (t.isFlexible() && t.flexFactor() > 0) frSize = Math.max(frSize, Math.max(base[i], contentLimit[i]) / t.flexFactor());
                }
                for (int i = 0; i < n; i++) if (tracks.get(i).isFlexible()) base[i] = Math.max(base[i], frSize * tracks.get(i).flexFactor());
            }
        } else if (stretchAuto && !Float.isNaN(available)) {
            float free = available - gaps;
            for (float b : base) free -= b;
            int autos = 0;
            for (TrackSize t : tracks) if (t.maxSizing() instanceof TrackSize.Auto) autos++;
            if (free > 0 && autos > 0) {
                for (int i = 0; i < n; i++) if (tracks.get(i).maxSizing() instanceof TrackSize.Auto) base[i] += free / autos;
            }
        }
        return base;
    }

    /** Spreads {@code free} space over growable tracks, capping each at its limit. */
    private static void distribute(float[] base, float[] limit, List<TrackSize> tracks, float free, boolean includeFlex) {
        for (int iter = 0; iter < base.length + 1 && free > 0.01f; iter++) {
            int count = 0;
            for (int i = 0; i < base.length; i++) {
                if ((includeFlex || !tracks.get(i).isFlexible()) && base[i] < limit[i]) count++;
            }
            if (count == 0) break;
            float share = free / count;
            for (int i = 0; i < base.length; i++) {
                if ((includeFlex || !tracks.get(i).isFlexible()) && base[i] < limit[i]) {
                    float add = Math.min(share, limit[i] - base[i]);
                    base[i] += add;
                    free -= add;
                }
            }
        }
    }

    /** {min-content, max-content} outer size of an item along an axis. */
    private static float[] contribution(Item it, boolean rowsAxis, float cbW, float cbH, float[] colSizes, float gapC) {
        Edges m = it.margin;
        if (!rowsAxis) {
            float minC = Layout.size(it.node, NAN, NAN, 0, NAN, cbW, cbH, false)[0];
            float maxC = Layout.size(it.node, NAN, NAN, NAN, NAN, cbW, cbH, false)[0];
            return new float[]{minC + m.horizontal(), maxC + m.horizontal()};
        }
        float aw = span(colSizes, it.c0, it.c1, gapC) - m.horizontal();
        Align js = it.cs.get(Props.JUSTIFY_SELF);
        float fw = js == Align.AUTO || js == Align.STRETCH ? Math.max(0, aw) : NAN;
        float h = Layout.size(it.node, fw, NAN, Math.max(0, aw), NAN, cbW, cbH, false)[1] + m.vertical();
        return new float[]{h, h};
    }

    private static float span(float[] sizes, int a, int b, float gap) {
        float s = 0;
        for (int i = a; i < b && i < sizes.length; i++) s += sizes[i];
        return s + gap * Math.max(0, Math.min(b, sizes.length) - a - 1);
    }

    private static float total(float[] sizes, float gap) {
        float s = 0;
        for (float v : sizes) s += v;
        return s + gap * Math.max(0, sizes.length - 1);
    }

    /** Track start offsets, applying justify-content / align-content when space is left over. */
    private static float[] positions(float[] sizes, float gap, float available, Justify j) {
        float[] pos = new float[sizes.length + 1];
        float free = Float.isNaN(available) ? 0 : available - total(sizes, gap);
        float start = 0, between = 0;
        int n = sizes.length;
        if (free > 0) {
            switch (j) {
                case END -> start = free;
                case CENTER -> start = free / 2;
                case SPACE_BETWEEN -> between = n > 1 ? free / (n - 1) : 0;
                case SPACE_AROUND -> { between = free / n; start = between / 2; }
                case SPACE_EVENLY -> { between = free / (n + 1); start = between; }
                default -> { }
            }
        }
        float p = start;
        for (int i = 0; i < n; i++) {
            pos[i] = p;
            p += sizes[i] + gap + between;
        }
        pos[n] = p;
        return pos;
    }
}
