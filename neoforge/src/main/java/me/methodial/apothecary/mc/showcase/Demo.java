package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.widget.Text;

/** Small builders shared by showcase pages. */
final class Demo {
    private Demo() { }

    static final String[] PALETTE = {"#ff7a90", "#ffb86b", "#ffe073", "#7ef0a8", "#6fd8ff", "#8b9dff", "#c48bff", "#ff8be0"};

    static Node page() { return new Node("page"); }

    static Node section(String title, String desc, Node... content) {
        Node head = new Node("section-head").add(new Text(title).cls("section-title"));
        if (desc != null) head.add(new Text(desc).cls("section-desc"));
        return new Node("section").add(head).add(content);
    }

    static Node card(String title, String desc, Node... content) {
        Node c = new Node().cls("card");
        if (title != null) c.add(new Text(title).cls("card-title"));
        if (desc != null) c.add(new Text(desc).cls("card-desc"));
        return c.add(content);
    }

    static Node row(Node... n) { return new Node().cls("demo-row").add(n); }

    static Node col(Node... n) { return new Node().cls("demo-col").add(n); }

    static Node control(String label, Node control) {
        return new Node().cls("control-row").add(new Text(label).cls("control-label"), control);
    }

    static Text code(String s) { return (Text) new Text(s).type("code"); }

    static Node badge(String text, String kind) {
        Node b = new Node("badge").add(new Text(text));
        if (kind != null) b.cls(kind);
        return b;
    }

    static Node grow(Node n) { return n.style(s -> s.grow(1)); }
}
