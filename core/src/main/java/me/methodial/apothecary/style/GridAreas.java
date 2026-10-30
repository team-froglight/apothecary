package me.methodial.apothecary.style;

/** grid-template-areas: a matrix of area names ("." = empty cell). */
public record GridAreas(String[][] rows) {
    public static final GridAreas NONE = new GridAreas(new String[0][]);

    /** One string per row, names separated by spaces ("." = empty): {@code GridAreas.of("head head", "side main")}. */
    public static GridAreas of(String... rows) {
        String[][] r = new String[rows.length][];
        for (int i = 0; i < rows.length; i++) r[i] = rows[i].trim().split("\\s+");
        return new GridAreas(r);
    }

    public int rowCount() { return rows.length; }
    public int columnCount() { int c = 0; for (String[] r : rows) c = Math.max(c, r.length); return c; }

    /** {rowStart, colStart, rowEnd, colEnd} as 0-based track indices (end exclusive), or null. */
    public int[] find(String name) {
        int r0 = -1, c0 = -1, r1 = -1, c1 = -1;
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                if (rows[r][c].equals(name)) {
                    if (r0 < 0) { r0 = r; c0 = c; }
                    r1 = Math.max(r1, r + 1);
                    c1 = Math.max(c1, c + 1);
                    c0 = Math.min(c0, c);
                }
            }
        }
        return r0 < 0 ? null : new int[]{r0, c0, r1, c1};
    }
}
