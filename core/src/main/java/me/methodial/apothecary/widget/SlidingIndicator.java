package me.methodial.apothecary.widget;

import me.methodial.apothecary.anim.Spring;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;

/**
 * An absolutely positioned highlight that springs to the bounds of a target sibling
 * (tab underline, segmented-control pill, nav highlight). Stiffness/damping are tweakable.
 */
public class SlidingIndicator extends Node {
    private final Spring x = new Spring(0), y = new Spring(0), w = new Spring(0), h = new Spring(0);
    private Node target;
    private boolean placed;
    private boolean horizontalOnly;

    /** Only follow the target's x/width (vertical placement comes from the sheet, e.g. an underline). */
    public SlidingIndicator horizontalOnly() { this.horizontalOnly = true; return this; }

    public SlidingIndicator(String type) {
        super(type);
        baseStyle(s -> s.absolute().pointerEvents(me.methodial.apothecary.style.PointerEvents.NONE));
        springs(260, 26);
    }

    public SlidingIndicator springs(float stiffness, float damping) {
        for (Spring s : new Spring[]{x, y, w, h}) s.stiffness(stiffness).damping(damping);
        return this;
    }

    public void target(Node n) { this.target = n; }

    @Override
    protected void onFrame(float dt) {
        if (target == null || target.parent() == null || target.width() <= 0) return;
        // Layout positions are relative to the parent's border box, but absolute insets are relative
        // to its padding box: subtract the border so the indicator lines up exactly.
        Node p = parent();
        float bx = p == null ? 0 : p.borderEdges().left(), by = p == null ? 0 : p.borderEdges().top();
        float tx = target.x() - bx, ty = target.y() - by;
        if (!placed) {
            x.snap(tx); y.snap(ty); w.snap(target.width()); h.snap(target.height());
            placed = true;
        } else {
            x.target(tx); y.target(ty); w.target(target.width()); h.target(target.height());
            x.tick(dt); y.tick(dt); w.tick(dt); h.tick(dt);
        }
        setPx(Props.LEFT, x.value());
        setPx(Props.WIDTH, w.value());
        if (!horizontalOnly) {
            setPx(Props.TOP, y.value());
            setPx(Props.HEIGHT, h.value());
        }
    }

    private void setPx(me.methodial.apothecary.style.Prop<Length> p, float v) {
        Length l = Length.px(Math.round(v * 4) / 4f);
        if (!l.equals(style().get(p))) style().set(p, l);
    }
}
