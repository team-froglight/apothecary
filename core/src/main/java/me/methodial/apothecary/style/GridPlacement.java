package me.methodial.apothecary.style;

/**
 * Where an item sits on one grid axis. Most code uses the {@link Style} helpers instead:
 * {@code s.colSpan(2)}, {@code s.gridCol(1, -1)}, {@code s.area("main")}.
 */
public record GridPlacement(Line start, Line end) {
    public static final GridPlacement AUTO = new GridPlacement(Line.AUTO, Line.AUTO);

    /** A grid line reference. */
    public sealed interface Line {
        Line AUTO = new Auto();

        record Auto() implements Line { }
        /** 1-based line number; negative counts from the end (-1 = last line). */
        record Index(int index) implements Line { }
        record Span(int span) implements Line { }
        /** A named template area ("header" → its start or end line). */
        record Area(String name) implements Line { }
    }

    /** Auto-placed, spanning {@code n} tracks. */
    public static GridPlacement span(int n) { return new GridPlacement(new Line.Span(Math.max(1, n)), Line.AUTO); }

    /** From line {@code start} to line {@code end} (1-based; negative counts from the end). */
    public static GridPlacement lines(int start, int end) { return new GridPlacement(new Line.Index(start), new Line.Index(end)); }

    /** Starts at line {@code start}, one track. */
    public static GridPlacement at(int start) { return new GridPlacement(new Line.Index(start), Line.AUTO); }

    /** Starts at line {@code start}, spanning {@code n} tracks. */
    public static GridPlacement at(int start, int span) { return new GridPlacement(new Line.Index(start), new Line.Span(Math.max(1, span))); }

    /** A named template area. */
    public static GridPlacement area(String name) { return new GridPlacement(new Line.Area(name), new Line.Area(name)); }

    public boolean isAuto() {
        return !(start instanceof Line.Index) && !(start instanceof Line.Area)
                && !(end instanceof Line.Index) && !(end instanceof Line.Area);
    }
}
