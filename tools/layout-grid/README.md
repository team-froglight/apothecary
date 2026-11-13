# layout-grid

Checks a live Apothecary screen for uneven layout and draws what it finds.

1. Open the screen in a client with jec, then run `collect.jsnip` with `jec_client_eval` (paste its body). It writes
   every laid-out box to `<gameDir>/layout-grid/boxes.tsv` and grabs the frame to `<gameDir>/screenshots/layout-grid.png`.
   Run it after the screen's rise-in animation has settled.
2. `java LayoutGrid.java <gameDir>` prints the findings and writes `<gameDir>/layout-grid/grid.png`: every box
   outlined by depth, each finding boxed in red with its number.

Findings:

- `EDGE`: among siblings, a left or right edge 1 to 12 px off the edge most of them share (a near miss).
- `HEIGHT`: boxes on one row with different heights.
- `GAP`: a column whose gaps between children differ.
- `OVER`: a box sticking out of a parent that doesn't scroll.
