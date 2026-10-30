package me.methodial.apothecary.widget;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;

/**
 * A modal dialog. Structure: {@code dialog > dialog-header > (icon? + text.dialog-title + button.dialog-close)},
 * {@code dialog-body}, {@code dialog-footer > button*}. Opened on the root's modal layer with an
 * animated backdrop ({@code modal-backdrop}); Escape or the close button dismisses it.
 */
public class Dialog extends Node {
    private final Node body = new Node("dialog-body");
    private final Node footer = new Node("dialog-footer");
    private Node backdrop;
    private boolean dismissOnBackdrop = true;
    private Runnable onClose;

    public Dialog(String title) { this(null, title); }

    public Dialog(String icon, String title) {
        super("dialog");
        Node header = new Node("dialog-header");
        if (icon != null) header.add(new Icon(icon).cls("dialog-icon"));
        header.add(new Text(title).cls("dialog-title"));
        Button close = Button.icon("x", null);
        close.cls("dialog-close", "ghost", "sm");
        close.onClick(this::close);
        header.add(close);
        add(header, body, footer);
        footer.style(s -> s.hidden());
    }

    public Node body() { return body; }

    public Dialog body(Node... content) { body.add(content); return this; }

    public Dialog text(String paragraph) { body.add(new Text(paragraph).cls("dialog-text")); return this; }

    /** Adds a footer button; the dialog closes after {@code action} unless it's null. */
    public Dialog button(String label, String variant, Runnable action) {
        footer.style(s -> s.display(me.methodial.apothecary.style.Display.FLEX));
        Button b = new Button(label);
        if (variant != null) b.variant(variant);
        b.onClick(() -> {
            if (action != null) action.run();
            close();
        });
        footer.add(b);
        return this;
    }

    public Dialog dismissOnBackdrop(boolean d) { this.dismissOnBackdrop = d; return this; }

    public Dialog onClose(Runnable r) { this.onClose = r; return this; }

    public Dialog open(UIRoot root) {
        backdrop = root.openModal(this, onClose);
        if (dismissOnBackdrop) backdrop.cls("dismissable");
        root.platform().playSound("open");
        return this;
    }

    public void close() {
        if (root() != null && backdrop != null) {
            root().platform().playSound("close");
            root().close(backdrop);
        }
    }
}
