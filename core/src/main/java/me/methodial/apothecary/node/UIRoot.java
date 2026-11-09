package me.methodial.apothecary.node;

import me.methodial.apothecary.anim.Animator;
import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.theme.Theme;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.event.CharEvent;
import me.methodial.apothecary.event.EventType;
import me.methodial.apothecary.event.KeyEvent;
import me.methodial.apothecary.event.MouseEvent;
import me.methodial.apothecary.event.ScrollEvent;
import me.methodial.apothecary.event.UIEvent;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.style.Cursor;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.PointerEvents;
import me.methodial.apothecary.style.Justify;
import me.methodial.apothecary.style.Align;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * A live UI tree bound to a {@link Platform}: owns sheets and themes, the animator, input routing, focus,
 * hover/active states, tooltips and overlay layers (popups, modals, toasts).
 * <p>A backend calls {@link #resize}, the input methods, and once per frame {@link #render}.
 */
public class UIRoot {
    /** Where a popup goes relative to its anchor. */
    public enum Placement { BELOW, ABOVE, RIGHT, LEFT, BELOW_END, CURSOR }

    private static final class Overlay {
        final Node node;
        final Node anchor;
        final Placement placement;
        final Runnable onClose;
        final boolean modal;
        /** Where the press that opened it was, for {@link Placement#CURSOR}: later presses don't move it. */
        final float atX, atY;

        Overlay(Node node, Node anchor, Placement placement, Runnable onClose, boolean modal, float atX, float atY) {
            this.node = node;
            this.anchor = anchor;
            this.placement = placement;
            this.onClose = onClose;
            this.modal = modal;
            this.atX = atX;
            this.atY = atY;
        }
    }

    private final Platform platform;
    private final Animator animator = new Animator();
    private final List<Sheet> sheets = new ArrayList<>();
    private final int[] sheetVersions = new int[64];
    private final Node rootNode = new Node("root");
    private final Node contentLayer = new Node("layer").cls("content-layer");
    private final Node popupLayer = new Node("layer").cls("popup-layer");
    private final Node modalLayer = new Node("layer").cls("modal-layer");
    private final Node toastLayer = new Node("layer").cls("toast-layer");
    private final Node tooltipLayer = new Node("layer").cls("tooltip-layer");
    private final List<Overlay> overlays = new ArrayList<>();
    private float width, height;
    private float time;
    private BiConsumer<Node, String> warnings = (n, m) -> System.err.println("[apothecary] " + n + ": " + m);

    // input state
    private float mouseX = -1, mouseY = -1;
    private final List<Node> hoverChain = new ArrayList<>();
    private Node pressed, dragTarget;
    private int pressedButton = -1, pressMods;
    private float pressX, pressY;
    private boolean dragging;
    private Node focused;
    private Cursor cursor = Cursor.DEFAULT;
    private float lastClickTime = -10, lastClickX, lastClickY;
    private int clickCount;
    private Node scrollbarDrag;
    private boolean scrollbarVertical;
    private float scrollbarGrab;

    // tooltip state
    private Node tooltipOwner, tooltipShown;
    private float tooltipHoverStart;
    private float tooltipDelay = 0.45f;

    public UIRoot(Platform platform) {
        this.platform = platform;
        Props.init();
        // Layer structure is declared in code as base styles: lowest priority, so sheets can still
        // restyle it (e.g. move the toast stack), and clearing inline styles can't break it.
        for (Node l : List.of(contentLayer, popupLayer, modalLayer, toastLayer, tooltipLayer)) {
            l.baseStyle(s -> s.cover().pointerEvents(PointerEvents.NONE));
        }
        contentLayer.baseStyle(s -> s.column().pointerEvents(PointerEvents.AUTO));
        toastLayer.baseStyle(s -> s.column().justify(Justify.END).alignItems(Align.END).pad(8).gap(6));
        // The root only holds the layers: a click on nothing in them reaches the host (a game screen behind the UI).
        rootNode.baseStyle(s -> s.pointerEvents(PointerEvents.NONE));
        rootNode.add(contentLayer, popupLayer, modalLayer, toastLayer, tooltipLayer);
        rootNode.attach(this);
    }

    // ================================================================== accessors

    public Platform platform() { return platform; }
    public Animator animator() { return animator; }
    public float time() { return time; }
    public float width() { return width; }
    public float height() { return height; }
    public float mouseX() { return mouseX; }
    public float mouseY() { return mouseY; }

    /** The node user content goes into (a full-size flex column). */
    public Node content() { return contentLayer; }

    public UIRoot setContent(Node n) {
        contentLayer.setChildren(n);
        return this;
    }

    public Node rootNode() { return rootNode; }
    public Node toastLayer() { return toastLayer; }

    public void onWarning(BiConsumer<Node, String> w) { this.warnings = w; }

    void warn(Node n, String msg) { warnings.accept(n, msg); }

    void requestFrame() { }

    public void setTooltipDelay(float seconds) { this.tooltipDelay = seconds; }

    // ================================================================== sheets & themes

    /** Adds a sheet; later sheets win over earlier ones at equal specificity. */
    public UIRoot addSheet(Sheet s) {
        if (!sheets.contains(s)) sheets.add(s);
        restyleAll();
        return this;
    }

    public UIRoot removeSheet(Sheet s) {
        sheets.remove(s);
        restyleAll();
        return this;
    }

    public List<Sheet> sheets() { return Collections.unmodifiableList(sheets); }

    public void restyleAll() { rootNode.markSubtreeStyleDirty(); }

    /** Applies a theme (token values) to the whole UI; changed colors animate where transitions are set. */
    public UIRoot theme(Theme theme) {
        rootNode.theme(theme);
        return this;
    }

    /** Overrides one design token UI-wide: {@code ui.token(Tokens.ACCENT, Color.hex("#ff5c8a"))}. */
    public <T> UIRoot token(Token<T> token, T value) {
        rootNode.token(token, value);
        return this;
    }

    /** A token's current value at the root. */
    public <T> T token(Token<T> token) { return rootNode.token(token); }

    // ================================================================== frame

    public void resize(float w, float h) {
        if (w == width && h == height) return;
        width = w;
        height = h;
        rootNode.markLayoutDirty();
        restyleAll(); // media rules
    }

    /** Advances time by {@code dt} seconds, restyles, lays out and paints. */
    public void render(Canvas canvas, float dt) {
        update(dt);
        rootNode.paint(canvas);
    }

    /** Everything except painting (used by tests and headless layout). */
    public void update(float dt) {
        settleFocus();
        dt = Math.max(0, Math.min(dt, 0.25f));
        time += dt;
        for (int i = 0; i < sheets.size() && i < sheetVersions.length; i++) {
            if (sheets.get(i).version() != sheetVersions[i]) {
                sheetVersions[i] = sheets.get(i).version();
                restyleAll();
            }
        }
        animator.tick(dt);
        Length.setViewport(width, height);
        rootNode.frame(dt, time);
        positionOverlays();
        rootNode.updateStyles(sheets, time, width, height, false);
        if (rootNode.layoutDirty) Layout.layoutRoot(rootNode, width, height);
        // Overlays are positioned from the measured size of the popup, so lay out once more if they moved.
        if (positionOverlays()) {
            rootNode.updateStyles(sheets, time, width, height, false);
            if (rootNode.layoutDirty) Layout.layoutRoot(rootNode, width, height);
        }
        // Rebuilt elements are styled now, so focus can find them. A dialog that just closed hands focus back to
        // its opener, which outranks the rebuild's guess at "the same place" (the dialog's own element is gone).
        if (scopeClosing()) restorePath = null;
        applyFocusRestore();
        syncFocusScope();
        // A list that a rebuild took away for good ends its scrollbar drag.
        if (scrollbarDrag != null && !scrollbarDrag.isAttached()) scrollbarDrag = null;
        // Content can move under a still cursor (scrolling, animation, layout changes): re-hit-test
        // every frame so :hover, enter/leave and tooltips stay truthful. Uses last frame's paint.
        if (mouseX >= 0 && pressed == null && scrollbarDrag == null) updateHover();
        updateTooltip();
    }

    /** Lays out without advancing time (tests). */
    public void layoutNow() {
        Length.setViewport(width, height);
        rootNode.updateStyles(sheets, time, width, height, false);
        Layout.layoutRoot(rootNode, width, height);
        if (scopeClosing()) restorePath = null;
        applyFocusRestore();
        syncFocusScope();
    }

    // ================================================================== overlays

    /** Shows {@code popup} next to {@code anchor}; it closes on outside click or Escape. */
    public void showPopup(Node popup, Node anchor, Placement placement, Runnable onClose) {
        popup.baseStyle(s -> s.absolute().pointerEvents(PointerEvents.AUTO));
        popupLayer.add(popup);
        overlays.add(new Overlay(popup, anchor, placement, onClose, false, pressX, pressY));
        positionOverlays();
    }

    public boolean isOpen(Node overlay) {
        for (Overlay o : overlays) if (o.node == overlay) return true;
        return false;
    }

    /** Closes an overlay, playing its {@code :closing} style for {@code closeSeconds} first. */
    public void close(Node overlay) {
        Overlay found = null;
        for (Overlay o : overlays) if (o.node == overlay) found = o;
        if (found == null) return;
        overlays.remove(found);
        if (found.onClose != null) found.onClose.run();
        removeAnimated(overlay, 0.15f);
    }

    public void closeAllPopups() {
        for (Overlay o : new ArrayList<>(overlays)) if (!o.modal) close(o.node);
    }

    /** Opens a modal: a full-screen backdrop that blocks interaction with everything below. */
    public Node openModal(Node dialog, Runnable onClose) {
        Node backdrop = new Node("backdrop").cls("modal-backdrop");
        backdrop.baseStyle(s -> s.cover().row().center().pointerEvents(PointerEvents.AUTO));
        backdrop.add(dialog);
        backdrop.on(EventType.MOUSE_DOWN, e -> {
            if (e.target() == backdrop && backdrop.hasCls("dismissable")) close(backdrop);
        });
        modalLayer.add(backdrop);
        overlays.add(new Overlay(backdrop, null, null, onClose, true, 0, 0));
        Node f = firstFocusable(dialog);
        if (f != null) setFocus(f);
        return backdrop;
    }

    /** Closes the modal containing {@code node}. */
    public void closeModal(Node node) {
        for (Node n = node; n != null; n = n.parent()) {
            if (n.parent() == modalLayer) { close(n); return; }
        }
    }

    public boolean hasModal() {
        for (Overlay o : overlays) if (o.modal) return true;
        return false;
    }

    /** Adds a toast to the toast stack (bottom right by default). */
    public void toast(Node toast, float seconds) {
        toast.baseStyle(s -> s.pointerEvents(PointerEvents.AUTO));
        toastLayer.add(toast);
        animator.schedule(seconds, () -> removeAnimated(toast, 0.25f));
    }

    /**
     * Removes a node after letting it play its {@code :closing} state for {@code seconds}
     * (style it with e.g. {@code .toast:closing { opacity: 0; transform: translateX(20px) }} + a transition).
     */
    public void removeAnimated(Node n, float seconds) {
        if (n.hasState("closing")) return;
        n.state("closing", true);
        n.style().pointerEvents(PointerEvents.NONE);
        animator.schedule(seconds, n::removeFromParent);
    }

    private boolean positionOverlays() {
        boolean moved = false;
        for (Overlay o : overlays) {
            if (o.anchor == null || o.node.hasState("closing")) continue;
            Rect a = o.anchor.globalBounds();
            float pw = o.node.width(), ph = o.node.height();
            float x, y;
            switch (o.placement) {
                case ABOVE -> { x = a.x(); y = a.y() - ph - 2; }
                case RIGHT -> { x = a.right() + 2; y = a.y(); }
                case LEFT -> { x = a.x() - pw - 2; y = a.y(); }
                case BELOW_END -> { x = a.right() - pw; y = a.bottom() + 2; }
                case CURSOR -> { x = o.atX; y = o.atY; }
                default -> { x = a.x(); y = a.bottom() + 2; }
            }
            if (o.placement == Placement.BELOW && y + ph > height - 2 && a.y() - ph - 2 >= 2) y = a.y() - ph - 2;
            x = Math.max(2, Math.min(x, width - pw - 2));
            y = Math.max(2, Math.min(y, height - ph - 2));
            Object oldL = o.node.style().get(Props.LEFT), oldT = o.node.style().get(Props.TOP);
            Length nl = Length.px(Math.round(x)), nt = Length.px(Math.round(y));
            if (!nl.equals(oldL) || !nt.equals(oldT)) {
                o.node.style().set(Props.LEFT, nl);
                o.node.style().set(Props.TOP, nt);
                moved = true;
            }
        }
        return moved;
    }

    // ================================================================== tooltips

    private void updateTooltip() {
        Node owner = null;
        if (pressed == null) {
            for (int i = hoverChain.size() - 1; i >= 0; i--) {
                if (hoverChain.get(i).tooltipSupplier() != null) { owner = hoverChain.get(i); break; }
            }
        }
        if (owner != tooltipOwner) {
            tooltipOwner = owner;
            tooltipHoverStart = time;
            hideTooltip();
        }
        if (owner != null && tooltipShown == null && time - tooltipHoverStart >= tooltipDelay) {
            Supplier<Node> s = owner.tooltipSupplier();
            Node content = s.get();
            if (content == null) return;
            Node tip = new Node("tooltip").cls("tooltip").add(content);
            // A theme may draw a speech bubble from these: a cap on the right end and a stepped tail below it.
            tip.add(new Node().cls("tooltip-cap"));
            for (int i = 0; i < 3; i++) tip.add(new Node().cls("tooltip-step", "s" + i));
            tip.baseStyle(st -> st.absolute().pointerEvents(PointerEvents.NONE));
            tooltipLayer.add(tip);
            tooltipShown = tip;
        }
        if (tooltipShown != null) {
            float tw = tooltipShown.width(), th = tooltipShown.height();
            // Above the pointer, its tail (9 below the box, its tip 9 in from the right edge) pointing at it; below the
            // pointer, without the tail, where there is no room above.
            float x = mouseX - tw + 9, y = mouseY - th - 11;
            boolean below = y < 2;
            if (below) y = mouseY + 14;
            tooltipShown.toggleCls("below", below);
            if (x + tw + 3 > width - 2) x = width - 2 - tw - 3;
            tooltipShown.style().set(Props.LEFT, Length.px(Math.round(Math.max(2, x))));
            tooltipShown.style().set(Props.TOP, Length.px(Math.round(Math.max(2, y))));
        }
    }

    private void hideTooltip() {
        if (tooltipShown != null) {
            Node t = tooltipShown;
            tooltipShown = null;
            removeAnimated(t, 0.1f);
        }
    }

    // ================================================================== focus

    private boolean focusDetached;

    /** Drops focus from a node that left the tree, or re-marks focus-within if it was moved. */
    private void settleFocus() {
        if (!focusDetached) return;
        focusDetached = false;
        if (pressed != null && pressed.root() != this) pressed = null;
        if (dragTarget != null && dragTarget.root() != this) dragTarget = null;
        if (focused == null) return;
        if (focused.root() != this) {
            Node old = focused;
            focused = null;
            old.state("focus", false).state("focus-visible", false);
            old.dispatchLocal(EventType.BLUR, new UIEvent());
        } else {
            for (Node a = focused.parent(); a != null; a = a.parent()) a.state("focus-within", true);
        }
    }

    public Node focused() { settleFocus(); return focused; }

    public void setFocus(Node n) { setFocus(n, false); }

    public void setFocus(Node n, boolean keyboard) {
        settleFocus();
        if (n == focused) {
            if (n != null) n.state("focus-visible", keyboard);
            return;
        }
        Node old = focused;
        focused = n;
        if (old != null) {
            old.state("focus", false).state("focus-visible", false);
            for (Node a = old.parent(); a != null; a = a.parent()) a.state("focus-within", false);
            old.dispatchLocal(EventType.BLUR, new UIEvent());
        }
        if (n != null) {
            n.state("focus", true).state("focus-visible", keyboard);
            for (Node a = n.parent(); a != null; a = a.parent()) a.state("focus-within", true);
            n.dispatchLocal(EventType.FOCUS, new UIEvent());
            if (keyboard) n.scrollIntoView();
        }
        if (focusListener != null) focusListener.accept(n, keyboard);
    }

    private java.util.function.BiConsumer<Node, Boolean> focusListener;

    /**
     * Called whenever focus moves to another element (or to none), with whether the keyboard moved it. A platform
     * uses it to announce the focused element; there is one listener.
     */
    public void onFocusChange(java.util.function.BiConsumer<Node, Boolean> listener) { this.focusListener = listener; }

    private Node firstFocusable(Node n) {
        if (!n.isRendered()) return null;
        if (n.isFocusable()) return n;
        for (Node c : n.children()) {
            Node f = firstFocusable(c);
            if (f != null) return f;
        }
        return null;
    }

    private void collectFocusable(Node n, List<Node> out) {
        if (!n.isRendered()) return;
        if (n.isFocusable()) out.add(n);
        for (Node c : n.children()) collectFocusable(c, out);
    }

    /**
     * Where keyboard focus may go: the topmost modal overlay, else the innermost {@link Node#focusScope} in the
     * content (the last one in tree order, which draws on top), else the whole tree.
     */
    public Node focusScope() {
        for (int i = overlays.size() - 1; i >= 0; i--) if (overlays.get(i).modal) return overlays.get(i).node;
        Node scope = innermostScope(rootNode);
        return scope != null ? scope : rootNode;
    }

    private Node innermostScope(Node n) {
        if (!n.isRendered()) return null;
        Node found = n.isFocusScope() ? n : null;
        for (Node c : n.children()) {
            Node inner = innermostScope(c);
            if (inner != null) found = inner;
        }
        return found;
    }

    private static boolean within(Node n, Node ancestor) {
        for (Node a = n; a != null; a = a.parent()) if (a == ancestor) return true;
        return false;
    }

    public void focusNext(boolean backwards) {
        Node scope = focusScope();
        List<Node> list = new ArrayList<>();
        collectFocusable(scope, list);
        if (list.isEmpty()) return;
        int idx = focused == null ? -1 : list.indexOf(focused);
        int next = idx < 0 ? (backwards ? list.size() - 1 : 0) : (idx + (backwards ? -1 : 1) + list.size()) % list.size();
        setFocus(list.get(next), true);
    }

    /**
     * Moves keyboard focus to the nearest focusable element in a direction (dx, dy of -1, 0 or 1), within the
     * {@link #focusScope}: the candidate must lie on that side of the focused one's center, and the closest one
     * along the direction wins, with sideways distance counting double so a grid moves along its rows and
     * columns. With nothing focused, focuses the first element. Returns whether focus moved.
     */
    public boolean focusToward(int dx, int dy) {
        settleFocus();
        Node scope = focusScope();
        List<Node> list = new ArrayList<>();
        collectFocusable(scope, list);
        if (list.isEmpty()) return false;
        if (focused == null || !list.contains(focused)) {
            setFocus(list.get(0), true);
            return true;
        }
        var from = layoutBounds(focused);
        Node best = null;
        float bestScore = Float.MAX_VALUE;
        boolean bestInLine = false;
        for (Node c : list) {
            if (c == focused) continue;
            var b = layoutBounds(c);
            float ox = b.centerX() - from.centerX(), oy = b.centerY() - from.centerY();
            float along = dx != 0 ? ox * dx : oy * dy;
            if (along <= 0.5f) continue;
            // In line: it shares some of the focused element's extent across the direction (a wide search field
            // above a card, the catalog beside a sidebar entry). Those win over anything diagonal.
            boolean inLine = dx != 0 ? b.y() < from.bottom() && b.bottom() > from.y() : b.x() < from.right() && b.right() > from.x();
            float across = inLine ? 0 : Math.abs(dx != 0 ? oy : ox);
            float score = along + 2 * across;
            if (inLine && !bestInLine || inLine == bestInLine && score < bestScore) {
                bestScore = score;
                best = c;
                bestInLine = inLine;
            }
        }
        if (best == null) return false;
        setFocus(best, true);
        return true;
    }

    /** Where layout put {@code n} in root space (scrolling included, transforms ignored); needs no paint. */
    private static me.methodial.apothecary.geom.Rect layoutBounds(Node n) {
        float x = n.x(), y = n.y();
        for (Node p = n.parent(); p != null; p = p.parent()) {
            x += p.x() - p.scrollX();
            y += p.y() - p.scrollY();
        }
        return new me.methodial.apothecary.geom.Rect(x, y, n.width(), n.height());
    }

    // Focus that a scope took when it appeared, given back when it goes: a path from the content root, because the
    // element itself may have been rebuilt meanwhile.
    private boolean scopeActive;
    private int[] scopeReturnPath;
    private boolean scopeReturnKeyboard;

    private boolean scopeClosing() {
        if (!scopeActive || innermostScope(rootNode) != null) return false;
        for (Overlay o : overlays) if (o.modal) return false;
        return true;
    }

    /** Moves focus into a focus scope that appeared, and back to where it was when the scope is gone. */
    private void syncFocusScope() {
        settleFocus();
        Node scope = innermostScope(rootNode);
        boolean modal = false;
        for (Overlay o : overlays) modal |= o.modal;
        if (scope == null || modal) {
            if (scopeActive && !modal) {
                scopeActive = false;
                Node back = scopeReturnPath == null ? null : resolvePath(rootNode, scopeReturnPath);
                if (focused == null && back != null) {
                    Node target = back.isFocusable() ? back : firstFocusable(back);
                    if (target != null) setFocus(target, scopeReturnKeyboard);
                }
                scopeReturnPath = null;
            }
            return;
        }
        if (!scopeActive) {
            scopeActive = true;
            scopeReturnPath = focused == null ? null : pathOf(rootNode, focused);
            scopeReturnKeyboard = focused != null && focused.hasState("focus-visible");
        }
        if (focused == null || !within(focused, scope)) {
            Node first = firstFocusable(scope);
            if (first != null) setFocus(first, scopeReturnKeyboard);
        }
    }

    /** Child indices from {@code ancestor} down to {@code n}, or null when {@code n} is not inside it. */
    static int[] pathOf(Node ancestor, Node n) {
        List<Integer> path = new ArrayList<>();
        for (Node c = n; c != ancestor; c = c.parent()) {
            if (c == null || c.parent() == null) return null;
            path.add(0, c.parent().children().indexOf(c));
        }
        return path.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Follows {@code path} from {@code ancestor} as far as the tree still allows; the deepest node reached. */
    static Node resolvePath(Node ancestor, int[] path) {
        Node n = ancestor;
        for (int index : path) {
            if (index < 0 || index >= n.children().size()) break;
            n = n.children().get(index);
        }
        return n;
    }

    private Node restoreUnder;
    private int[] restorePath;
    private boolean restoreKeyboard;

    /**
     * After a rebuild removed the focused element: put focus back on the element now at {@code path} under
     * {@code ancestor} (or its first focusable), once the new elements are styled and laid out.
     */
    void queueFocusRestore(Node ancestor, int[] path, boolean keyboard) {
        restoreUnder = ancestor;
        restorePath = path;
        restoreKeyboard = keyboard;
    }

    private void applyFocusRestore() {
        if (restorePath == null) return;
        Node ancestor = restoreUnder;
        int[] path = restorePath;
        restorePath = null;
        restoreUnder = null;
        settleFocus();
        if (focused != null || ancestor.root() != this) return;
        Node at = resolvePath(ancestor, path);
        Node target = at.isFocusable() ? at : firstFocusable(at);
        // Confined to a scope only once it is active: a scope that just appeared first learns where focus was.
        if (target != null && (!scopeActive || within(target, focusScope()))) setFocus(target, restoreKeyboard);
    }

    // ================================================================== input

    /** Updates hover state for the pointer at (x, y) in root space. */
    public void mouseMove(float x, float y) {
        settleFocus();
        mouseX = x;
        mouseY = y;
        if (scrollbarDrag != null) {
            dragScrollbar();
            return;
        }
        updateHover();
        if (pressed != null) {
            float dx = x - pressX, dy = y - pressY;
            if (!dragging && dx * dx + dy * dy > 4) {
                dragging = true;
                dragTarget = findDragTarget(pressed);
                if (dragTarget != null) dragTarget.dispatchLocal(EventType.DRAG_START, new MouseEvent(x, y, pressedButton, 0, dx, dy));
            }
            if (dragging && dragTarget != null) dragTarget.dispatchLocal(EventType.DRAG, new MouseEvent(x, y, pressedButton, 0, dx, dy));
        }
        Node target = hoverChain.isEmpty() ? null : hoverChain.get(hoverChain.size() - 1);
        if (target != null) target.emit(EventType.MOUSE_MOVE, new MouseEvent(x, y, -1, 0, 0, 0));
    }

    private Node findDragTarget(Node n) {
        for (Node a = n; a != null; a = a.parent()) {
            if (a.hasListener(EventType.DRAG) || a.hasListener(EventType.DRAG_START)) return a;
        }
        return null;
    }

    private Node hit(float x, float y) {
        Node t = rootNode.hitTest(x, y);
        return t;
    }

    private void updateHover() {
        Node target = mouseX < 0 ? null : hit(mouseX, mouseY);
        List<Node> chain = new ArrayList<>();
        for (Node n = target; n != null; n = n.parent()) chain.add(0, n);
        // leave nodes no longer hovered (deepest first), enter new ones (outermost first)
        Set<Node> now = new HashSet<>(chain);
        for (int i = hoverChain.size() - 1; i >= 0; i--) {
            Node n = hoverChain.get(i);
            if (!now.contains(n)) {
                n.state("hover", false);
                n.dispatchLocal(EventType.MOUSE_LEAVE, new MouseEvent(mouseX, mouseY, -1, 0, 0, 0));
            }
        }
        Set<Node> before = new HashSet<>(hoverChain);
        for (Node n : chain) {
            if (!before.contains(n)) {
                n.state("hover", true);
                n.dispatchLocal(EventType.MOUSE_ENTER, new MouseEvent(mouseX, mouseY, -1, 0, 0, 0));
            }
        }
        hoverChain.clear();
        hoverChain.addAll(chain);
        Node cursorNode = pressed != null ? pressed : target;
        Cursor c = cursorNode == null ? Cursor.DEFAULT : cursorNode.get(Props.CURSOR);
        if (cursorNode != null && cursorNode.isDisabled()) c = Cursor.NOT_ALLOWED;
        if (scrollbarDrag != null) c = Cursor.DEFAULT;
        if (c != cursor) {
            cursor = c;
            platform.setCursor(c);
        }
    }

    /** The hovered node, deepest first. */
    public Node hovered() { return hoverChain.isEmpty() ? null : hoverChain.get(hoverChain.size() - 1); }

    Node scrollbarDragNode() { return scrollbarDrag; }

    public boolean mouseDown(float x, float y, int button) { return mouseDown(x, y, button, 0); }

    /** A press with the modifier keys held ({@link KeyEvent#MOD_SHIFT} etc), which shift-click selection reads. */
    public boolean mouseDown(float x, float y, int button, int mods) {
        settleFocus();
        mouseX = x;
        mouseY = y;
        updateHover();
        hideTooltip();
        tooltipHoverStart = Float.MAX_VALUE / 2;
        Node target = hovered();

        // Outside click closes popups (not when clicking the popup or its anchor).
        for (Overlay o : new ArrayList<>(overlays)) {
            if (o.modal) continue;
            boolean inside = target != null && (target.isDescendantOf(o.node) || (o.anchor != null && target.isDescendantOf(o.anchor)));
            if (!inside) close(o.node);
        }

        if (button == MouseEvent.LEFT && target != null && startScrollbarDrag(target, x, y)) return true;
        if (target == null) {
            setFocus(null);
            return false;
        }
        if (target.isDisabled()) return true;
        pressed = target;
        pressedButton = button;
        pressMods = mods;
        pressX = x;
        pressY = y;
        dragging = false;
        dragTarget = null;
        for (Node a = target; a != null; a = a.parent()) a.state("active", button == MouseEvent.LEFT);
        Node focusTarget = null;
        for (Node a = target; a != null; a = a.parent()) if (a.isFocusable()) { focusTarget = a; break; }
        setFocus(focusTarget);

        float dt = time - lastClickTime;
        if (dt < 0.35f && Math.abs(x - lastClickX) < 4 && Math.abs(y - lastClickY) < 4) clickCount++;
        else clickCount = 1;
        lastClickTime = time;
        lastClickX = x;
        lastClickY = y;
        MouseEvent e = target.emit(EventType.MOUSE_DOWN, new MouseEvent(x, y, button, clickCount, 0, 0, pressMods));
        return true;
    }

    public boolean mouseUp(float x, float y, int button) {
        settleFocus();
        mouseX = x;
        mouseY = y;
        if (scrollbarDrag != null) {
            scrollbarDrag = null;
            updateHover();
            return true;
        }
        Node p = pressed;
        if (p == null) return false;
        Node target = hit(x, y);
        if (target != null) target.emit(EventType.MOUSE_UP, new MouseEvent(x, y, button, clickCount, x - pressX, y - pressY));
        if (dragging && dragTarget != null) {
            dragTarget.dispatchLocal(EventType.DRAG_END, new MouseEvent(x, y, button, 0, x - pressX, y - pressY));
        }
        boolean suppressClick = dragging && dragTarget != null && (Math.abs(x - pressX) > 4 || Math.abs(y - pressY) > 4);
        for (Node a = p; a != null; a = a.parent()) a.state("active", false);
        pressed = null;
        dragging = false;
        dragTarget = null;
        if (!suppressClick && target != null && target.isDescendantOf(p) || (!suppressClick && target != null && p.isDescendantOf(target))) {
            if (!p.isDisabled()) {
                if (button == MouseEvent.LEFT) p.emit(EventType.CLICK, new MouseEvent(x, y, button, clickCount, 0, 0, pressMods));
                else if (button == MouseEvent.RIGHT) p.emit(EventType.CONTEXT_MENU, new MouseEvent(x, y, button, clickCount, 0, 0, pressMods));
            }
        }
        updateHover();
        return true;
    }

    public boolean scroll(float x, float y, float dx, float dy) {
        settleFocus();
        mouseX = x;
        mouseY = y;
        hideTooltip();
        tooltipHoverStart = time;   // restart the tooltip delay after scrolling
        Node target = hit(x, y);
        if (target == null) return false;
        ScrollEvent e = target.emit(EventType.SCROLL, new ScrollEvent(x, y, dx, dy));
        if (e.isDefaultPrevented()) return true;
        float step = 28;
        for (Node n = target; n != null; n = n.parent()) {
            boolean canY = n.scrollsY() && n.maxScrollY() > 0 && ((dy > 0 && n.scrollTargetY > 0) || (dy < 0 && n.scrollTargetY < n.maxScrollY()));
            boolean canX = n.scrollsX() && n.maxScrollX() > 0 && (dx != 0 || !n.scrollsY())
                    && (((dx != 0 ? dx : dy) > 0 && n.scrollTargetX > 0) || ((dx != 0 ? dx : dy) < 0 && n.scrollTargetX < n.maxScrollX()));
            if (canY && dy != 0) { n.fling(0, -dy * step); return true; }
            if (canX) { n.fling(-(dx != 0 ? dx : dy) * step, 0); return true; }
        }
        return false;
    }

    public boolean keyDown(int key, int scancode, int mods) {
        settleFocus();
        KeyEvent e = new KeyEvent(key, scancode, mods);
        Node target = focused != null ? focused : rootNode;
        target.emit(EventType.KEY_DOWN, e);
        if (e.isDefaultPrevented()) return true;
        if (key == KeyEvent.KEY_TAB) {
            focusNext(e.shift());
            return true;
        }
        if (key == KeyEvent.KEY_ESCAPE) {
            if (!overlays.isEmpty()) {
                close(overlays.get(overlays.size() - 1).node);
                return true;
            }
            // Escape leaves a text field (one press, the caret goes); anywhere else it is not ours, so the screen closes.
            if (focused instanceof me.methodial.apothecary.widget.TextField || focused instanceof me.methodial.apothecary.widget.TextArea) {
                setFocus(null);
                return true;
            }
        }
        // A disabled element ignores Enter and Space as it ignores the mouse.
        if ((key == KeyEvent.KEY_ENTER || key == KeyEvent.KEY_SPACE) && focused != null && !focused.isDisabled()
                && focused.hasListener(EventType.CLICK)) {
            focused.emit(EventType.CLICK, new MouseEvent(mouseX, mouseY, MouseEvent.LEFT, 1, 0, 0));
            return true;
        }
        // Arrows the focused element did not use (text carets, selects and sliders take theirs) move focus.
        int dx = key == KeyEvent.KEY_RIGHT ? 1 : key == KeyEvent.KEY_LEFT ? -1 : 0;
        int dy = key == KeyEvent.KEY_DOWN ? 1 : key == KeyEvent.KEY_UP ? -1 : 0;
        if ((dx != 0 || dy != 0) && !e.primary() && !e.alt()) return focusToward(dx, dy);
        return false;
    }

    public boolean keyUp(int key, int scancode, int mods) {
        settleFocus();
        KeyEvent e = new KeyEvent(key, scancode, mods);
        (focused != null ? focused : rootNode).emit(EventType.KEY_UP, e);
        return e.isDefaultPrevented();
    }

    public boolean charTyped(int codepoint, int mods) {
        settleFocus();
        if (focused == null) return false;
        CharEvent e = focused.emit(EventType.CHAR, new CharEvent(codepoint, mods));
        return e.isDefaultPrevented();
    }

    /** True when the UI wants keyboard input (a text field is focused etc). */
    public boolean wantsKeyboard() { settleFocus(); return focused != null; }

    // ================================================================== scrollbar dragging

    private boolean startScrollbarDrag(Node target, float x, float y) {
        for (Node n = target; n != null; n = n.parent()) {
            float[] v = n.verticalThumb(), h = n.horizontalThumb();
            if (v == null && h == null) continue;
            float[] l = n.toLocal(x, y);
            if (v != null && l[0] >= v[0] - 3 && l[0] <= n.width()) {
                scrollbarDrag = n;
                scrollbarVertical = true;
                boolean onThumb = l[1] >= v[1] && l[1] <= v[1] + v[3];
                scrollbarGrab = onThumb ? l[1] - v[1] : v[3] / 2;
                dragScrollbar();
                return true;
            }
            if (h != null && l[1] >= h[1] - 3 && l[1] <= n.height()) {
                scrollbarDrag = n;
                scrollbarVertical = false;
                boolean onThumb = l[0] >= h[0] && l[0] <= h[0] + h[2];
                scrollbarGrab = onThumb ? l[0] - h[0] : h[2] / 2;
                dragScrollbar();
                return true;
            }
        }
        return false;
    }

    private void dragScrollbar() {
        Node n = scrollbarDrag;
        // A rebuild detaches and re-attaches a kept list (see Region.keep), often mid-drag as the list reaches new
        // rows; only a list that stayed gone ends the drag.
        if (!n.isAttached()) {
            scrollbarDrag = null;
            return;
        }
        float[] l = n.toLocal(mouseX, mouseY);
        if (scrollbarVertical) {
            float[] v = n.verticalThumb();
            if (v == null) return;
            float trackTop = n.borderEdges().top() + 2, trackH = n.height() - n.borderEdges().vertical() - 4;
            float f = (l[1] - scrollbarGrab - trackTop) / Math.max(1, trackH - v[3]);
            n.setScroll(n.scrollX(), f * n.maxScrollY());
        } else {
            float[] h = n.horizontalThumb();
            if (h == null) return;
            float trackL = n.borderEdges().left() + 2, trackW = n.width() - n.borderEdges().horizontal() - 4;
            float f = (l[0] - scrollbarGrab - trackL) / Math.max(1, trackW - h[2]);
            n.setScroll(f * n.maxScrollX(), n.scrollY());
        }
    }

    // ================================================================== bookkeeping

    void nodeDetached(Node n) {
        // Kept until the next event or frame: a rebuild may re-attach the same node (see Region.keep).
        if (focused == n) focusDetached = true;
        if (pressed == n || dragTarget == n) focusDetached = true;
        hoverChain.remove(n);
        if (tooltipOwner == n) {
            tooltipOwner = null;
            hideTooltip();
        }
        overlays.removeIf(o -> o.node == n);
    }

    /** Called by backends when the host closes the UI. */
    public void dispose() {
        platform.setCursor(Cursor.DEFAULT);
    }
}
