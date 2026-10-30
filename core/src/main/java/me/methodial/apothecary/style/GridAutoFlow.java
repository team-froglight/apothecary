package me.methodial.apothecary.style;

/** Direction auto-placed grid items fill in; dense back-fills holes. */
public enum GridAutoFlow {
    ROW, COLUMN, ROW_DENSE, COLUMN_DENSE;

    public boolean isColumn() { return this == COLUMN || this == COLUMN_DENSE; }
    public boolean isDense() { return this == ROW_DENSE || this == COLUMN_DENSE; }
}
