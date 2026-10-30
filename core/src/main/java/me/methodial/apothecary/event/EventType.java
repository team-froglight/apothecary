package me.methodial.apothecary.event;

/** A typed event key. {@code bubbles = false} events are delivered to the target only. */
public final class EventType<E extends UIEvent> {
    private final String name;
    private final boolean bubbles;

    public EventType(String name, boolean bubbles) {
        this.name = name;
        this.bubbles = bubbles;
    }

    public String name() { return name; }
    public boolean bubbles() { return bubbles; }

    @Override public String toString() { return name; }

    public static final EventType<MouseEvent> MOUSE_DOWN = new EventType<>("mousedown", true);
    public static final EventType<MouseEvent> MOUSE_UP = new EventType<>("mouseup", true);
    /** Press and release on the same element with the left button. */
    public static final EventType<MouseEvent> CLICK = new EventType<>("click", true);
    /** Press and release with the right button. */
    public static final EventType<MouseEvent> CONTEXT_MENU = new EventType<>("contextmenu", true);
    public static final EventType<MouseEvent> MOUSE_MOVE = new EventType<>("mousemove", true);
    public static final EventType<MouseEvent> MOUSE_ENTER = new EventType<>("mouseenter", false);
    public static final EventType<MouseEvent> MOUSE_LEAVE = new EventType<>("mouseleave", false);
    /** Sent to the element that received MOUSE_DOWN once the pointer moves past the drag threshold. */
    public static final EventType<MouseEvent> DRAG_START = new EventType<>("dragstart", false);
    public static final EventType<MouseEvent> DRAG = new EventType<>("drag", false);
    public static final EventType<MouseEvent> DRAG_END = new EventType<>("dragend", false);
    public static final EventType<ScrollEvent> SCROLL = new EventType<>("wheel", true);
    public static final EventType<KeyEvent> KEY_DOWN = new EventType<>("keydown", true);
    public static final EventType<KeyEvent> KEY_UP = new EventType<>("keyup", true);
    public static final EventType<CharEvent> CHAR = new EventType<>("char", true);
    public static final EventType<UIEvent> FOCUS = new EventType<>("focus", false);
    public static final EventType<UIEvent> BLUR = new EventType<>("blur", false);
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static final EventType<ChangeEvent<?>> CHANGE = new EventType("change", true);
    /** Fired when an element is attached to a live root. */
    public static final EventType<UIEvent> MOUNT = new EventType<>("mount", false);
    public static final EventType<UIEvent> UNMOUNT = new EventType<>("unmount", false);
}
