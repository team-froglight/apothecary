package me.methodial.apothecary.node;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.sheet.Styleable;
import me.methodial.apothecary.theme.Theme;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.event.UIEvent;
import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.geom.Edges;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Display;
import me.methodial.apothecary.style.Overflow;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.PointerEvents;
import me.methodial.apothecary.style.Prop;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.style.Transform;
import me.methodial.apothecary.style.TransformOrigin;
import me.methodial.apothecary.style.TransitionSpec;
import me.methodial.apothecary.style.Visibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The element type everything is built from: a styled box that lays out and paints its children.
 * <p>
 * Subclasses customize three hooks: {@link #measureContent} (intrinsic size of leaf content),
 * {@link #paintContent} (drawing inside the content box) and {@link #onFrame} (per-frame logic).
 * <pre>{@code
 * Node card = new Node().cls("card").style(s -> s.column().gap(4).padding(8));
 * card.add(new Text("Hello"), new Button("Go").onClick(e -> ...));
 * }</pre>
 */
public class Node implements Styleable {
    // ------------------------------------------------------------------ tree & identity
    private Node parent;
    private final List<Node> children = new ArrayList<>();
    private UIRoot root;
    private String type;
    private String id;
    private final Set<String> classes = new LinkedHashSet<>();
    private final Set<String> states = new HashSet<>();
    private final Style inline = new Style();
    private final Style base = new Style();
    private Object userData;
    private int indexInParent;

    // ------------------------------------------------------------------ style state
    private ComputedStyle target;     // cascade result
    private ComputedStyle current;    // displayed: target + inheritance of animated parents + animations
    boolean styleDirty = true;
    boolean currentDirty = true;
    final Map<Integer, StyleResolver.PropTransition> transitions = new HashMap<>();
    final List<StyleResolver.RunningKeyframes> keyframes = new ArrayList<>();
    final List<StyleResolver.PropTween> tweens = new ArrayList<>();
    private Set<String> warned;

    // ------------------------------------------------------------------ layout state
    float x, y, width, height;
    float contentW, contentH;            // scrollable extent (border box space)
    boolean layoutDirty = true;
    boolean laidOut;
    final LayoutCache cache = new LayoutCache();
    private float layoutOffsetX, layoutOffsetY;
    private float flipStartX, flipStartY, flipStart, flipDuration;
    private Easing flipEasing = Easings.EASE_OUT;

    // ------------------------------------------------------------------ scroll
    float scrollX, scrollY, scrollTargetX, scrollTargetY;
    /** The wheel's momentum, in units per second; friction (scroll-inertia) wears it down. */
    private float flingX, flingY;

    // ------------------------------------------------------------------ paint & hit testing
    private Affine globalMatrix = Affine.IDENTITY;
    private Affine inverseMatrix = Affine.IDENTITY;
    private boolean painted;

    // ------------------------------------------------------------------ interaction
    private final Map<EventType<?>, List<Consumer<? super UIEvent>>> listeners = new LinkedHashMap<>();
    private boolean focusable;
    private boolean focusScope;
    private String accessibleName;
    private Supplier<Node> tooltip;
    private boolean disabled;

    public Node() { this("box"); }

    public Node(String type) {
        this.type = type;
        inline.onChange(this::markStyleDirty);
        base.onChange(this::markStyleDirty);
    }

    // ================================================================== identity / classes / states

    @Override public String typeName() { return type; }
    @Override public String id() { return id; }
    @Override public Set<String> classes() { return Collections.unmodifiableSet(classes); }
    @Override public boolean hasState(String s) { return states.contains(s); }
    @Override public Styleable styleParent() { return parent; }
    @Override public int indexInParent() { return indexInParent; }
    @Override public int siblingCount() { return parent == null ? 1 : parent.children.size(); }

    /** Changes the type selector name (e.g. a Node acting as "card"). */
    public Node type(String t) { this.type = t; markSubtreeStyleDirty(); return this; }

    public Node id(String id) { this.id = id; markSubtreeStyleDirty(); return this; }

    public Node cls(String... names) {
        boolean changed = false;
        for (String n : names) for (String part : n.trim().split("\\s+")) if (!part.isEmpty()) changed |= classes.add(part);
        if (changed) markSubtreeStyleDirty();
        return this;
    }

    public Node removeCls(String... names) {
        boolean changed = false;
        for (String n : names) changed |= classes.remove(n);
        if (changed) markSubtreeStyleDirty();
        return this;
    }

    public Node toggleCls(String name, boolean on) { return on ? cls(name) : removeCls(name); }

    public boolean hasCls(String name) { return classes.contains(name); }

    /** Sets or clears a pseudo-class state (hover, active, focus, checked, open, or any custom name). */
    public Node state(String s, boolean on) {
        if (on ? states.add(s) : states.remove(s)) markSubtreeStyleDirty();
        return this;
    }

    public Set<String> states() { return Collections.unmodifiableSet(states); }

    public Object userData() { return userData; }
    public Node userData(Object o) { this.userData = o; return this; }

    private java.util.Map<Object, Object> attachments;

    /** A value attached under {@code key} (helpers keep their own state on nodes this way). */
    public Object userDataFor(Object key) { return attachments == null ? null : attachments.get(key); }

    public Node userDataFor(Object key, Object value) {
        if (attachments == null) attachments = new java.util.HashMap<>();
        attachments.put(key, value);
        return this;
    }

    // ================================================================== style API

    /** The inline style (highest priority). Mutating it restyles the element. */
    public Style style() { return inline; }

    /**
     * The base style: declared in code, lowest priority (below every sheet rule), untouched by
     * {@code style().clear()}. Widgets put their structural layout here so themes can still override it.
     */
    public Style baseStyle() { return base; }

    public Node baseStyle(Consumer<Style> edit) { edit.accept(base); return this; }

    public Node style(Consumer<Style> edit) { edit.accept(inline); return this; }

    // ------------------------------------------------------------------ state styles

    private final Map<String, Style> stateStyles = new LinkedHashMap<>();

    /** Inline style applied while this element has {@code state} (no sheet needed). */
    public Node when(String state, Consumer<Style> edit) {
        Style s = stateStyles.computeIfAbsent(state, k -> {
            Style st = new Style();
            st.onChange(this::markStyleDirty);
            return st;
        });
        edit.accept(s);
        markStyleDirty();
        return this;
    }

    public Node hover(Consumer<Style> edit) { return when("hover", edit); }
    public Node active(Consumer<Style> edit) { return when("active", edit); }
    public Node focused(Consumer<Style> edit) { return when("focus", edit); }
    public Node whenDisabled(Consumer<Style> edit) { return when("disabled", edit); }

    Map<String, Style> stateStyleMap() { return stateStyles; }

    // ------------------------------------------------------------------ tokens & themes

    /** Applies a theme to this subtree (tokens only; the rules come from sheets). */
    public Node theme(Theme t) {
        for (Map.Entry<Token<?>, Object> e : t.values().entrySet()) putToken(e.getKey(), e.getValue());
        t.scopeStyle().accept(base);
        markSubtreeStyleDirty();
        return this;
    }

    /** Overrides one design token for this subtree. */
    public <T> Node token(Token<T> t, T value) {
        base.token(t, value);
        markSubtreeStyleDirty();
        return this;
    }

    @SuppressWarnings("unchecked")
    private <T> void putToken(Token<T> t, Object v) {
        if (v instanceof me.methodial.apothecary.theme.Ref<?> r) base.token(t, (me.methodial.apothecary.theme.Ref<T>) r);
        else base.token(t, (T) v);
    }

    /** A token value as this element sees it (after styling). */
    public <T> T token(Token<T> t) { return current == null ? me.methodial.apothecary.theme.TokenScope.DEFAULTS.get(t) : current.token(t); }

    /** Displayed style values (after transitions/animations). Null until the element is styled once. */
    public ComputedStyle computed() { return current; }

    /** Cascade result before animation. */
    public ComputedStyle targetStyle() { return target; }

    public <T> T get(Prop<T> p) { return current == null ? p.initial() : current.get(p); }

    /**
     * Animates a property from its displayed value to {@code to}; when done the value is written into
     * the inline style so it stays.
     */
    public <T> Node animate(Prop<T> prop, T to, float seconds, Easing easing) { return animate(prop, null, to, seconds, easing, null); }

    public <T> Node animate(Prop<T> prop, T from, T to, float seconds, Easing easing, Runnable onDone) {
        float now = root != null ? root.time() : 0;
        tweens.removeIf(t -> t.prop == prop);
        tweens.add(new StyleResolver.PropTween(prop, from, to, now, seconds, easing, () -> {
            inline.set(prop, to);
            if (onDone != null) onDone.run();
        }));
        currentDirty = true;
        return this;
    }

    /** Restarts keyframe animations from their beginning (e.g. replay an "enter" animation). */
    public Node replayAnimations() {
        keyframes.clear();
        markStyleDirty();
        return this;
    }

    /** Stops programmatic animations of this element (values jump to where the style says). */
    public Node stopAnimations() { tweens.clear(); currentDirty = true; return this; }

    public boolean isAnimating() { return !transitions.isEmpty() || !tweens.isEmpty() || hasRunningKeyframes() || flipDuration > 0; }

    private boolean hasRunningKeyframes() {
        for (StyleResolver.RunningKeyframes k : keyframes) if (!k.finished) return true;
        return false;
    }

    void warnOnce(String msg) {
        if (warned == null) warned = new HashSet<>();
        if (warned.add(msg) && root != null) root.warn(this, msg);
    }

    // ================================================================== tree API

    public Node parent() { return parent; }
    public List<Node> children() { return Collections.unmodifiableList(children); }
    public int childCount() { return children.size(); }
    public Node child(int i) { return children.get(i); }
    public UIRoot root() { return root; }
    public boolean isAttached() { return root != null; }

    public Node add(Node... nodes) {
        for (Node n : nodes) insert(children.size(), n);
        return this;
    }

    public Node add(List<? extends Node> nodes) {
        for (Node n : nodes) insert(children.size(), n);
        return this;
    }

    public Node insert(int index, Node n) {
        if (n.parent != null) n.parent.remove(n);
        n.parent = this;
        children.add(Math.max(0, Math.min(index, children.size())), n);
        reindex();
        if (root != null) n.attach(root);
        n.markSubtreeStyleDirty();
        markSubtreeStyleDirty();   // structural pseudo-classes of siblings
        markLayoutDirty();
        return this;
    }

    public Node remove(Node n) {
        if (n.parent != this) return this;
        children.remove(n);
        n.parent = null;
        reindex();
        if (n.root != null) n.detach();
        markSubtreeStyleDirty();
        markLayoutDirty();
        return this;
    }

    public void removeFromParent() { if (parent != null) parent.remove(this); }

    public Node clear() {
        for (Node c : new ArrayList<>(children)) remove(c);
        return this;
    }

    /** Replaces all children. */
    public Node setChildren(Node... nodes) { clear(); return add(nodes); }

    private void reindex() { for (int i = 0; i < children.size(); i++) children.get(i).indexInParent = i; }

    void attach(UIRoot r) {
        this.root = r;
        styleDirty = true;
        for (Node c : children) c.attach(r);
        dispatchLocal(EventType.MOUNT, new UIEvent());
        onAttach();
    }

    void detach() {
        UIRoot r = root;
        onDetach();
        dispatchLocal(EventType.UNMOUNT, new UIEvent());
        for (Node c : children) c.detach();
        if (r != null) r.nodeDetached(this);
        this.root = null;
        painted = false;
    }

    protected void onAttach() { }
    protected void onDetach() { }

    /** Depth-first search for the first node matching {@code id}. */
    public Node findById(String id) {
        if (id.equals(this.id)) return this;
        for (Node c : children) {
            Node f = c.findById(id);
            if (f != null) return f;
        }
        return null;
    }

    public boolean isDescendantOf(Node ancestor) {
        for (Node n = this; n != null; n = n.parent) if (n == ancestor) return true;
        return false;
    }

    // ================================================================== dirty tracking

    public void markStyleDirty() {
        styleDirty = true;
        if (root != null) root.requestFrame();
    }

    public void markSubtreeStyleDirty() {
        styleDirty = true;
        for (Node c : children) c.markSubtreeStyleDirty();
        if (root != null) root.requestFrame();
    }

    public void markLayoutDirty() {
        for (Node n = this; n != null; n = n.parent) {
            n.layoutDirty = true;
            n.cache.clear();
        }
        if (root != null) root.requestFrame();
    }

    // ================================================================== per-frame update

    /** Resolves styles for this subtree. {@code parentChanged} = an inherited value of the parent changed. */
    void updateStyles(List<me.methodial.apothecary.sheet.Sheet> sheets, float now, float rootW, float rootH, boolean parentChanged) {
        boolean restyled = false;
        if (styleDirty || target == null) {
            ComputedStyle next = StyleResolver.cascade(this, sheets, rootW, rootH);
            if (target != null) StyleResolver.startTransitions(this, target, next, current, now);
            StyleResolver.syncKeyframes(this, next, now);
            target = next;
            styleDirty = false;
            restyled = true;
        }
        boolean inheritedChanged = false;
        if (restyled || parentChanged || currentDirty || isAnimating()) {
            ComputedStyle prev = current;
            ComputedStyle cur = new ComputedStyle(target);
            if (parent != null && parent.current != null) {
                for (Prop<?> p : Props.all()) {
                    if (p.inherited() && !target.isSpecified(p)) cur.setRaw(p.id(), parent.current.raw(p.id()));
                }
            }
            StyleResolver.sampleKeyframes(this, cur, cur, now);
            StyleResolver.sampleTransitions(this, cur, now);
            StyleResolver.sampleTweens(this, cur, now);
            current = cur;
            currentDirty = false;
            if (prev == null) {
                markLayoutDirty();
                inheritedChanged = true;
            } else {
                for (Prop<?> p : Props.all()) {
                    if (!prev.sameAs(cur, p.id())) {
                        if (p.affects() == Prop.Affects.LAYOUT) markLayoutDirty();
                        if (p.inherited()) inheritedChanged = true;
                        onStyleChanged(p);
                    }
                }
            }
        }
        for (int i = 0; i < children.size(); i++) children.get(i).updateStyles(sheets, now, rootW, rootH, inheritedChanged);
    }

    /** Called when a displayed style value changes (subclasses may cache derived data). */
    protected void onStyleChanged(Prop<?> p) { }

    /** Per-frame hook, called before layout. */
    protected void onFrame(float dt) { }

    void frame(float dt, float now) {
        onFrame(dt);
        updateScroll(dt);
        updateFlip(now);
        for (int i = 0; i < children.size(); i++) children.get(i).frame(dt, now);
    }

    // ================================================================== layout hooks & results

    /**
     * Intrinsic content size of a leaf for the given inner constraints (NaN = unconstrained).
     * Return null for container nodes (children are laid out instead).
     */
    protected float[] measureContent(float availWidth, float availHeight) { return null; }

    /** True when this node sizes from {@link #measureContent} instead of children. */
    public boolean isLeaf() { return false; }

    /** Position relative to the parent's border box (before transforms and scrolling). */
    public float x() { return x; }
    public float y() { return y; }
    public float width() { return width; }
    public float height() { return height; }

    public Rect bounds() { return new Rect(x, y, width, height); }

    /** Border box in root coordinates (bounding box when transformed). Valid after the first paint. */
    public Rect globalBounds() { return globalMatrix.mapBounds(new Rect(0, 0, width, height)); }

    public Affine globalMatrix() { return globalMatrix; }

    /** Converts a root-space point to this node's local border-box space. */
    public float[] toLocal(float rx, float ry) {
        return new float[]{inverseMatrix.mapX(rx, ry), inverseMatrix.mapY(rx, ry)};
    }

    public Edges borderEdges() { return current == null ? Edges.ZERO : current.border(); }

    public Edges paddingEdges() { return current == null ? Edges.ZERO : current.padding(parent != null ? parent.width : width); }

    /** Content box in local coordinates. */
    public Rect contentBox() {
        Edges b = borderEdges(), p = paddingEdges();
        return new Rect(b.left() + p.left(), b.top() + p.top(),
                Math.max(0, width - b.horizontal() - p.horizontal()), Math.max(0, height - b.vertical() - p.vertical()));
    }

    /** Called by the layout engine. Triggers FLIP animation when {@code layout-transition} is set. */
    void setLayoutPosition(float nx, float ny) {
        if (laidOut && (nx != x || ny != y) && current != null && !current.get(Props.LAYOUT_TRANSITION).isEmpty() && root != null) {
            TransitionSpec spec = current.get(Props.LAYOUT_TRANSITION).get(0);
            flipStartX = layoutOffsetX + (x - nx);
            flipStartY = layoutOffsetY + (y - ny);
            layoutOffsetX = flipStartX;
            layoutOffsetY = flipStartY;
            flipStart = root.time();
            flipDuration = Math.max(0.01f, spec.duration());
            flipEasing = spec.easing();
        }
        x = nx;
        y = ny;
        laidOut = true;
    }

    void setLayoutSize(float w, float h) {
        width = w;
        height = h;
    }

    private void updateFlip(float now) {
        if (flipDuration <= 0) return;
        float t = (now - flipStart) / flipDuration;
        if (t >= 1) {
            layoutOffsetX = layoutOffsetY = 0;
            flipDuration = 0;
            return;
        }
        float e = 1 - flipEasing.apply(t);
        layoutOffsetX = flipStartX * e;
        layoutOffsetY = flipStartY * e;
    }

    /** Called after this node's children were laid out (subclasses may react to their new size). */
    protected void onLayout() { }

    // ================================================================== scrolling

    public boolean scrollsX() { return current != null && current.get(Props.OVERFLOW_X) == Overflow.SCROLL; }
    public boolean scrollsY() { return current != null && current.get(Props.OVERFLOW_Y) == Overflow.SCROLL; }
    public boolean clipsContent() {
        return current != null && (current.get(Props.OVERFLOW_X) != Overflow.VISIBLE || current.get(Props.OVERFLOW_Y) != Overflow.VISIBLE);
    }

    public float scrollX() { return scrollX; }
    public float scrollY() { return scrollY; }
    /** contentW/H = border-box size that would show all content, so overflow = content - size. */
    public float maxScrollX() { return Math.max(0, contentW - width); }
    public float maxScrollY() { return Math.max(0, contentH - height); }

    /** Scrolls smoothly (per scroll-smoothness) to the given offsets, clamped. */
    public void scrollTo(float sx, float sy) {
        flingX = flingY = 0;
        scrollTargetX = Math.max(0, Math.min(maxScrollX(), sx));
        scrollTargetY = Math.max(0, Math.min(maxScrollY(), sy));
        if (root != null) root.requestFrame();
    }

    public void scrollBy(float dx, float dy) { scrollTo(scrollTargetX + dx, scrollTargetY + dy); }

    /**
     * Scrolls by about {@code dx}, {@code dy} with momentum, as a wheel notch does: the list glides there and eases
     * out. Notches in quick succession build on the glide, up to twice as far each, so a fast spin covers ground;
     * one against the glide stops it first. Without scroll-inertia this is {@link #scrollBy}.
     */
    public void fling(float dx, float dy) {
        float inertia = get(Props.SCROLL_INERTIA);
        if (inertia <= 0) { scrollBy(dx, dy); return; }
        flingX = push(flingX, dx / inertia);
        flingY = push(flingY, dy / inertia);
        if (root != null) root.requestFrame();
    }

    private static float push(float velocity, float impulse) {
        if (impulse == 0) return velocity;
        if (Math.signum(velocity) != Math.signum(impulse)) return impulse;
        return velocity + impulse * (1 + Math.min(1, Math.abs(velocity / impulse) / 4));
    }

    /** Whether the scroll position is still moving: gliding, or easing towards where it was sent. */
    public boolean scrolling() {
        return flingX != 0 || flingY != 0 || scrollX != scrollTargetX || scrollY != scrollTargetY;
    }

    /** Jumps without animation. */
    public void setScroll(float sx, float sy) {
        scrollTo(sx, sy); // also ends any glide
        scrollX = scrollTargetX;
        scrollY = scrollTargetY;
    }

    private void updateScroll(float dt) {
        if (!scrollsX() && !scrollsY()) { scrollX = scrollY = scrollTargetX = scrollTargetY = flingX = flingY = 0; return; }
        if (flingX != 0 || flingY != 0) {
            // The glide moves the target; it stops at an edge and fades once too slow to see.
            float maxX = maxScrollX(), maxY = maxScrollY();
            scrollTargetX += flingX * dt;
            scrollTargetY += flingY * dt;
            if (scrollTargetX <= 0 || scrollTargetX >= maxX) flingX = 0;
            if (scrollTargetY <= 0 || scrollTargetY >= maxY) flingY = 0;
            float decay = (float) Math.exp(-dt / Math.max(1e-3f, get(Props.SCROLL_INERTIA)));
            flingX *= decay;
            flingY *= decay;
            if (Math.abs(flingX) < 4) flingX = 0;
            if (Math.abs(flingY) < 4) flingY = 0;
        }
        scrollTargetX = Math.max(0, Math.min(maxScrollX(), scrollTargetX));
        scrollTargetY = Math.max(0, Math.min(maxScrollY(), scrollTargetY));
        float smooth = get(Props.SCROLL_SMOOTHNESS);
        if (smooth <= 0) { scrollX = scrollTargetX; scrollY = scrollTargetY; return; }
        float k = 1 - (float) Math.exp(-dt / (smooth / 3f));
        scrollX += (scrollTargetX - scrollX) * k;
        scrollY += (scrollTargetY - scrollY) * k;
        if (Math.abs(scrollX - scrollTargetX) < 0.05f) scrollX = scrollTargetX;
        if (Math.abs(scrollY - scrollTargetY) < 0.05f) scrollY = scrollTargetY;
    }

    /** Scrolls ancestors so this node is visible. */
    public void scrollIntoView() {
        for (Node a = parent; a != null; a = a.parent) {
            if (!a.scrollsY() && !a.scrollsX()) continue;
            float top = 0, left = 0;
            for (Node n = this; n != a && n != null; n = n.parent) { top += n.y; left += n.x; }
            float viewH = a.height - a.borderEdges().vertical(), viewW = a.width - a.borderEdges().horizontal();
            float ty = a.scrollTargetY, tx = a.scrollTargetX;
            if (top < ty) ty = top;
            else if (top + height > ty + viewH) ty = top + height - viewH;
            if (left < tx) tx = left;
            else if (left + width > tx + viewW) tx = left + width - viewW;
            a.scrollTo(tx, ty);
            return;
        }
    }

    /** Scrollbar geometry {x, y, w, h} of the vertical thumb in local space, or null. */
    float[] verticalThumb() {
        if (!scrollsY() || maxScrollY() <= 0.5f) return null;
        Edges b = borderEdges();
        float sw = get(Props.SCROLLBAR_WIDTH);
        if (sw <= 0) return null;
        float trackH = height - b.vertical() - 4;
        float viewH = height - b.vertical();
        float thumbH = Math.max(12, trackH * viewH / (viewH + maxScrollY()));
        float ty = b.top() + 2 + (trackH - thumbH) * (scrollY / maxScrollY());
        return new float[]{width - b.right() - sw - 1.5f, ty, sw, thumbH};
    }

    float[] horizontalThumb() {
        if (!scrollsX() || maxScrollX() <= 0.5f) return null;
        Edges b = borderEdges();
        float sw = get(Props.SCROLLBAR_WIDTH);
        if (sw <= 0) return null;
        float trackW = width - b.horizontal() - 4;
        float viewW = width - b.horizontal();
        float thumbW = Math.max(12, trackW * viewW / (viewW + maxScrollX()));
        float tx = b.left() + 2 + (trackW - thumbW) * (scrollX / maxScrollX());
        return new float[]{tx, height - b.bottom() - sw - 1.5f, thumbW, sw};
    }

    // ================================================================== painting

    /** Visible = displayed, not visibility:hidden and not fully transparent. */
    public boolean isRendered() {
        return current != null && current.display() != Display.NONE && current.get(Props.VISIBILITY) == Visibility.VISIBLE;
    }

    void paint(Canvas c) {
        painted = false;
        if (current == null || current.display() == Display.NONE) return;
        float opacity = current.opacity();
        if (current.get(Props.VISIBILITY) != Visibility.VISIBLE || opacity <= 0.001f) return;

        c.save();
        float sx = parent != null ? parent.scrollX : 0, sy = parent != null ? parent.scrollY : 0;
        c.translate(x + layoutOffsetX - sx, y + layoutOffsetY - sy);
        Transform tf = current.get(Props.TRANSFORM);
        if (!tf.isIdentity()) {
            TransformOrigin o = current.get(Props.TRANSFORM_ORIGIN);
            c.transform(tf.toAffine(width, height, o.resolveX(width), o.resolveY(height)));
        }
        globalMatrix = c.matrix();
        inverseMatrix = globalMatrix.invert();
        painted = true;
        c.pushOpacity(opacity);

        Rect box = new Rect(0, 0, width, height);
        Rect clip = c.clipBounds();
        boolean culled = clip != null && globalMatrix.mapBounds(box.outset(32)).intersect(clip).isEmpty() && !overflowsVisibly();
        if (!culled) {
            paintBackground(c, box);
            paintContent(c, contentBox());
        }
        paintChildren(c);
        if (!culled) {
            paintScrollbars(c);
            paintOutline(c, box);
            paintOverlay(c);
        }
        c.popOpacity();
        c.restore();
    }

    /** Children may stick out of a box that doesn't clip, so such boxes are never culled by their own bounds. */
    private boolean overflowsVisibly() { return !clipsContent() && !children.isEmpty(); }

    protected void paintBackground(Canvas c, Rect box) {
        Corners radii = current.radius().fit(width, height);
        List<Shadow> shadows = current.shadows();
        for (Shadow s : shadows) if (!s.inset() && !s.color().isTransparent()) c.drawShadow(box, radii, s);

        Paint bg = current.background();
        Edges b = current.border();
        Color bc = current.get(Props.BORDER_COLOR);
        boolean uniform = b.top() == b.right() && b.right() == b.bottom() && b.bottom() == b.left();
        if (uniform) {
            if (bg.isVisible() || (b.top() > 0 && !bc.isTransparent())) c.drawBox(box, radii, bg, b.top(), bc);
        } else {
            if (bg.isVisible()) c.drawBox(box, radii, bg, 0, Color.TRANSPARENT);
            if (!bc.isTransparent()) {
                if (b.top() > 0) c.fillRect(new Rect(0, 0, width, b.top()), bc);
                if (b.bottom() > 0) c.fillRect(new Rect(0, height - b.bottom(), width, b.bottom()), bc);
                if (b.left() > 0) c.fillRect(new Rect(0, b.top(), b.left(), height - b.vertical()), bc);
                if (b.right() > 0) c.fillRect(new Rect(width - b.right(), b.top(), b.right(), height - b.vertical()), bc);
            }
        }
        for (Shadow s : shadows) if (s.inset() && !s.color().isTransparent()) c.drawShadow(box, radii, s);
    }

    /** Draws this node's own content (text, image...) inside {@code content}. */
    protected void paintContent(Canvas c, Rect content) { }

    /** Drawn after children (and scrollbars). */
    protected void paintOverlay(Canvas c) { }

    protected void paintChildren(Canvas c) {
        if (children.isEmpty()) return;
        boolean clip = clipsContent();
        if (clip) {
            Edges b = borderEdges();
            c.pushClip(new Rect(b.left(), b.top(), width - b.horizontal(), height - b.vertical()));
        }
        for (Node child : paintOrder()) child.paint(c);
        if (clip) c.popClip();
    }

    /** Children sorted by z-index (stable). */
    protected List<Node> paintOrder() {
        boolean anyZ = false;
        for (Node ch : children) if (ch.get(Props.Z_INDEX) != 0) { anyZ = true; break; }
        if (!anyZ) return children;
        List<Node> sorted = new ArrayList<>(children);
        sorted.sort((a, b) -> Integer.compare(a.get(Props.Z_INDEX), b.get(Props.Z_INDEX)));
        return sorted;
    }

    private void paintScrollbars(Canvas c) {
        float[] v = verticalThumb(), h = horizontalThumb();
        if (v == null && h == null) return;
        Color thumb = get(Props.SCROLLBAR_COLOR), track = get(Props.SCROLLBAR_TRACK_COLOR);
        boolean hot = root != null && (root.scrollbarDragNode() == this || hasState("hover"));
        if (get(Props.SCROLLBAR_BLOCKY)) {
            if (v != null) paintBlockyScrollbar(c, new Rect(v[0], borderEdges().top() + 2, v[2], height - borderEdges().vertical() - 4),
                    new Rect(v[0], v[1], v[2], v[3]), thumb, track, hot);
            if (h != null) paintBlockyScrollbar(c, new Rect(borderEdges().left() + 2, h[1], width - borderEdges().horizontal() - 4, h[3]),
                    new Rect(h[0], h[1], h[2], h[3]), thumb, track, hot);
            return;
        }
        Color tc = hot ? thumb : thumb.multiplyAlpha(0.7f);
        if (v != null) {
            if (!track.isTransparent()) c.fillRoundRect(new Rect(v[0], borderEdges().top() + 2, v[2], height - borderEdges().vertical() - 4), v[2] / 2, Paint.of(track));
            c.fillRoundRect(new Rect(v[0], v[1], v[2], v[3]), v[2] / 2, Paint.of(tc));
        }
        if (h != null) {
            if (!track.isTransparent()) c.fillRoundRect(new Rect(borderEdges().left() + 2, h[1], width - borderEdges().horizontal() - 4, h[3]), h[3] / 2, Paint.of(track));
            c.fillRoundRect(new Rect(h[0], h[1], h[2], h[3]), h[3] / 2, Paint.of(tc));
        }
    }

    /**
     * A blocky scrollbar: a groove of {@code track} and the thumb as a raised block, framed in near-black, a light line
     * along its top and left, a dark one along its bottom and right; lighter when hovered or dragged.
     */
    private static void paintBlockyScrollbar(Canvas c, Rect groove, Rect thumbBox, Color thumb, Color track, boolean hot) {
        if (!track.isTransparent()) c.fillRect(groove, track);
        Color face = hot ? thumb.lerp(Color.WHITE, 0.12f) : thumb;
        c.fillRect(thumbBox.outset(1), Color.argb(255, 30, 30, 31));
        c.fillRect(thumbBox, face);
        Color light = Color.argb(70, 255, 255, 255), dark = Color.argb(90, 0, 0, 0);
        c.fillRect(new Rect(thumbBox.x(), thumbBox.y(), thumbBox.width(), 1), light);
        c.fillRect(new Rect(thumbBox.x(), thumbBox.y(), 1, thumbBox.height()), light);
        c.fillRect(new Rect(thumbBox.x(), thumbBox.bottom() - 1, thumbBox.width(), 1), dark);
        c.fillRect(new Rect(thumbBox.right() - 1, thumbBox.y(), 1, thumbBox.height()), dark);
    }

    private void paintOutline(Canvas c, Rect box) {
        float ow = current.get(Props.OUTLINE_WIDTH);
        Color oc = current.get(Props.OUTLINE_COLOR);
        if (ow <= 0 || oc.isTransparent()) return;
        float off = current.get(Props.OUTLINE_OFFSET);
        Rect r = box.outset(off + ow);
        Corners radii = current.radius();
        // Rounded corners grow with the outline; square ones stay square, so blocky designs keep hard pixel corners.
        float grow = off + ow;
        Corners outer = new Corners(round(radii.topLeft(), grow), round(radii.topRight(), grow),
                round(radii.bottomRight(), grow), round(radii.bottomLeft(), grow));
        c.drawBox(r, outer.fit(r.width(), r.height()), Paint.NONE, ow, oc);
    }

    private static float round(float radius, float grow) { return radius > 0 ? radius + grow : 0; }

    // ================================================================== hit testing

    /** Deepest node under the root-space point, honoring transforms, clipping, z-order and pointer-events. */
    public Node hitTest(float rx, float ry) {
        if (!painted || !isRendered()) return null;
        float lx = inverseMatrix.mapX(rx, ry), ly = inverseMatrix.mapY(rx, ry);
        boolean inside = lx >= 0 && ly >= 0 && lx < width && ly < height;
        if (clipsContent() && !inside) return null;
        List<Node> order = paintOrder();
        for (int i = order.size() - 1; i >= 0; i--) {
            Node h = order.get(i).hitTest(rx, ry);
            if (h != null) return h;
        }
        if (inside && get(Props.POINTER_EVENTS) != PointerEvents.NONE) return this;
        return null;
    }

    public boolean containsPoint(float rx, float ry) {
        float lx = inverseMatrix.mapX(rx, ry), ly = inverseMatrix.mapY(rx, ry);
        return lx >= 0 && ly >= 0 && lx < width && ly < height;
    }

    // ================================================================== events

    @SuppressWarnings("unchecked")
    public <E extends UIEvent> Node on(EventType<E> type, Consumer<? super E> handler) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add((Consumer<? super UIEvent>) handler);
        return this;
    }

    public <E extends UIEvent> Node off(EventType<E> type) {
        listeners.remove(type);
        return this;
    }

    public Node onClick(Consumer<MouseEvent> handler) { return on(EventType.CLICK, handler); }

    public Node onClick(Runnable handler) { return on(EventType.CLICK, e -> handler.run()); }

    /** Delivers an event to this node's listeners only. */
    public <E extends UIEvent> void dispatchLocal(EventType<E> type, E event) {
        List<Consumer<? super UIEvent>> ls = listeners.get(type);
        if (ls == null) return;
        event.dispatching(event.target() == null ? this : event.target(), this);
        if (event instanceof MouseEvent me) {
            me.setLocal(inverseMatrix.mapX(me.x(), me.y()), inverseMatrix.mapY(me.x(), me.y()));
        }
        for (Consumer<? super UIEvent> l : new ArrayList<>(ls)) l.accept(event);
    }

    /** Dispatches from this node up through its ancestors (if the type bubbles). */
    public <E extends UIEvent> E emit(EventType<E> type, E event) {
        event.dispatching(this, this);
        for (Node n = this; n != null; n = n.parent) {
            n.dispatchLocal(type, event);
            if (event.isPropagationStopped() || !type.bubbles()) break;
        }
        return event;
    }

    public boolean hasListener(EventType<?> type) {
        List<?> l = listeners.get(type);
        return l != null && !l.isEmpty();
    }

    // ================================================================== focus, disabled, tooltip

    public Node focusable(boolean f) { this.focusable = f; return this; }
    public boolean isFocusable() { return focusable && !isDisabled() && isRendered(); }
    public boolean isFocused() { return hasState("focus"); }

    public void focus() { if (root != null) root.setFocus(this); }

    public void blur() { if (root != null && root.focused() == this) root.setFocus(null); }

    /** Disables this element (and, for interaction, its subtree). Styles can target {@code :disabled}. */
    public Node disabled(boolean d) {
        this.disabled = d;
        state("disabled", d);
        return this;
    }

    public boolean isDisabled() {
        for (Node n = this; n != null; n = n.parent) if (n.disabled) return true;
        return false;
    }

    /** A plain-text tooltip shown on hover. */
    public Node tooltip(String text) {
        this.tooltip = () -> new Node("tooltip-text").add(new me.methodial.apothecary.widget.Text(text));
        return this;
    }

    /** A custom tooltip built when shown. */
    public Node tooltip(Supplier<Node> content) { this.tooltip = content; return this; }

    public Supplier<Node> tooltipSupplier() { return tooltip; }

    /**
     * Keeps keyboard focus inside this subtree while it is in the tree: Tab and the arrow keys stay in it, focus
     * moves into it when it appears and goes back where it was when it leaves. For dialogs drawn as part of the
     * content rather than through {@link UIRoot#openModal}.
     */
    public Node focusScope(boolean scope) { this.focusScope = scope; return this; }
    public boolean isFocusScope() { return focusScope; }

    /**
     * What a screen reader (Minecraft's narrator) says for this element. Needed for elements whose text does not
     * name them, such as a text field whose label sits beside it; others derive it, see {@link #accessibleName()}.
     */
    public Node accessibleName(String name) { this.accessibleName = name; return this; }

    /** The given accessible name, else the text inside this element, else its tooltip's text; may be empty. */
    public String accessibleName() {
        if (accessibleName != null) return accessibleName;
        String text = textOf(this);
        if (!text.isEmpty() || tooltip == null) return text;
        return textOf(tooltip.get());
    }

    private static String textOf(Node n) {
        StringBuilder out = new StringBuilder();
        java.util.ArrayDeque<Node> queue = new java.util.ArrayDeque<>(List.of(n));
        while (!queue.isEmpty()) {
            Node c = queue.poll();
            if (c instanceof me.methodial.apothecary.widget.Text t && !t.text().isBlank()) {
                if (!out.isEmpty()) out.append(", ");
                out.append(t.text().strip());
            }
            queue.addAll(c.children());
        }
        return out.toString();
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder(type);
        if (id != null) sb.append('#').append(id);
        for (String c : classes) sb.append('.').append(c);
        return sb.toString();
    }
}
