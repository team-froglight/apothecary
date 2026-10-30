package me.methodial.apothecary.widget;

import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;

/**
 * A collapsible section whose body animates its height open/closed. Structure:
 * {@code accordion > accordion-header > (text + icon.accordion-arrow)}, {@code accordion-body > accordion-content}.
 * {@code accordion:open} while expanded.
 */
public class Accordion extends ValueWidget<Boolean> {
    private final Node body = new Node("accordion-body");
    private final Node content = new Node("accordion-content");
    private float duration = 0.28f;

    public Accordion(String title, Node... children) {
        super("accordion", false);
        Node header = new Node("accordion-header");
        header.focusable(true);
        header.add(new Text(title).cls("accordion-title"), new Icon("chevron-down").cls("accordion-arrow"));
        header.onClick(() -> { value(!isOpen()); sound("click"); });
        content.add(children);
        body.add(content);
        body.baseStyle(s -> s.clip());
        body.style().height(0);
        add(header, body);
    }

    public Node content() { return content; }

    public boolean isOpen() { return Boolean.TRUE.equals(value); }

    public Accordion open(boolean o) {
        value(o);
        return this;
    }

    public Accordion duration(float seconds) { this.duration = seconds; return this; }

    @Override
    protected void valueChanged(Boolean old, Boolean now) {
        boolean open = Boolean.TRUE.equals(now);
        state("open", open);
        float natural = content.height();
        if (!isAttached() || natural <= 0) {
            body.style().set(Props.HEIGHT, open ? Length.AUTO : Length.px(0));
            return;
        }
        float from = body.height();
        // Animate in pixels, then release to auto so the content can reflow freely.
        body.animate(Props.HEIGHT, Length.px(from), Length.px(open ? natural : 0), duration, Easings.OUT_CUBIC,
                () -> { if (isOpen()) body.style().set(Props.HEIGHT, Length.AUTO); });
    }
}
