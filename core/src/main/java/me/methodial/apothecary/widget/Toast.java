package me.methodial.apothecary.widget;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.node.UIRoot;
import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.theme.Motion;


/**
 * Transient notifications stacked in the root's toast layer. Kinds (class): {@code info}, {@code success},
 * {@code warning}, {@code error}. Structure: {@code toast > icon.toast-icon + toast-text > (text.toast-title
 * + text.toast-message)} and a {@code toast-timer} bar that drains over the toast's lifetime.
 */
public final class Toast {
    private Toast() { }

    public static Node show(UIRoot root, String kind, String title, String message, float seconds) {
        String icon = switch (kind) {
            case "success" -> "check-circle";
            case "warning" -> "alert";
            case "error" -> "x-circle";
            default -> "info";
        };
        Node toast = new Node("toast").cls(kind);
        Node text = new Node("toast-text");
        text.add(new Text(title).cls("toast-title"));
        if (message != null && !message.isEmpty()) text.add(new Text(message).cls("toast-message"));
        Node timer = new Node("toast-timer");
        // The drain animation's duration follows the toast's lifetime.
        timer.style(s -> s.animation(Motion.DRAIN.play(seconds).easing(Easings.LINEAR).forwards()));
        Button close = Button.icon("x", null);
        close.cls("toast-close", "ghost", "sm");
        close.onClick(() -> root.removeAnimated(toast, 0.25f));
        toast.add(new Icon(icon).cls("toast-icon"), text, close, timer);
        root.toast(toast, seconds);
        root.platform().playSound(kind.equals("error") ? "error" : "toast");
        return toast;
    }
}
