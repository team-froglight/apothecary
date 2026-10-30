package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.style.Align;
import me.methodial.apothecary.style.FlexDirection;
import me.methodial.apothecary.style.FlexWrap;
import me.methodial.apothecary.style.Justify;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.widget.Button;
import me.methodial.apothecary.widget.NumberStepper;
import me.methodial.apothecary.widget.SegmentedControl;
import me.methodial.apothecary.widget.Select;
import me.methodial.apothecary.widget.Slider;
import me.methodial.apothecary.widget.Switch;
import me.methodial.apothecary.widget.Text;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.theme.Mixins;
import me.methodial.apothecary.theme.Tokens;

import java.util.Random;

import static me.methodial.apothecary.mc.showcase.Demo.*;

final class FlexPage {
    private FlexPage() { }

    static Node build(ShowcaseScreen app) {
        Node play = new Node("playground");
        Random rnd = new Random(7);
        Runnable[] refill = new Runnable[1];
        NumberStepper count = new NumberStepper(1, 16, 6);
        Switch varied = new Switch("Varied sizes", true);
        Switch growFirst = new Switch("First item grows", false);
        refill[0] = () -> {
            while (play.childCount() > count.value()) play.remove(play.child(play.childCount() - 1));
            while (play.childCount() < count.value()) {
                int i = play.childCount();
                Node item = new Node("flex-item").add(new Text(String.valueOf(i + 1)));
                Color bg = Color.hex(PALETTE[i % PALETTE.length]);
                item.style(s -> s.bg(bg));
                item.userData(new float[]{14 + rnd.nextInt(30), 12 + rnd.nextInt(26)});
                play.add(item);
            }
            for (int i = 0; i < play.childCount(); i++) {
                Node item = play.child(i);
                float[] sz = (float[]) item.userData();
                item.style().set(Props.WIDTH, varied.isOn() ? Length.px(sz[0] + 6) : Length.px(28));
                item.style().set(Props.HEIGHT, varied.isOn() ? Length.px(sz[1] + 4) : Length.px(22));
                item.style().set(Props.FLEX_GROW, i == 0 && growFirst.isOn() ? 1f : 0f);
            }
        };
        refill[0].run();
        count.onChange(v -> refill[0].run());
        varied.onChange(v -> refill[0].run());
        growFirst.onChange(v -> refill[0].run());

        SegmentedControl<FlexDirection> dir = new SegmentedControl<FlexDirection>()
                .option(FlexDirection.ROW, "row").option(FlexDirection.ROW_REVERSE, "row-reverse")
                .option(FlexDirection.COLUMN, "column").option(FlexDirection.COLUMN_REVERSE, "column-reverse");
        Select<Justify> justify = new Select<>();
        for (Justify j : new Justify[]{Justify.START, Justify.CENTER, Justify.END, Justify.SPACE_BETWEEN, Justify.SPACE_AROUND, Justify.SPACE_EVENLY}) {
            justify.option(j, j.name().toLowerCase().replace('_', '-'));
        }
        Select<Align> align = new Select<>();
        for (Align a : new Align[]{Align.STRETCH, Align.START, Align.CENTER, Align.END}) align.option(a, a.name().toLowerCase());
        align.value(Align.CENTER);
        Select<Justify> alignContent = new Select<>();
        for (Justify j : new Justify[]{Justify.STRETCH, Justify.START, Justify.CENTER, Justify.END, Justify.SPACE_BETWEEN, Justify.SPACE_AROUND}) {
            alignContent.option(j, j.name().toLowerCase().replace('_', '-'));
        }
        Switch wrap = new Switch("wrap()", true);
        Slider gap = new Slider(0, 16, 4).step(1).showValue(v -> Math.round(v) + "px");

        Text codeOut = code("");
        Runnable apply = () -> {
            play.style().display(me.methodial.apothecary.style.Display.FLEX)
                    .direction(dir.value()).justify(justify.value()).alignItems(align.value()).alignContent(alignContent.value())
                    .wrap(wrap.isOn() ? FlexWrap.WRAP : FlexWrap.NOWRAP).gap(gap.value());
            codeOut.text("s.flex().direction(" + dir.value() + ").justify(" + justify.value() + ").items(" + align.value()
                    + ").content(" + alignContent.value() + ")" + (wrap.isOn() ? ".wrap()" : "") + ".gap(" + Math.round(gap.value()) + ")");
        };
        dir.onChange(v -> apply.run());
        justify.onChange(v -> apply.run());
        align.onChange(v -> apply.run());
        alignContent.onChange(v -> apply.run());
        wrap.onChange(v -> apply.run());
        gap.onChange(v -> apply.run());
        apply.run();

        Node controls = card("Container", null,
                control("direction", dir),
                control("justify", justify),
                control("align-items", align),
                control("align-content", alignContent),
                control("gap", gap.style(s -> s.w(100))),
                control("items", row(count, new Button("refresh", "Shuffle").cls("sm").action(() -> {
                    for (int i = 0; i < play.childCount(); i++) play.child(i).userData(new float[]{14 + rnd.nextInt(30), 12 + rnd.nextInt(26)});
                    refill[0].run();
                }))),
                row(wrap, varied, growFirst));

        Node holy = holyGrail();

        return page().add(
                section("Playground", "Every change re-runs layout; items glide to their new places thanks to layoutTransition().",
                        controls, play, codeOut),
                section("Classic layouts", "Built with nothing but flex.", holy, mediaObject(), pushRow()));
    }

    private static Node tile(String label, String color, java.util.function.Consumer<me.methodial.apothecary.style.Style> extra) {
        return new Node().style(s -> s.radius(4).pad(4).bg(Color.hex(color)).apply(extra)).add(new Text(label));
    }

    private static Node holyGrail() {
        Node header = tile("header", "#8b7bff44", s -> { });
        Node nav = tile("nav", "#4fd8c444", s -> s.w(50));
        Node content = tile("main — grow()", "#ffffff10", s -> s.grow());
        Node aside = tile("aside", "#ff7ac844", s -> s.w(50));
        Node footer = tile("footer", "#ffb86b44", s -> { });
        Node middle = new Node().style(s -> s.flex().gap(4).h(50)).add(nav, content, aside);
        return card("Holy grail", "col > row(nav, grow, aside)",
                new Node().style(s -> s.col().gap(4)).add(header, middle, footer));
    }

    private static Node mediaObject() {
        Node avatar = new Node().style(s -> s.size(26).round().noShrink().gradient(135, Tokens.ACCENT, Tokens.ACCENT_2));
        Node body = new Node().style(s -> s.col().gap(1).shrink(1)).add(
                new Text("Media object").cls("card-title"),
                new Text("The avatar keeps its size (noShrink) while the text wraps beside it, no matter how narrow the card gets.").cls("muted"));
        return card("Media object", null, new Node().style(s -> s.flex().gap(6).itemsStart()).add(avatar, body));
    }

    private static Node pushRow() {
        return card("Auto margins", "pushRight() pushes items apart",
                new Node().style(s -> s.row().gap(4).pad(4).radius(4).apply(Mixins.WELL)).add(
                        new Text("Logo").cls("h3"),
                        new Button("Docs").ghost().cls("sm"),
                        new Button("Blog").ghost().cls("sm"),
                        new Button("Sign in").cls("sm", "primary").style(s -> s.pushRight())));
    }
}
