package me.methodial.apothecary.theme;

import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.style.Align;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Justify;
import me.methodial.apothecary.style.Length;
import me.methodial.apothecary.style.Tr;

import static me.methodial.apothecary.sheet.Selector.*;
import static me.methodial.apothecary.theme.Colors.alpha;
import static me.methodial.apothecary.theme.Motion.*;
import static me.methodial.apothecary.theme.Tokens.*;

/**
 * The built-in look of every widget, written as a {@link Sheet}. It only uses {@link Tokens}, so a
 * {@link Theme} re-skins it completely. Build your own design system the same way, or layer a sheet on
 * top that overrides a few rules (later sheets win ties).
 */
public final class DefaultTheme {
    private DefaultTheme() { }

    private static Sheet instance;

    /** The shared default sheet (built once). */
    public static synchronized Sheet sheet() {
        if (instance == null) instance = build();
        return instance;
    }

    public static Sheet build() {
        Sheet sheet = new Sheet("apothecary:default");
        base(sheet);
        utilities(sheet);
        surfaces(sheet);
        buttons(sheet);
        toggles(sheet);
        sliders(sheet);
        fields(sheet);
        menus(sheet);
        tabs(sheet);
        disclosure(sheet);
        overlays(sheet);
        pickers(sheet);
        minecraft(sheet);
        return sheet;
    }

    // ------------------------------------------------------------------ root & theming defaults

    private static void base(Sheet sheet) {
        sheet.rule(root(), s -> s.font(FONT).fontSize(8).fg(TEXT).lineHeight(1.5f).scrollbar(3, SCROLLBAR));
        // Lowest-specificity color transitions so theme switches animate everywhere;
        // any element with its own transition list overrides this.
        sheet.rule(any(), s -> s.transition(Tr.colors(0.26f)));
    }

    // ------------------------------------------------------------------ utility classes

    private static void utilities(Sheet sheet) {
        sheet.rule(cls("h1"), s -> s.type(TypeScale.H1));
        sheet.rule(cls("h2"), s -> s.type(TypeScale.H2));
        sheet.rule(cls("h3"), s -> s.type(TypeScale.H3));
        sheet.rule(cls("lead"), s -> s.type(TypeScale.LEAD));
        sheet.rule(cls("muted"), s -> s.fg(TEXT_DIM));
        sheet.rule(cls("faint"), s -> s.fg(TEXT_FAINT));
        sheet.rule(cls("small"), s -> s.fontSize(7));
        sheet.rule(cls("mono"), s -> s.font(MONO));
        sheet.rule(cls("caps"), s -> s.type(TypeScale.CAPS));
        sheet.rule(cls("accent"), s -> s.fg(ACCENT));
    }

    // ------------------------------------------------------------------ surfaces

    private static void surfaces(Sheet sheet) {
        sheet.onCls("card", r -> r
                .style(Mixins.CARD)
                .variant("hoverable", v -> v
                        .style(s -> s.pointer().transition(Tr.transform(0.22f, OUT), Tr.shadow(0.22f), Tr.colors(0.22f)))
                        .hover(Mixins.LIFTED)));
        sheet.onCls("panel", r -> r.style(Mixins.PANEL));
        sheet.on("divider", r -> r
                .style(s -> s.h(1).bg(BORDER).noShrink())
                .variantStyle("vertical", s -> s.w(1).h(Length.AUTO).self(Align.STRETCH)));
        sheet.on("badge", r -> r
                .style(s -> s.row().gap(2).pad(1, 4).radius(6).bg(ACCENT_SOFT).fg(ACCENT_HOVER).fontSize(6.5f).semibold())
                .variantStyle("success", s -> s.bg(alpha(SUCCESS, 0.15f)).fg(SUCCESS))
                .variantStyle("warning", s -> s.bg(alpha(WARNING, 0.15f)).fg(WARNING))
                .variantStyle("danger", s -> s.bg(alpha(DANGER, 0.15f)).fg(DANGER))
                .variantStyle("info", s -> s.bg(alpha(INFO, 0.15f)).fg(INFO)));
        sheet.style("kbd", s -> s.padX(3).radius(2).bg(SURFACE_3).border(1, BORDER_STRONG).font(MONO).fontSize(6.5f).fg(TEXT_DIM));
        sheet.style("code", s -> s.type(TypeScale.CODE).fg(ACCENT_HOVER).bg(ACCENT_SOFT).pad(1, 3).radius(2));
    }

    // ------------------------------------------------------------------ buttons

    private static void buttons(Sheet sheet) {
        sheet.on("button", r -> r
                .style(s -> s.row().center().gap(4).pad(4, 9).radius(RADIUS).bg(SURFACE_3).border(1, BORDER_STRONG)
                        .fg(TEXT).medium().pointer().noShrink()
                        .transition(Tr.colors(FAST), Tr.transform(0.14f, OUT), Tr.shadow(NORMAL), Tr.opacity(FAST)))
                .hover(s -> s.bg(SURFACE_HOVER))
                .active(s -> s.scale(0.96f))
                .focusVisible(Mixins.FOCUS_RING)
                .disabled(s -> s.opacity(0.4f).untransformed())
                .variant("primary", v -> v
                        .style(s -> s.gradient(180, ACCENT_HOVER, ACCENT).borderColor(Colors.alpha(Colors.WHITE, 0.19f))
                                .fg(ACCENT_TEXT).shadow(0, 2, 8, alpha(ACCENT_PRESS, 0.33f)))
                        .hover(s -> s.gradient(180, Colors.lighten(ACCENT_HOVER, 0.15f), ACCENT_HOVER).shadow(0, 4, 14, alpha(ACCENT_PRESS, 0.53f)))
                        .active(s -> s.bg(ACCENT_PRESS)))
                .variant("ghost", v -> v
                        .style(s -> s.bg(Color.TRANSPARENT).borderColor(Color.TRANSPARENT).fg(TEXT_DIM))
                        .hover(s -> s.bg(HOVER_TINT).fg(TEXT)))
                .variant("outline", v -> v
                        .style(s -> s.bg(Color.TRANSPARENT).borderColor(ACCENT).fg(ACCENT_HOVER))
                        .hover(s -> s.bg(ACCENT_SOFT)))
                .variant("danger", v -> v
                        .style(s -> s.bg(alpha(DANGER, 0.13f)).borderColor(alpha(DANGER, 0.33f)).fg(DANGER))
                        .hover(s -> s.bg(alpha(DANGER, 0.22f))))
                .variant("success", v -> v
                        .style(s -> s.bg(alpha(SUCCESS, 0.13f)).borderColor(alpha(SUCCESS, 0.33f)).fg(SUCCESS))
                        .hover(s -> s.bg(alpha(SUCCESS, 0.22f))))
                .variantStyle("sm", s -> s.pad(2, 6).fontSize(7).radius(RADIUS_SM))
                .variantStyle("lg", s -> s.pad(6, 14).fontSize(10).radius(RADIUS_LG))
                .variantStyle("icon-only", s -> s.pad(4))
                .refine(sel -> sel.withCls("icon-only").withCls("sm"), v -> v.style(s -> s.pad(2)))
                .variantStyle("pill", s -> s.round())
                .partCls("button-icon", p -> p.style(s -> s.fontSize(9).stroke(2.2f))));
        sheet.rule(cls("button-icon").in(is("button").withCls("sm")), s -> s.fontSize(7));
        sheet.rule(cls("button-icon").in(is("button").withCls("lg")), s -> s.fontSize(11));
    }

    // ------------------------------------------------------------------ checkbox, switch, radio

    private static void toggles(Sheet sheet) {
        for (String t : new String[]{"checkbox", "switch", "radio"}) {
            sheet.on(t, r -> r.style(s -> s.row().gap(5).pointer()).disabled(s -> s.opacity(0.45f)));
        }

        sheet.on("checkbox", r -> r
                .part("checkbox-box", b -> b.style(s -> s.size(10).noShrink().radius(3).border(1, BORDER_STRONG).bg(SURFACE).center()
                        .transition(Tr.colors(FAST), Tr.transform(0.16f, OUT))))
                .partCls("check-mark", m -> m.style(s -> s.fontSize(8).stroke(3).fg(ACCENT_TEXT).opacity(0).scale(0.4f)
                        .transition(Tr.opacity(FAST), Tr.transform(0.26f, POP))))
                .when(HOVER, h -> h.partStyle("checkbox-box", s -> s.borderColor(ACCENT)))
                .when(ACTIVE, a -> a.partStyle("checkbox-box", s -> s.scale(0.88f)))
                .when(CHECKED, c -> c
                        .partStyle("checkbox-box", s -> s.bg(ACCENT).borderColor(ACCENT))
                        .partCls("check-mark", m -> m.style(s -> s.opacity(1).untransformed())))
                .when(INDETERMINATE, c -> c
                        .partStyle("checkbox-box", s -> s.bg(ACCENT).borderColor(ACCENT))
                        .partCls("check-mark", m -> m.style(s -> s.opacity(1).untransformed())))
                .when(FOCUS_VISIBLE, f -> f.partStyle("checkbox-box", Mixins.FOCUS_RING)));

        sheet.on("switch", r -> r
                .partStyle("switch-track", s -> s.size(20, 11).noShrink().round().bg(SURFACE_3).border(1, BORDER_STRONG).pad(1)
                        .transition(Tr.colors(0.18f)))
                .partStyle("switch-knob", s -> s.size(7).round().bg(TEXT_DIM).shadow(0, 1, 2, alpha(SHADOW_COLOR, 0.5f))
                        .transition(Tr.transform(0.26f, BOUNCE), Tr.colors(0.18f), Tr.size(0.16f, OUT)))
                .when(ACTIVE, a -> a.partStyle("switch-knob", s -> s.w(9)))
                .when(CHECKED, c -> c
                        .partStyle("switch-track", s -> s.bg(ACCENT).borderColor(ACCENT))
                        .partStyle("switch-knob", s -> s.translateX(9).bg(Color.WHITE)))
                .refine(sel -> sel.checked().active(), c -> c.partStyle("switch-knob", s -> s.translateX(7)))
                .when(FOCUS_VISIBLE, f -> f.partStyle("switch-track", Mixins.FOCUS_RING)));

        sheet.style("radio-group", s -> s.col().gap(4));
        sheet.on("radio", r -> r
                .partStyle("radio-dot", s -> s.size(10).noShrink().round().border(1, BORDER_STRONG).bg(SURFACE).center()
                        .transition(Tr.colors(FAST)))
                .partStyle("radio-inner", s -> s.size(4).round().bg(ACCENT).scale(0).transition(Tr.transform(0.24f, POP)))
                .when(HOVER, h -> h.partStyle("radio-dot", s -> s.borderColor(ACCENT)))
                .when(CHECKED, c -> c
                        .partStyle("radio-dot", s -> s.borderColor(ACCENT))
                        .partStyle("radio-inner", s -> s.untransformed()))
                .when(FOCUS_VISIBLE, f -> f.partStyle("radio-dot", Mixins.FOCUS_RING)));
    }

    // ------------------------------------------------------------------ slider, progress, spinner

    private static void sliders(Sheet sheet) {
        sheet.on("slider", r -> r
                .style(s -> s.h(12).minW(60).flex().itemsCenter().padX(5).pointer())
                .partStyle("slider-track", s -> s.grow().h(3).radius(2).bg(SURFACE_3))
                .partStyle("slider-fill", s -> s.fullH().radius(2).gradient(90, ACCENT, ACCENT_2))
                .partStyle("slider-thumb", s -> s.abs().top(1).size(10).round().bg(Color.WHITE)
                        .shadow(0, 1, 4, alpha(SHADOW_COLOR, 0.65f)).transition(Tr.transform(0.18f, OUT), Tr.shadow(0.18f)))
                .partCls("slider-value", v -> v.style(s -> s.abs().bottom(13).left(-6).minW(22).pad(1, 3).radius(3)
                        .bg(SURFACE_3).border(1, BORDER_STRONG).fontSize(6.5f).textCenter().fg(TEXT)
                        .opacity(0).translate(0, 3, 0.85f).transition(Tr.opacity(FAST), Tr.transform(0.18f, OUT))))
                .when(HOVER, h -> h
                        .partStyle("slider-thumb", s -> s.scale(1.12f).ring(3, ACCENT_SOFT))
                        .partCls("slider-value", v -> v.style(s -> s.opacity(1).untransformed())))
                .when(DRAGGING, d -> d
                        .partStyle("slider-thumb", s -> s.scale(1.25f).ring(4, ACCENT_SOFT))
                        .partCls("slider-value", v -> v.style(s -> s.opacity(1).untransformed())))
                .when(FOCUS_VISIBLE, f -> f.partStyle("slider-thumb", Mixins.FOCUS_RING)));

        sheet.on("progress", r -> r
                .style(s -> s.h(4).minW(40).radius(2).bg(SURFACE_3).clip())
                .partStyle("progress-fill", s -> s.fullH().radius(2).gradient(90, ACCENT, ACCENT_2).transition(Tr.size(0.45f, OUT)))
                .when("complete", c -> c.partStyle("progress-fill", s -> s.bg(SUCCESS)))
                .when(INDETERMINATE, c -> c.partStyle("progress-fill", s -> s.wPct(35)
                        .animation(SWEEP.play(1.3f).easing(Easings.EASE_IN_OUT).infinite()))));

        sheet.style("spinner", s -> s.size(14).fg(ACCENT).stroke(2).borderColor(BORDER));
    }

    // ------------------------------------------------------------------ text field

    private static void fields(Sheet sheet) {
        sheet.on("textfield", r -> r
                .style(s -> s.minW(60).h(16).padX(6).radius(RADIUS).bg(SURFACE).border(1, BORDER_STRONG)
                        .cursor(me.methodial.apothecary.style.Cursor.TEXT).transition(Tr.colors(FAST), Tr.shadow(NORMAL)))
                .hover(s -> s.borderColor(ACCENT_SOFT))
                .focus(s -> s.borderColor(ACCENT).ring(2, ACCENT_SOFT).bg(FIELD_FOCUS_BG))
                .variantStyle("invalid", s -> s.borderColor(DANGER).ring(2, alpha(DANGER, 0.2f))));
        sheet.rule(cls("textarea"), s -> s.h(Length.AUTO).padY(4));
    }

    // ------------------------------------------------------------------ select & menus

    private static void menus(Sheet sheet) {
        sheet.on("select", r -> r
                .style(s -> s.row().gap(4).h(16).pad(0, 5, 0, 7).minW(70).radius(RADIUS).bg(SURFACE_3).border(1, BORDER_STRONG)
                        .pointer().transition(Tr.colors(FAST)))
                .hover(s -> s.bg(SURFACE_HOVER))
                .open(s -> s.borderColor(ACCENT))
                .focusVisible(Mixins.FOCUS_RING)
                .partCls("select-label", l -> l.style(s -> s.grow().apply(Mixins.TRUNCATE)))
                .partCls("select-arrow", a -> a.style(s -> s.fontSize(8).fg(TEXT_DIM).transition(Tr.transform(0.22f, OUT))))
                .when("placeholder", p -> p.partCls("select-label", l -> l.style(s -> s.fg(TEXT_FAINT))))
                .when(OPEN, o -> o.partCls("select-arrow", a -> a.style(s -> s.rotate(180)))));

        sheet.on("menu", r -> r
                .style(s -> s.col().pad(3).minW(80).maxH(160).scrollY().apply(Mixins.PANEL)
                        .animation(POP_IN.play(0.16f).easing(OUT)).origin(0.5f, 0)
                        .transition(Tr.opacity(FAST), Tr.transform(0.15f)))
                .closing(s -> s.opacity(0).translate(0, -3, 0.96f)));
        sheet.on("menu-item", r -> r
                .style(s -> s.row().gap(5).pad(3, 6).radius(RADIUS_SM).pointer().transition(Tr.colors(0.08f)))
                .state(HIGHLIGHTED, s -> s.bg(PRESS_TINT))
                .selected(s -> s.fg(ACCENT_HOVER))
                .when(SELECTED, sel -> sel.partCls("menu-label", l -> l.style(s -> s.semibold()))));
        sheet.rule(cls("menu-icon"), s -> s.fontSize(8).fg(TEXT_DIM));
        sheet.rule(cls("menu-label"), s -> s.grow().nowrap());
        sheet.rule(cls("menu-shortcut"), s -> s.fg(TEXT_FAINT).fontSize(6.5f).ml(10));
        sheet.style("menu-separator", s -> s.h(1).m(3, 2).bg(BORDER));
        sheet.rule(cls("menu-header"), s -> s.pad(3, 6, 1, 6).type(TypeScale.CAPS).fontSize(6));
    }

    // ------------------------------------------------------------------ tabs & segmented

    private static void tabs(Sheet sheet) {
        sheet.style("tabs", s -> s.col().gap(6));
        sheet.style("tab-bar", s -> s.row().gap(2).borderBottom(1, BORDER));
        sheet.on("tab", r -> r
                .style(s -> s.row().gap(3).pad(4, 8, 5, 8).fg(TEXT_DIM).pointer().transition(Tr.colors(FAST)))
                .hover(s -> s.fg(TEXT))
                .selected(s -> s.fg(TEXT)));
        sheet.rule(cls("tab-icon"), s -> s.fontSize(8));
        sheet.style("tab-indicator", s -> s.bottom(-1).h(2).radius(1).bg(ACCENT).z(1));
        sheet.style("tab-panel", s -> s.col().grow());
        sheet.rule(cls("enter").withCls("from-right").childOf(is("tab-panel")), s -> s.animation(SLIDE_FROM_RIGHT.play(0.26f).easing(OUT)));
        sheet.rule(cls("enter").withCls("from-left").childOf(is("tab-panel")), s -> s.animation(SLIDE_FROM_LEFT.play(0.26f).easing(OUT)));

        sheet.style("segmented", s -> s.row().pad(2).radius(RADIUS).bg(SURFACE).border(1, BORDER));
        sheet.style("segment-pill", s -> s.radius(3).bg(SURFACE_3).shadow(SHADOW_SM).border(1, BORDER_STRONG));
        sheet.on("segment", r -> r
                .style(s -> s.pad(2, 7).fg(TEXT_DIM).pointer().transition(Tr.colors(FAST)))
                .hover(s -> s.fg(TEXT))
                .selected(s -> s.fg(TEXT)));
        sheet.rule(cls("segment-label"), s -> s.nowrap().fontSize(7));
    }

    // ------------------------------------------------------------------ accordion & stepper

    private static void disclosure(Sheet sheet) {
        sheet.style("accordion", s -> s.col().border(1, BORDER).radius(RADIUS).bg(SURFACE));
        sheet.on("accordion-header", r -> r
                .style(s -> s.row().pad(5, 7).pointer().radius(RADIUS).transition(Tr.colors(FAST)))
                .hover(s -> s.bg(HOVER_TINT)));
        sheet.rule(cls("accordion-title"), s -> s.grow().medium());
        sheet.rule(cls("accordion-arrow"), s -> s.fontSize(8).fg(TEXT_DIM).transition(Tr.transform(0.26f, OUT)));
        sheet.rule(cls("accordion-arrow").in(is("accordion").open()), s -> s.rotate(180));
        sheet.style("accordion-content", s -> s.col().gap(4).pad(0, 7, 7, 7).fg(TEXT_DIM));

        sheet.style("stepper", s -> s.row().border(1, BORDER_STRONG).radius(RADIUS).bg(SURFACE).pad(1));
        sheet.rule(cls("stepper-value"), s -> s.minW(18).textCenter().font(MONO).fontSize(7.5f));
    }

    // ------------------------------------------------------------------ tooltips, dialogs, toasts

    private static void overlays(Sheet sheet) {
        sheet.on("tooltip", r -> r
                .style(s -> s.pad(3, 6).maxW(160).radius(RADIUS_SM).bg(TOOLTIP_BG).border(1, BORDER_STRONG).shadow(SHADOW)
                        .fontSize(7).fg(TEXT).animation(TOOLTIP_IN.play(0.14f).easing(OUT)).transition(Tr.opacity(0.09f)))
                .closing(s -> s.opacity(0)));

        sheet.on(cls("modal-backdrop"), r -> r
                .style(s -> s.bg(BACKDROP).animation(FADE_IN.play(0.18f)).transition(Tr.opacity(0.15f)))
                .closing(s -> s.opacity(0))
                .when(CLOSING, c -> c.partStyle("dialog", s -> s.scale(0.95f).opacity(0))));
        sheet.style("dialog", s -> s.col().w(220).maxW(Length.pct(90)).bg(SURFACE_2).border(1, BORDER_STRONG).radius(RADIUS_LG)
                .shadow(SHADOW_LG).animation(DIALOG_IN.play(0.26f).easing(ENTER)).transition(Tr.transform(0.15f), Tr.opacity(0.15f)));
        sheet.style("dialog-header", s -> s.row().gap(5).pad(7, 7, 3, 10));
        sheet.rule(cls("dialog-icon"), s -> s.fontSize(11).fg(ACCENT));
        sheet.rule(cls("dialog-title"), s -> s.grow().fontSize(10).semibold());
        sheet.style("dialog-body", s -> s.col().gap(6).pad(3, 10, 10, 10));
        sheet.rule(cls("dialog-text"), s -> s.fg(TEXT_DIM));
        sheet.style("dialog-footer", s -> s.row().justify(Justify.END).gap(4).pad(7, 10).borderTop(1, BORDER)
                .bg(alpha(SHADOW_COLOR, 0.13f)).radiusBottom(RADIUS_LG));

        sheet.on("toast", r -> r
                .style(s -> s.row().itemsStart().gap(6).w(160).pad(7, 6, 8, 8).radius(RADIUS_LG).bg(SURFACE_2).border(1, BORDER_STRONG)
                        .shadow(SHADOW_LG).clip().animation(TOAST_IN.play(0.38f).easing(ENTER))
                        .transition(Tr.opacity(0.22f), Tr.transform(0.25f, OUT)))
                .closing(s -> s.opacity(0).translateX(30)));
        sheet.style("toast-text", s -> s.col().gap(1).grow().shrink(1));
        sheet.rule(cls("toast-title"), s -> s.semibold());
        sheet.rule(cls("toast-message"), s -> s.fg(TEXT_DIM).fontSize(7));
        sheet.rule(cls("toast-icon"), s -> s.fontSize(10).noShrink());
        sheet.rule(cls("toast-close"), s -> s.mt(-3).mr(-2));
        sheet.style("toast-timer", s -> s.abs().left(8).right(8).bottom(3).h(2).radius(1).bg(TEXT_DIM).opacity(0.7f).origin(0, 0.5f));
        for (String kind : new String[]{"info", "success", "warning", "error"}) {
            Token<Color> c = switch (kind) { case "success" -> SUCCESS; case "warning" -> WARNING; case "error" -> DANGER; default -> INFO; };
            sheet.on(is("toast").withCls(kind), r -> r
                    .partCls("toast-icon", i -> i.style(s -> s.fg(c)))
                    .partStyle("toast-timer", s -> s.bg(c)));
        }
    }

    // ------------------------------------------------------------------ color picker

    private static void pickers(Sheet sheet) {
        sheet.style("color-picker", s -> s.col().gap(5).w(120));
        sheet.rule(cls("picker-sv"), s -> s.h(80).radius(RADIUS).border(1, BORDER_STRONG));
        sheet.style("picker-knob", s -> s.abs().size(8).m(-4, 0, 0, -4).round().border(2, Color.WHITE)
                .shadow(0, 1, 3, alpha(SHADOW_COLOR, 0.65f)).passThrough());
        sheet.rule(cls("picker-hue"), s -> s.h(8).round().border(1, BORDER_STRONG));
        sheet.rule(cls("picker-alpha"), s -> s.h(8).round().border(1, BORDER_STRONG));
        sheet.style("picker-bar", s -> s.abs().top(-2).size(4, 10).ml(-2).radius(2).bg(Color.WHITE)
                .ring(1, alpha(SHADOW_COLOR, 0.55f)).passThrough());
        sheet.style("picker-row", s -> s.row().gap(4));
        sheet.style("picker-swatch", s -> s.size(16).radius(RADIUS_SM).border(1, BORDER_STRONG));
        sheet.rule(is("textfield").in(is("picker-row")), s -> s.grow().font(MONO).fontSize(7));
    }

    // ------------------------------------------------------------------ Minecraft nodes (backends provide them)

    private static void minecraft(Sheet sheet) {
        sheet.style("item", s -> s.size(16));
        sheet.on("item-slot", r -> r
                .style(s -> s.size(20).pad(2).bg(alpha(SHADOW_COLOR, 0.3f)).border(1, BORDER).radius(RADIUS_SM)
                        .transition(Tr.colors(0.1f), Tr.transform(0.16f, OUT)))
                .hover(s -> s.bg(HOVER_TINT).borderColor(ACCENT).scale(1.08f)));
    }
}
