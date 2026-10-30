package me.methodial.apothecary.event;

/** Mouse wheel. Positive {@code deltaY} = wheel up (content moves down). */
public class ScrollEvent extends MouseEvent {
    private final float deltaX, deltaY;

    public ScrollEvent(float x, float y, float deltaX, float deltaY) {
        super(x, y, -1, 0, 0, 0);
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public float deltaX() { return deltaX; }
    public float deltaY() { return deltaY; }
}
