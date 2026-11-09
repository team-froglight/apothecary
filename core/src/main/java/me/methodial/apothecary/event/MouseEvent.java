package me.methodial.apothecary.event;

/**
 * Pointer events. {@code x/y} are in root space; {@link #localX()}/{@link #localY()} are relative to the
 * current listener's border box (transforms and scrolling accounted for).
 */
public class MouseEvent extends UIEvent {
    public static final int LEFT = 0, RIGHT = 1, MIDDLE = 2;

    private final float x, y;
    private final int button;
    private final int clickCount;
    private final float dragX, dragY;
    private final int modifiers;
    private float localX, localY;

    public MouseEvent(float x, float y, int button, int clickCount, float dragX, float dragY) {
        this(x, y, button, clickCount, dragX, dragY, 0);
    }

    public MouseEvent(float x, float y, int button, int clickCount, float dragX, float dragY, int modifiers) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.clickCount = clickCount;
        this.dragX = dragX;
        this.dragY = dragY;
        this.modifiers = modifiers;
    }

    public float x() { return x; }
    public float y() { return y; }
    public int button() { return button; }
    /** 1 for a single click, 2 for a double click... */
    public int clickCount() { return clickCount; }
    /** For drag events: total movement since the drag started. */
    public float dragX() { return dragX; }
    public float dragY() { return dragY; }
    public float localX() { return localX; }
    public float localY() { return localY; }
    /** The modifier keys held at the press ({@link KeyEvent#MOD_SHIFT} etc), on press and click events. */
    public int modifiers() { return modifiers; }
    public boolean shift() { return (modifiers & KeyEvent.MOD_SHIFT) != 0; }
    /** Control, or Command on macOS. */
    public boolean primary() { return (modifiers & (KeyEvent.MOD_CONTROL | KeyEvent.MOD_SUPER)) != 0; }
    public boolean alt() { return (modifiers & KeyEvent.MOD_ALT) != 0; }

    public void setLocal(float lx, float ly) { localX = lx; localY = ly; }
}
