package me.methodial.apothecary.style;

import me.methodial.apothecary.geom.Corners;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The property registry. Most code never touches these directly — use the fluent {@link Style} API —
 * but they're handy for reading computed values ({@code node.get(Props.COLOR)}), animating
 * ({@code node.animate(Props.OPACITY, 0f, ...)}) and choosing what to transition.
 */
public final class Props {
    private Props() { }

    private static final List<Prop<?>> ALL = new ArrayList<>();

    /** Registers a custom property. */
    public static synchronized <T> Prop<T> register(String name, T initial, boolean inherited, Prop.Affects affects, Prop.Lerp<T> lerp) {
        Prop<T> p = new Prop<>(name, ALL.size(), initial, inherited, affects, lerp);
        ALL.add(p);
        return p;
    }

    public static int count() { return ALL.size(); }
    public static List<Prop<?>> all() { return Collections.unmodifiableList(ALL); }

    // ---------------------------------------------------------------- helpers

    private static final Prop.Lerp<Float> F = (a, b, t) -> a + (b - a) * t;
    private static final Prop.Lerp<Length> L = Length::lerp;
    private static final Prop.Lerp<Color> C = Color::lerp;
    private static final Prop.Affects LAYOUT = Prop.Affects.LAYOUT, PAINT = Prop.Affects.PAINT;

    private static Prop<Length> length(String name, Length initial, Prop.Affects affects) { return register(name, initial, false, affects, L); }

    private static Prop<Float> number(String name, float initial, Prop.Affects affects) { return register(name, initial, false, affects, F); }

    private static <E extends Enum<E>> Prop<E> discrete(String name, E initial, boolean inherited, Prop.Affects affects) {
        return register(name, initial, inherited, affects, null);
    }

    // ---------------------------------------------------------------- box & layout

    public static final Prop<Display> DISPLAY = discrete("display", Display.BLOCK, false, LAYOUT);
    public static final Prop<Position> POSITION = discrete("position", Position.RELATIVE, false, LAYOUT);
    public static final Prop<Length> TOP = length("top", Length.AUTO, LAYOUT);
    public static final Prop<Length> RIGHT = length("right", Length.AUTO, LAYOUT);
    public static final Prop<Length> BOTTOM = length("bottom", Length.AUTO, LAYOUT);
    public static final Prop<Length> LEFT = length("left", Length.AUTO, LAYOUT);

    public static final Prop<Length> WIDTH = length("width", Length.AUTO, LAYOUT);
    public static final Prop<Length> HEIGHT = length("height", Length.AUTO, LAYOUT);
    public static final Prop<Length> MIN_WIDTH = length("min-width", Length.AUTO, LAYOUT);
    public static final Prop<Length> MIN_HEIGHT = length("min-height", Length.AUTO, LAYOUT);
    public static final Prop<Length> MAX_WIDTH = length("max-width", Length.AUTO, LAYOUT);
    public static final Prop<Length> MAX_HEIGHT = length("max-height", Length.AUTO, LAYOUT);
    /** width / height; NaN = none. */
    public static final Prop<Float> ASPECT_RATIO = number("aspect-ratio", Float.NaN, LAYOUT);

    public static final Prop<Length> MARGIN_TOP = length("margin-top", Length.ZERO, LAYOUT);
    public static final Prop<Length> MARGIN_RIGHT = length("margin-right", Length.ZERO, LAYOUT);
    public static final Prop<Length> MARGIN_BOTTOM = length("margin-bottom", Length.ZERO, LAYOUT);
    public static final Prop<Length> MARGIN_LEFT = length("margin-left", Length.ZERO, LAYOUT);
    public static final Prop<Length> PADDING_TOP = length("padding-top", Length.ZERO, LAYOUT);
    public static final Prop<Length> PADDING_RIGHT = length("padding-right", Length.ZERO, LAYOUT);
    public static final Prop<Length> PADDING_BOTTOM = length("padding-bottom", Length.ZERO, LAYOUT);
    public static final Prop<Length> PADDING_LEFT = length("padding-left", Length.ZERO, LAYOUT);
    public static final Prop<Float> BORDER_TOP_WIDTH = number("border-top-width", 0, LAYOUT);
    public static final Prop<Float> BORDER_RIGHT_WIDTH = number("border-right-width", 0, LAYOUT);
    public static final Prop<Float> BORDER_BOTTOM_WIDTH = number("border-bottom-width", 0, LAYOUT);
    public static final Prop<Float> BORDER_LEFT_WIDTH = number("border-left-width", 0, LAYOUT);

    public static final Prop<FlexDirection> FLEX_DIRECTION = discrete("flex-direction", FlexDirection.ROW, false, LAYOUT);
    public static final Prop<FlexWrap> FLEX_WRAP = discrete("flex-wrap", FlexWrap.NOWRAP, false, LAYOUT);
    public static final Prop<Justify> JUSTIFY_CONTENT = discrete("justify-content", Justify.START, false, LAYOUT);
    public static final Prop<Justify> ALIGN_CONTENT = discrete("align-content", Justify.STRETCH, false, LAYOUT);
    public static final Prop<Align> ALIGN_ITEMS = discrete("align-items", Align.STRETCH, false, LAYOUT);
    public static final Prop<Align> ALIGN_SELF = discrete("align-self", Align.AUTO, false, LAYOUT);
    public static final Prop<Align> JUSTIFY_ITEMS = discrete("justify-items", Align.STRETCH, false, LAYOUT);
    public static final Prop<Align> JUSTIFY_SELF = discrete("justify-self", Align.AUTO, false, LAYOUT);
    public static final Prop<Float> FLEX_GROW = number("flex-grow", 0f, LAYOUT);
    public static final Prop<Float> FLEX_SHRINK = number("flex-shrink", 1f, LAYOUT);
    public static final Prop<Length> FLEX_BASIS = length("flex-basis", Length.AUTO, LAYOUT);
    public static final Prop<Integer> ORDER = register("order", 0, false, LAYOUT, null);
    public static final Prop<Length> ROW_GAP = length("row-gap", Length.ZERO, LAYOUT);
    public static final Prop<Length> COLUMN_GAP = length("column-gap", Length.ZERO, LAYOUT);

    public static final Prop<List<TrackEntry>> GRID_TEMPLATE_COLUMNS = register("grid-template-columns", List.of(), false, LAYOUT, null);
    public static final Prop<List<TrackEntry>> GRID_TEMPLATE_ROWS = register("grid-template-rows", List.of(), false, LAYOUT, null);
    public static final Prop<GridAreas> GRID_TEMPLATE_AREAS = register("grid-template-areas", GridAreas.NONE, false, LAYOUT, null);
    public static final Prop<List<TrackSize>> GRID_AUTO_COLUMNS = register("grid-auto-columns", List.of(TrackSize.AUTO), false, LAYOUT, null);
    public static final Prop<List<TrackSize>> GRID_AUTO_ROWS = register("grid-auto-rows", List.of(TrackSize.AUTO), false, LAYOUT, null);
    public static final Prop<GridAutoFlow> GRID_AUTO_FLOW = discrete("grid-auto-flow", GridAutoFlow.ROW, false, LAYOUT);
    public static final Prop<GridPlacement> GRID_COLUMN = register("grid-column", GridPlacement.AUTO, false, LAYOUT, null);
    public static final Prop<GridPlacement> GRID_ROW = register("grid-row", GridPlacement.AUTO, false, LAYOUT, null);

    public static final Prop<Overflow> OVERFLOW_X = discrete("overflow-x", Overflow.VISIBLE, false, LAYOUT);
    public static final Prop<Overflow> OVERFLOW_Y = discrete("overflow-y", Overflow.VISIBLE, false, LAYOUT);
    public static final Prop<Integer> Z_INDEX = register("z-index", 0, false, PAINT, null);

    // ---------------------------------------------------------------- visuals

    public static final Prop<Paint> BACKGROUND = register("background", Paint.NONE, false, PAINT, Paint::lerp);
    public static final Prop<Color> BORDER_COLOR = register("border-color", Color.TRANSPARENT, false, PAINT, C);
    public static final Prop<Corners> BORDER_RADIUS = register("border-radius", Corners.ZERO, false, PAINT, Corners::lerp);
    public static final Prop<List<Shadow>> BOX_SHADOW = register("box-shadow", Shadow.NONE, false, PAINT, Shadow::lerp);
    public static final Prop<Float> OPACITY = number("opacity", 1f, PAINT);
    public static final Prop<Transform> TRANSFORM = register("transform", Transform.IDENTITY, false, PAINT, Transform::lerp);
    public static final Prop<TransformOrigin> TRANSFORM_ORIGIN = register("transform-origin", TransformOrigin.CENTER, false, PAINT,
            (a, b, t) -> new TransformOrigin(a.x().lerp(b.x(), t), a.y().lerp(b.y(), t)));
    public static final Prop<Visibility> VISIBILITY = discrete("visibility", Visibility.VISIBLE, true, PAINT);
    public static final Prop<Cursor> CURSOR = discrete("cursor", Cursor.DEFAULT, true, PAINT);
    /** Not inherited here (unlike CSS): NONE makes only this element transparent to the pointer. */
    public static final Prop<PointerEvents> POINTER_EVENTS = discrete("pointer-events", PointerEvents.AUTO, false, PAINT);
    public static final Prop<Color> OUTLINE_COLOR = register("outline-color", Color.TRANSPARENT, false, PAINT, C);
    public static final Prop<Float> OUTLINE_WIDTH = number("outline-width", 0, PAINT);
    public static final Prop<Float> OUTLINE_OFFSET = number("outline-offset", 0, PAINT);

    /** Tint multiplied into images, icons and shader nodes. */
    public static final Prop<Color> TINT = register("tint", Color.WHITE, false, PAINT, C);
    public static final Prop<ObjectFit> OBJECT_FIT = discrete("object-fit", ObjectFit.FILL, false, PAINT);
    /** Icon name for {@code Icon} nodes, themable from sheets. */
    public static final Prop<String> ICON = register("icon", "", false, PAINT, null);
    /** Stroke width for vector icons and lines, in the icon's 24-unit space. */
    public static final Prop<Float> STROKE_WIDTH = register("stroke-width", 2f, true, PAINT, F);

    public static final Prop<Color> SCROLLBAR_COLOR = register("scrollbar-color", Color.argb(110, 255, 255, 255), true, PAINT, C);
    public static final Prop<Color> SCROLLBAR_TRACK_COLOR = register("scrollbar-track-color", Color.TRANSPARENT, true, PAINT, C);
    public static final Prop<Float> SCROLLBAR_WIDTH = register("scrollbar-width", 4f, true, PAINT, F);
    /** A square scrollbar drawn as a raised block in a dark groove, for blocky designs, instead of a rounded pill. */
    public static final Prop<Boolean> SCROLLBAR_BLOCKY = register("scrollbar-blocky", false, true, PAINT, null);
    /** Seconds the smooth-scroll animation takes to settle; 0 = instant. */
    public static final Prop<Float> SCROLL_SMOOTHNESS = register("scroll-smoothness", 0.12f, true, PAINT, F);

    // ---------------------------------------------------------------- text (mostly inherited)

    public static final Prop<Color> COLOR = register("color", Color.WHITE, true, PAINT, C);
    public static final Prop<String> FONT_FAMILY = register("font-family", "default", true, LAYOUT, null);
    /** Font size in px (9 = one vanilla Minecraft line). */
    public static final Prop<Float> FONT_SIZE = register("font-size", 9f, true, LAYOUT, F);
    public static final Prop<FontWeight> FONT_WEIGHT = discrete("font-weight", FontWeight.NORMAL, true, LAYOUT);
    public static final Prop<Boolean> FONT_ITALIC = register("font-style", false, true, LAYOUT, null);
    /** Minecraft-style hard drop shadow under text. */
    public static final Prop<Boolean> TEXT_SHADOW = register("text-shadow", false, true, PAINT, null);
    public static final Prop<TextAlign> TEXT_ALIGN = discrete("text-align", TextAlign.LEFT, true, PAINT);
    public static final Prop<VerticalAlign> VERTICAL_ALIGN = discrete("vertical-align", VerticalAlign.TOP, false, PAINT);
    /** Line height as a multiple of the font size. */
    public static final Prop<Float> LINE_HEIGHT = register("line-height", 1.2f, true, LAYOUT, F);
    public static final Prop<Float> LETTER_SPACING = register("letter-spacing", 0f, true, LAYOUT, F);
    public static final Prop<WhiteSpace> WHITE_SPACE = discrete("white-space", WhiteSpace.NORMAL, true, LAYOUT);
    public static final Prop<TextOverflow> TEXT_OVERFLOW = discrete("text-overflow", TextOverflow.CLIP, false, LAYOUT);
    public static final Prop<TextTransform> TEXT_TRANSFORM = discrete("text-transform", TextTransform.NONE, true, LAYOUT);
    public static final Prop<TextDecoration> TEXT_DECORATION = register("text-decoration", TextDecoration.NONE, true, PAINT, null);
    /** Maximum number of lines; 0 = unlimited. With an ellipsis the last line gets "…". */
    public static final Prop<Integer> MAX_LINES = register("max-lines", 0, false, LAYOUT, null);

    // ---------------------------------------------------------------- motion

    public static final Prop<List<TransitionSpec>> TRANSITION = register("transition", List.of(), false, PAINT, null);
    public static final Prop<List<AnimationSpec>> ANIMATION = register("animation", List.of(), false, PAINT, null);
    /** Animates an element's position when layout moves it (FLIP). */
    public static final Prop<List<TransitionSpec>> LAYOUT_TRANSITION = register("layout-transition", List.of(), false, PAINT, null);

    // ---------------------------------------------------------------- groups (handy for transitions)

    /** Background, text, border, outline and tint colors. */
    public static final List<Prop<?>> COLORS = List.of(BACKGROUND, COLOR, BORDER_COLOR, OUTLINE_COLOR, TINT, SCROLLBAR_COLOR);
    public static final List<Prop<?>> PADDING = List.of(PADDING_TOP, PADDING_RIGHT, PADDING_BOTTOM, PADDING_LEFT);
    public static final List<Prop<?>> MARGIN = List.of(MARGIN_TOP, MARGIN_RIGHT, MARGIN_BOTTOM, MARGIN_LEFT);
    public static final List<Prop<?>> BORDER_WIDTHS = List.of(BORDER_TOP_WIDTH, BORDER_RIGHT_WIDTH, BORDER_BOTTOM_WIDTH, BORDER_LEFT_WIDTH);
    public static final List<Prop<?>> SIZE = List.of(WIDTH, HEIGHT);
    public static final List<Prop<?>> INSET = List.of(TOP, RIGHT, BOTTOM, LEFT);

    /** Forces class initialization (all built-in props registered). */
    public static void init() { }
}
