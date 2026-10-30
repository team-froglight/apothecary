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
    private float localX, localY;

    public MouseEvent(float x, float y, int button, int clickCount, float dragX, float dragY) {
        this.x = x;
        this.y = y;
        this.button = button;
        this.clickCount = clickCount;
        this.dragX = dragX;
        this.dragY = dragY;
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

    public void setLocal(float lx, float ly) { localX = lx; localY = ly; }
}
