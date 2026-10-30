package me.methodial.apothecary.style;

/** Main axis of a flex container. */
public enum FlexDirection {
    ROW, ROW_REVERSE, COLUMN, COLUMN_REVERSE;

    public boolean isRow() { return this == ROW || this == ROW_REVERSE; }
    public boolean isReverse() { return this == ROW_REVERSE || this == COLUMN_REVERSE; }
}
