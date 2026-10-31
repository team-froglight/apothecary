package me.methodial.apothecary;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.theme.DefaultTheme;
import me.methodial.apothecary.theme.OreUI;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.Checkbox;
import me.methodial.apothecary.widget.RadioGroup;
import me.methodial.apothecary.widget.Select;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.TextField;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Ore UI is layered over the default theme, as UIScreen layers it. Whatever the default theme does on hover or
 * focus (accent borders, soft rings, accent outlines) must not show through: Ore UI wells keep their dark frame
 * and top shade in every state, and only Ore UI's own white frame marks focus.
 */
class OreUITest {
    private static UIRoot root(Node content) {
        UIRoot r = new UIRoot(Platform.headless());
        r.addSheet(DefaultTheme.sheet());
        r.theme(OreUI.theme());
        r.addSheet(OreUI.sheet());
        r.resize(400, 300);
        r.setContent(content);
        settle(r);
        return r;
    }

    /** Lets transitions finish, so computed values are the end state. */
    private static void settle(UIRoot r) {
        for (int i = 0; i < 40; i++) r.update(0.05f);
        r.layoutNow();
    }

    private static Node part(Node n, String name) {
        ArrayDeque<Node> queue = new ArrayDeque<>(List.of(n));
        while (!queue.isEmpty()) {
            Node c = queue.poll();
            if (c.typeName().equals(name) || c.classes().contains(name)) return c;
            queue.addAll(c.children());
        }
        return null;
    }

    private record Look(Color border, List<Shadow> shadows, float outline, Color outlineColor) {
        static Look of(Node n) {
            var s = n.computed();
            return new Look(s.get(Props.BORDER_COLOR), s.get(Props.BOX_SHADOW), s.get(Props.OUTLINE_WIDTH), s.get(Props.OUTLINE_COLOR));
        }
    }

    private static void states(UIRoot r, Node n, boolean on, String... states) {
        for (String state : states) n.state(state, on);
        settle(r);
    }

    @Test
    void textFieldKeepsItsWellAndShowsOnlyTheWhiteFrame() {
        TextField field = new TextField();
        UIRoot r = root(new Node().add(field));
        Look rest = Look.of(field);
        var restFill = field.computed().get(Props.BACKGROUND);

        states(r, field, true, "hover");
        assertEquals(rest.border(), Look.of(field).border(), "hover recolors the frame");
        assertEquals(rest.shadows(), Look.of(field).shadows(), "hover replaces the top shade");
        assertNotEquals(field.computed().get(Props.BACKGROUND), restFill, "hover must show: the well lightens");
        states(r, field, false, "hover");

        for (String[] focus : List.of(new String[]{"focus"}, new String[]{"focus", "focus-visible"}, new String[]{"focus", "hover"})) {
            states(r, field, true, focus);
            Look focused = Look.of(field);
            assertEquals(rest.border(), focused.border(), "focus recolors the frame: " + List.of(focus));
            assertEquals(rest.shadows(), focused.shadows(), "focus replaces the top shade with a ring: " + List.of(focus));
            assertEquals(1f, focused.outline(), 0.001f);
            assertEquals(Color.hex("#ffffff"), focused.outlineColor(), "the focus frame is Ore UI's white");
            states(r, field, false, focus);
        }
    }

    @Test
    void everyButtonFaceGetsVisiblyLighterOnHoverAsInTheDesignSystem() {
        for (String variant : new String[]{null, "primary", "dark"}) assertLighterOnHover(variant);
    }

    @Test
    void pressedFacesQuietTheirEdgeLines() {
        // Sunk, a face keeps only a hint of its edges: at full strength (or hover strength) the light top and left line
        // read as a glowing L along a pressed face, the opposite of pressed in.
        for (String variant : new String[]{null, "primary", "dark"}) {
            Button button = new Button("Go");
            if (variant != null) button.variant(variant);
            UIRoot r = root(new Node().add(button));
            float rest = Math.abs(luma(topLine(button)) - luma(face(button)));
            states(r, button, true, "hover", "active");
            float pressed = Math.abs(luma(topLine(button)) - luma(face(button)));
            assertTrue(pressed < rest, variant + ": the top line stands out more pressed (" + pressed + ") than at rest (" + rest + ")");
        }
    }

    private static Color topLine(Node n) {
        for (Shadow s : n.computed().get(Props.BOX_SHADOW)) if (s.inset() && s.offsetY() > 0) return s.color();
        throw new AssertionError("no top line on " + n);
    }

    private static Color face(Node n) {
        return ((me.methodial.apothecary.style.Paint.Solid) n.computed().get(Props.BACKGROUND)).color();
    }

    private void assertLighterOnHover(String variant) {
        Button button = new Button("Go");
        if (variant != null) button.variant(variant);
        UIRoot r = root(new Node().add(button));
        Color rest = ((me.methodial.apothecary.style.Paint.Solid) button.computed().get(Props.BACKGROUND)).color();
        states(r, button, true, "hover");
        Color hover = ((me.methodial.apothecary.style.Paint.Solid) button.computed().get(Props.BACKGROUND)).color();
        assertTrue(luma(hover) - luma(rest) >= 0.04f, variant + ": hovered face barely changes: " + rest + " -> " + hover);
        states(r, button, false, "hover");
        Color restEdge = edge(button);
        states(r, button, true, "hover");
        Color hoverEdge = edge(button);
        assertTrue(luma(hoverEdge) >= luma(restEdge), variant + ": hovered edge darkens: " + restEdge + " -> " + hoverEdge);
    }

    /** The stroke along the face's right edge, as drawn. */
    private static Color edge(Node n) {
        for (Shadow s : n.computed().get(Props.BOX_SHADOW)) if (s.inset() && s.offsetX() < 0) return s.color();
        throw new AssertionError("no right-edge stroke on " + n);
    }

    private static float luma(Color c) { return 0.2126f * c.r() + 0.7152f * c.g() + 0.0722f * c.b(); }

    @Test
    void invalidTextFieldKeepsItsTopShade() {
        TextField field = new TextField();
        UIRoot r = root(new Node().add(field));
        List<Shadow> rest = Look.of(field).shadows();
        field.cls("invalid");
        settle(r);
        assertEquals(rest, Look.of(field).shadows(), "invalid adds the default theme's ring");
    }

    @Test
    void checkboxAndRadioWellsKeepTheirFrameOnHover() {
        Checkbox checkbox = new Checkbox("Check");
        RadioGroup<String> radios = new RadioGroup<>();
        radios.option("a", "A").option("b", "B");
        UIRoot r = root(new Node().add(checkbox, radios));
        Node box = part(checkbox, "checkbox-box");
        // The second option: the first starts selected, and a selected dot has no frame to recolor.
        Node radio = radios.children().get(1);
        Node dot = part(radio, "radio-dot");
        assertNotNull(box);
        assertNotNull(dot);
        Color boxRest = Look.of(box).border(), dotRest = Look.of(dot).border();
        states(r, checkbox, true, "hover");
        states(r, radio, true, "hover");
        assertEquals(boxRest, Look.of(box).border(), "checkbox hover recolors the frame");
        assertEquals(dotRest, Look.of(dot).border(), "radio hover recolors the frame");
    }

    @Test
    void buttonsAndSelectsHaveNoAccentOutlineOnKeyboardFocus() {
        Button button = new Button("Go");
        Select<String> select = new Select<>();
        select.option("a", "A");
        UIRoot r = root(new Node().add(button, select));
        states(r, button, true, "focus", "focus-visible");
        states(r, select, true, "focus", "focus-visible");
        assertEquals(0f, Look.of(button).outline(), 0.001f, "button gets the default theme's accent outline");
        assertEquals(0f, Look.of(select).outline(), 0.001f, "select gets the default theme's accent outline");
    }

    @Test
    void sliderThumbKeepsItsFaceOnHoverAndDrag() {
        Slider slider = new Slider(0, 10, 5);
        UIRoot r = root(new Node().add(slider));
        Node thumb = part(slider, "slider-thumb");
        assertNotNull(thumb);
        List<Shadow> rest = Look.of(thumb).shadows();
        for (String state : List.of("hover", "dragging")) {
            states(r, slider, true, state);
            assertEquals(rest, Look.of(thumb).shadows(), state + " replaces the thumb's face with a ring");
            states(r, slider, false, state);
        }
        states(r, slider, true, "focus", "focus-visible");
        assertEquals(0f, Look.of(thumb).outline(), 0.001f, "thumb gets the default theme's accent outline");
    }
}
