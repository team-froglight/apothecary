package me.methodial.apothecary.node;

import me.methodial.apothecary.geom.Edges;
import me.methodial.apothecary.style.Align;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Display;
import me.methodial.apothecary.style.FlexDirection;
import me.methodial.apothecary.style.FlexWrap;
import me.methodial.apothecary.style.Justify;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Overflow;
import me.methodial.apothecary.style.TextOverflow;
import me.methodial.apothecary.style.LengthEdges;
import me.methodial.apothecary.style.Position;
import me.methodial.apothecary.style.Props;

import java.util.ArrayList;
import java.util.List;

/**
 * The layout engine: box sizing, flexbox (CSS Flexible Box Layout §9 simplified), block flow (a
 * column flex), absolute positioning, and CSS grid (see {@link GridLayout}). All sizes are border-box.
 * <p>NaN means "undefined" throughout: an indefinite size or unbounded available space.
 */
final class Layout {
    private Layout() { }

    static final float NAN = Float.NaN;

    /** Lays out the whole tree rooted at {@code root} into a w*h viewport. */
    static void layoutRoot(Node root, float w, float h) {
        root.setLayoutPosition(0, 0);
        size(root, w, h, w, h, w, h, true);
    }

    /**
     * Computes (and when {@code commit}, applies) the border-box size of {@code n}.
     *
     * @param fixedW exact width imposed by the parent (flexed/stretched), or NaN
     * @param availW space available for content-based sizing, or NaN for unbounded
     * @param cbW    containing block width for percentages, or NaN
     */
    static float[] size(Node n, float fixedW, float fixedH, float availW, float availH, float cbW, float cbH, boolean commit) {
        ComputedStyle cs = n.computed();
        if (cs == null || cs.display() == Display.NONE) {
            if (commit) n.setLayoutSize(0, 0);
            return new float[]{0, 0};
        }
        float[] key = {fixedW, fixedH, availW, availH, cbW, cbH};
        if (commit && !n.layoutDirty && LayoutCache.same(n.cache.committedKey, key)) return new float[]{n.width, n.height};
        if (!commit) {
            float[] hit = n.cache.get(key);
            if (hit != null) return hit;
        }

        Edges pad = cs.padding(cbW), bor = cs.border();
        float ph = pad.horizontal() + bor.horizontal(), pv = pad.vertical() + bor.vertical();
        float minW = minOf(cs.get(Props.MIN_WIDTH), cbW), maxW = maxOf(cs.get(Props.MAX_WIDTH), cbW);
        float minH = minOf(cs.get(Props.MIN_HEIGHT), cbH), maxH = maxOf(cs.get(Props.MAX_HEIGHT), cbH);
        float ar = cs.get(Props.ASPECT_RATIO);

        float w = fixedW, h = fixedH;
        if (Float.isNaN(w)) w = cs.width().resolve(cbW);
        if (Float.isNaN(h)) h = cs.height().resolve(cbH);
        if (!Float.isNaN(ar) && ar > 0) {
            if (Float.isNaN(w) && !Float.isNaN(h)) w = h * ar;
            else if (Float.isNaN(h) && !Float.isNaN(w)) h = w / ar;
        }
        if (!Float.isNaN(w)) w = Math.max(ph, clamp(w, minW, maxW));
        if (!Float.isNaN(h)) h = Math.max(pv, clamp(h, minH, maxH));

        if (Float.isNaN(w) || Float.isNaN(h)) {
            float innerW = Float.isNaN(w) ? NAN : w - ph, innerH = Float.isNaN(h) ? NAN : h - pv;
            float aw = !Float.isNaN(innerW) ? innerW : Float.isNaN(availW) ? NAN : Math.max(0, Math.min(availW, maxW) - ph);
            float ah = !Float.isNaN(innerH) ? innerH : Float.isNaN(availH) ? NAN : Math.max(0, Math.min(availH, maxH) - pv);
            float[] content = content(n, cs, innerW, innerH, aw, ah, false, pad, bor);
            if (Float.isNaN(w)) {
                w = Math.max(ph, clamp(content[0] + ph, minW, maxW));
                if (!Float.isNaN(ar) && ar > 0 && !Float.isNaN(h)) w = h * ar;
            }
            if (Float.isNaN(h)) {
                // Height can depend on the final width (wrapping): re-measure when the width got clamped.
                float ch = content[1];
                if (Math.abs(w - ph - content[0]) > 0.01f && (Float.isNaN(fixedW))) {
                    ch = content(n, cs, w - ph, NAN, w - ph, ah, false, pad, bor)[1];
                }
                h = Math.max(pv, clamp(ch + pv, minH, maxH));
                if (!Float.isNaN(ar) && ar > 0) h = Math.max(pv, clamp(w / ar, minH, maxH));
            }
        }

        float[] result = {w, h};
        if (commit) {
            n.setLayoutSize(w, h);
            content(n, cs, w - ph, h - pv, w - ph, h - pv, true, pad, bor);
            n.layoutDirty = false;
            n.cache.committedKey = key;
            n.onLayout();
        }
        n.cache.put(key, result);
        return result;
    }

    /** Lays out (or measures) the content box; returns the content size. */
    private static float[] content(Node n, ComputedStyle cs, float innerW, float innerH, float availW, float availH,
                                   boolean commit, Edges pad, Edges bor) {
        float[] c;
        if (n.isLeaf()) {
            float[] m = n.measureContent(Float.isNaN(innerW) ? availW : innerW, Float.isNaN(innerH) ? availH : innerH);
            c = m == null ? new float[]{0, 0} : m;
            if (commit) {
                n.contentW = n.width;
                n.contentH = n.height;
            }
            return c;
        }
        float offX = bor.left() + pad.left(), offY = bor.top() + pad.top();
        c = switch (cs.display()) {
            case GRID -> GridLayout.layout(n, cs, innerW, innerH, availW, availH, commit, offX, offY);
            case BLOCK -> flex(n, cs, innerW, innerH, availW, availH, commit, offX, offY, true);
            default -> flex(n, cs, innerW, innerH, availW, availH, commit, offX, offY, false);
        };
        if (commit) {
            absolute(n, innerW + pad.horizontal(), innerH + pad.vertical(), bor);
            // Scroll extent: the border box needed to show every in-flow child plus trailing padding.
            float maxR = 0, maxB = 0;
            for (Node ch : n.children()) {
                ComputedStyle ccs = ch.computed();
                if (ccs == null || ccs.display() == Display.NONE) continue;
                Edges m = ccs.margin().resolve(innerW);
                maxR = Math.max(maxR, ch.x + ch.width + m.right());
                maxB = Math.max(maxB, ch.y + ch.height + m.bottom());
            }
            n.contentW = Math.max(n.width, maxR + pad.right() + bor.right());
            n.contentH = Math.max(n.height, maxB + pad.bottom() + bor.bottom());
        }
        return c;
    }

    // ================================================================== flexbox

    private static final class Item {
        Node node;
        ComputedStyle cs;
        float mainStartM, mainEndM, crossStartM, crossEndM;
        boolean autoMainStart, autoMainEnd, autoCrossStart, autoCrossEnd;
        float basis, hypo, minMain, maxMain, minCross, maxCross, styleCross;
        float grow, shrink;
        float target, cross;
        boolean frozen;
        Align align;
        float posMain, posCross;

        float outerHypo() { return hypo + mainStartM + mainEndM; }
        float outerTarget() { return target + mainStartM + mainEndM; }
        float outerCross() { return cross + crossStartM + crossEndM; }
    }

    private static final class Line {
        final List<Item> items = new ArrayList<>();
        float cross, offset;
    }

    static float[] flex(Node n, ComputedStyle cs, float innerW, float innerH, float availW, float availH,
                        boolean commit, float offX, float offY, boolean block) {
        FlexDirection dir = block ? FlexDirection.COLUMN : cs.get(Props.FLEX_DIRECTION);
        boolean row = dir.isRow(), rev = dir.isReverse();
        FlexWrap wrap = block ? FlexWrap.NOWRAP : cs.get(Props.FLEX_WRAP);
        Justify justify = block ? Justify.START : cs.get(Props.JUSTIFY_CONTENT);
        Align alignItems = block ? Align.STRETCH : cs.get(Props.ALIGN_ITEMS);
        Justify alignContent = cs.get(Props.ALIGN_CONTENT);

        float mainSize = row ? innerW : innerH, crossSize = row ? innerH : innerW;
        float availMain = row ? availW : availH, availCross = row ? availH : availW;
        float gapMain = (row ? cs.get(Props.COLUMN_GAP) : cs.get(Props.ROW_GAP)).resolveOr(mainSize, 0);
        float gapCross = (row ? cs.get(Props.ROW_GAP) : cs.get(Props.COLUMN_GAP)).resolveOr(crossSize, 0);
        float cbW = innerW, cbH = innerH;

        // ---- collect items
        List<Item> items = new ArrayList<>();
        for (Node ch : n.children()) {
            ComputedStyle ccs = ch.computed();
            if (ccs == null) continue;
            if (ccs.display() == Display.NONE) { if (commit) ch.setLayoutSize(0, 0); continue; }
            if (ccs.get(Props.POSITION) == Position.ABSOLUTE) continue;
            Item it = new Item();
            it.node = ch;
            it.cs = ccs;
            LengthEdges m = ccs.margin();
            Length ms = row ? m.left() : m.top(), me = row ? m.right() : m.bottom();
            Length cs0 = row ? m.top() : m.left(), ce = row ? m.bottom() : m.right();
            it.autoMainStart = ms.isAuto(); it.autoMainEnd = me.isAuto();
            it.autoCrossStart = cs0.isAuto(); it.autoCrossEnd = ce.isAuto();
            it.mainStartM = ms.resolveOr(cbW, 0); it.mainEndM = me.resolveOr(cbW, 0);
            it.crossStartM = cs0.resolveOr(cbW, 0); it.crossEndM = ce.resolveOr(cbW, 0);
            Edges cp = ccs.padding(cbW), cb = ccs.border();
            float pMain = row ? cp.horizontal() + cb.horizontal() : cp.vertical() + cb.vertical();
            float pCross = row ? cp.vertical() + cb.vertical() : cp.horizontal() + cb.horizontal();
            it.minMain = Math.max(pMain, minOf(row ? ccs.get(Props.MIN_WIDTH) : ccs.get(Props.MIN_HEIGHT), row ? cbW : cbH));
            it.maxMain = maxOf(row ? ccs.get(Props.MAX_WIDTH) : ccs.get(Props.MAX_HEIGHT), row ? cbW : cbH);
            it.minCross = Math.max(pCross, minOf(row ? ccs.get(Props.MIN_HEIGHT) : ccs.get(Props.MIN_WIDTH), row ? cbH : cbW));
            it.maxCross = maxOf(row ? ccs.get(Props.MAX_HEIGHT) : ccs.get(Props.MAX_WIDTH), row ? cbH : cbW);
            it.styleCross = (row ? ccs.height() : ccs.width()).resolve(row ? cbH : cbW);
            it.grow = block ? 0 : ccs.get(Props.FLEX_GROW);
            it.shrink = block ? 0 : ccs.get(Props.FLEX_SHRINK);
            Align self = ccs.get(Props.ALIGN_SELF);
            it.align = self == Align.AUTO ? alignItems : self;

            float basis = block ? NAN : ccs.get(Props.FLEX_BASIS).resolve(mainSize);
            if (Float.isNaN(basis)) basis = (row ? ccs.width() : ccs.height()).resolve(row ? cbW : cbH);
            if (Float.isNaN(basis)) {
                float am = Float.isNaN(availMain) ? NAN : Math.max(0, availMain - it.mainStartM - it.mainEndM);
                float acr = Float.isNaN(availCross) ? NAN : Math.max(0, availCross - it.crossStartM - it.crossEndM);
                if (row) {
                    float[] s = size(ch, NAN, it.styleCross, am, acr, cbW, cbH, false);
                    basis = s[0];
                } else {
                    float fw = it.styleCross;
                    if (Float.isNaN(fw) && it.align == Align.STRETCH && !Float.isNaN(crossSize) && !it.autoCrossStart && !it.autoCrossEnd) {
                        fw = Math.max(0, crossSize - it.crossStartM - it.crossEndM);
                    }
                    float[] s = size(ch, fw, NAN, Float.isNaN(fw) ? acr : fw, am, cbW, cbH, false);
                    basis = s[1];
                }
            }
            it.basis = basis;
            // CSS automatic minimum size: items don't shrink below their content unless they scroll/clip,
            // set an explicit min size, or truncate with an ellipsis.
            Length minStyle = row ? ccs.get(Props.MIN_WIDTH) : ccs.get(Props.MIN_HEIGHT);
            Overflow ov = row ? ccs.get(Props.OVERFLOW_X) : ccs.get(Props.OVERFLOW_Y);
            if (!block && minStyle.isAuto() && ov == Overflow.VISIBLE && ccs.get(Props.TEXT_OVERFLOW) != TextOverflow.ELLIPSIS
                    && it.shrink > 0) {
                float autoMin;
                if (row) {
                    autoMin = size(ch, NAN, it.styleCross, 0, NAN, cbW, cbH, false)[0];
                } else {
                    float fw = it.styleCross;
                    if (Float.isNaN(fw) && it.align == Align.STRETCH && !Float.isNaN(crossSize)) fw = Math.max(0, crossSize - it.crossStartM - it.crossEndM);
                    float acr = Float.isNaN(availCross) ? NAN : Math.max(0, availCross - it.crossStartM - it.crossEndM);
                    autoMin = size(ch, fw, NAN, Float.isNaN(fw) ? acr : fw, NAN, cbW, cbH, false)[1];
                }
                float styleMain = (row ? ccs.width() : ccs.height()).resolve(row ? cbW : cbH);
                if (!Float.isNaN(styleMain)) autoMin = Math.min(autoMin, styleMain);
                it.minMain = Math.max(it.minMain, Math.min(autoMin, it.maxMain));
            }
            it.hypo = clamp(basis, it.minMain, it.maxMain);
            items.add(it);
        }
        items.sort((a, b) -> Integer.compare(a.cs.get(Props.ORDER), b.cs.get(Props.ORDER)));

        // ---- break into lines
        List<Line> lines = new ArrayList<>();
        float lineLimit = !Float.isNaN(mainSize) ? mainSize : availMain;
        Line cur = new Line();
        lines.add(cur);
        float acc = 0;
        for (Item it : items) {
            float add = it.outerHypo() + (cur.items.isEmpty() ? 0 : gapMain);
            if (wrap != FlexWrap.NOWRAP && !Float.isNaN(lineLimit) && !cur.items.isEmpty() && acc + add > lineLimit + 0.01f) {
                cur = new Line();
                lines.add(cur);
                acc = 0;
                add = it.outerHypo();
            }
            cur.items.add(it);
            acc += add;
        }

        // ---- resolve flexible lengths
        float usedMainMax = 0;
        for (Line line : lines) {
            float sumHypo = 0;
            for (Item it : line.items) sumHypo += it.outerHypo();
            float gaps = Math.max(0, line.items.size() - 1) * gapMain;
            float lineMain;
            // An auto-width row fits its content but no wider than the space it has (shrink-to-fit);
            // an auto-height column simply grows to its content, as in CSS.
            if (!Float.isNaN(mainSize)) lineMain = mainSize;
            else if (!Float.isNaN(availMain) && row) lineMain = Math.min(availMain, sumHypo + gaps);
            else lineMain = sumHypo + gaps;
            resolveFlexible(line.items, lineMain - gaps);
            float used = gaps;
            for (Item it : line.items) used += it.outerTarget();
            usedMainMax = Math.max(usedMainMax, used);
        }
        float mainFinal = !Float.isNaN(mainSize) ? mainSize : usedMainMax;

        // ---- cross sizes
        for (Line line : lines) {
            float lc = 0;
            for (Item it : line.items) {
                Node ch = it.node;
                if (!Float.isNaN(it.styleCross)) {
                    it.cross = clamp(it.styleCross, it.minCross, it.maxCross);
                } else {
                    float acr = Float.isNaN(availCross) ? NAN : Math.max(0, availCross - it.crossStartM - it.crossEndM);
                    if (row) {
                        it.cross = size(ch, it.target, NAN, it.target, acr, cbW, cbH, false)[1];
                    } else {
                        float fw = NAN;
                        if (it.align == Align.STRETCH && !Float.isNaN(crossSize) && !it.autoCrossStart && !it.autoCrossEnd) {
                            fw = Math.max(0, crossSize - it.crossStartM - it.crossEndM);
                        }
                        it.cross = size(ch, fw, it.target, Float.isNaN(fw) ? acr : fw, it.target, cbW, cbH, false)[0];
                    }
                    it.cross = clamp(it.cross, it.minCross, it.maxCross);
                }
                lc = Math.max(lc, it.outerCross());
            }
            line.cross = lc;
        }
        if (wrap == FlexWrap.NOWRAP && !Float.isNaN(crossSize) && lines.size() == 1) lines.get(0).cross = crossSize;

        // ---- align-content: distribute cross space between lines
        float linesTotal = Math.max(0, lines.size() - 1) * gapCross;
        for (Line l : lines) linesTotal += l.cross;
        float crossFinal = !Float.isNaN(crossSize) ? crossSize : linesTotal;
        float extra = crossFinal - linesTotal;
        float lStart = 0, lBetween = 0;
        if (extra > 0 && wrap != FlexWrap.NOWRAP) {
            switch (alignContent) {
                case STRETCH -> { for (Line l : lines) l.cross += extra / lines.size(); }
                case END -> lStart = extra;
                case CENTER -> lStart = extra / 2;
                case SPACE_BETWEEN -> lBetween = lines.size() > 1 ? extra / (lines.size() - 1) : 0;
                case SPACE_AROUND -> { lBetween = extra / lines.size(); lStart = lBetween / 2; }
                case SPACE_EVENLY -> { lBetween = extra / (lines.size() + 1); lStart = lBetween; }
                default -> { }
            }
        }
        float cursorCross = lStart;
        for (Line l : lines) {
            l.offset = cursorCross;
            cursorCross += l.cross + gapCross + lBetween;
        }
        if (wrap == FlexWrap.WRAP_REVERSE) {
            for (Line l : lines) l.offset = crossFinal - l.offset - l.cross;
        }

        // ---- stretch, then position on both axes
        for (Line line : lines) {
            for (Item it : line.items) {
                if (it.align == Align.STRETCH && Float.isNaN(it.styleCross) && !it.autoCrossStart && !it.autoCrossEnd) {
                    it.cross = clamp(line.cross - it.crossStartM - it.crossEndM, it.minCross, it.maxCross);
                }
                float free = line.cross - it.outerCross();
                float pc;
                if (it.autoCrossStart && it.autoCrossEnd) pc = it.crossStartM + Math.max(0, free) / 2;
                else if (it.autoCrossStart) pc = it.crossStartM + Math.max(0, free);
                else if (it.autoCrossEnd) pc = it.crossStartM;
                else pc = switch (it.align) {
                    case END -> line.cross - it.cross - it.crossEndM;
                    case CENTER -> it.crossStartM + free / 2;
                    default -> it.crossStartM;
                };
                it.posCross = line.offset + pc;
            }

            float used = Math.max(0, line.items.size() - 1) * gapMain;
            int autoMargins = 0;
            for (Item it : line.items) {
                used += it.outerTarget();
                if (it.autoMainStart) autoMargins++;
                if (it.autoMainEnd) autoMargins++;
            }
            float free = mainFinal - used;
            float start = 0, between = 0, autoShare = 0;
            if (autoMargins > 0 && free > 0) {
                autoShare = free / autoMargins;
            } else {
                int k = line.items.size();
                switch (justify) {
                    case END -> start = free;
                    case CENTER -> start = free / 2;
                    case SPACE_BETWEEN -> between = free > 0 && k > 1 ? free / (k - 1) : 0;
                    case SPACE_AROUND -> { if (free > 0) { between = free / k; start = between / 2; } else start = free / 2; }
                    case SPACE_EVENLY -> { if (free > 0) { between = free / (k + 1); start = between; } else start = free / 2; }
                    default -> { }
                }
            }
            float cursor = start;
            for (Item it : line.items) {
                cursor += it.mainStartM + (it.autoMainStart ? autoShare : 0);
                it.posMain = cursor;
                cursor += it.target + it.mainEndM + (it.autoMainEnd ? autoShare : 0) + gapMain + between;
            }
            if (rev) for (Item it : line.items) it.posMain = mainFinal - it.posMain - it.target;
        }

        // ---- commit
        if (commit) {
            for (Line line : lines) {
                for (Item it : line.items) {
                    float x = row ? it.posMain : it.posCross, y = row ? it.posCross : it.posMain;
                    float w = row ? it.target : it.cross, h = row ? it.cross : it.target;
                    it.node.setLayoutPosition(offX + x, offY + y);
                    size(it.node, w, h, w, h, cbW, cbH, true);
                }
            }
        }
        return row ? new float[]{mainFinal, crossFinal} : new float[]{crossFinal, mainFinal};
    }

    /** CSS flex "resolve flexible lengths": grow or shrink with min/max freezing. */
    private static void resolveFlexible(List<Item> items, float space) {
        float sumHypo = 0;
        for (Item it : items) sumHypo += it.outerHypo();
        boolean growing = space > sumHypo;
        for (Item it : items) {
            it.target = it.hypo;
            it.frozen = growing ? it.grow <= 0 : it.shrink <= 0;
            if (growing && it.basis > it.hypo) it.frozen = true;
            if (!growing && it.basis < it.hypo) it.frozen = true;
        }
        for (int iter = 0; iter < 12; iter++) {
            float remaining = space;
            float factorSum = 0;
            boolean anyUnfrozen = false;
            for (Item it : items) {
                if (it.frozen) remaining -= it.outerTarget();
                else {
                    remaining -= it.basis + it.mainStartM + it.mainEndM;
                    factorSum += growing ? it.grow : it.shrink * it.basis;
                    anyUnfrozen = true;
                }
            }
            if (!anyUnfrozen) break;
            if (growing) {
                float growSum = 0;
                for (Item it : items) if (!it.frozen) growSum += it.grow;
                if (growSum < 1) remaining *= growSum;
            }
            float violation = 0;
            for (Item it : items) {
                if (it.frozen) continue;
                float t;
                if (factorSum <= 0) t = it.basis;
                else if (growing) t = it.basis + remaining * (it.grow / factorSum);
                else t = it.basis + remaining * (it.shrink * it.basis / factorSum);
                float c = clamp(t, it.minMain, it.maxMain);
                violation += c - t;
                it.target = c;
            }
            if (Math.abs(violation) < 0.001f) {
                for (Item it : items) it.frozen = true;
                break;
            }
            for (Item it : items) {
                if (it.frozen) continue;
                float unclamped = it.target;
                if (violation > 0 && unclamped <= it.minMain + 0.0001f) it.frozen = true;
                else if (violation < 0 && unclamped >= it.maxMain - 0.0001f) it.frozen = true;
            }
        }
        for (Item it : items) it.target = Math.max(0, it.target);
    }

    // ================================================================== absolute positioning

    private static void absolute(Node n, float cbW, float cbH, Edges bor) {
        for (Node ch : n.children()) {
            ComputedStyle ccs = ch.computed();
            if (ccs == null || ccs.display() == Display.NONE || ccs.get(Props.POSITION) != Position.ABSOLUTE) continue;
            float l = ccs.get(Props.LEFT).resolve(cbW), r = ccs.get(Props.RIGHT).resolve(cbW);
            float t = ccs.get(Props.TOP).resolve(cbH), b = ccs.get(Props.BOTTOM).resolve(cbH);
            Edges m = ccs.margin().resolve(cbW);
            float fw = ccs.width().resolve(cbW), fh = ccs.height().resolve(cbH);
            if (Float.isNaN(fw) && !Float.isNaN(l) && !Float.isNaN(r)) fw = Math.max(0, cbW - l - r - m.horizontal());
            if (Float.isNaN(fh) && !Float.isNaN(t) && !Float.isNaN(b)) fh = Math.max(0, cbH - t - b - m.vertical());
            float aw = Float.isNaN(fw) ? Math.max(0, cbW - (Float.isNaN(l) ? 0 : l) - (Float.isNaN(r) ? 0 : r) - m.horizontal()) : fw;
            float ah = Float.isNaN(fh) ? NAN : fh;
            float[] s = size(ch, fw, fh, aw, ah, cbW, cbH, false);
            float w = s[0], h = s[1];
            float x = !Float.isNaN(l) ? l + m.left() : !Float.isNaN(r) ? cbW - r - w - m.right() : m.left();
            float y = !Float.isNaN(t) ? t + m.top() : !Float.isNaN(b) ? cbH - b - h - m.bottom() : m.top();
            // margin: auto centering when both insets are set
            LengthEdges lm = ccs.margin();
            if (!Float.isNaN(l) && !Float.isNaN(r) && lm.left().isAuto() && lm.right().isAuto()) x = l + (cbW - l - r - w) / 2;
            if (!Float.isNaN(t) && !Float.isNaN(b) && lm.top().isAuto() && lm.bottom().isAuto()) y = t + (cbH - t - b - h) / 2;
            ch.setLayoutPosition(bor.left() + x, bor.top() + y);
            size(ch, w, h, w, h, cbW, cbH, true);
        }
    }

    // ================================================================== helpers

    static float clamp(float v, float min, float max) {
        if (!Float.isNaN(max) && v > max) v = max;
        if (!Float.isNaN(min) && v < min) v = min;
        return v;
    }

    static float minOf(Length l, float base) {
        float v = l.resolve(base);
        return Float.isNaN(v) ? 0 : v;
    }

    static float maxOf(Length l, float base) {
        float v = l.resolve(base);
        return Float.isNaN(v) ? Float.POSITIVE_INFINITY : v;
    }
}
