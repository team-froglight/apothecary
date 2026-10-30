package me.methodial.apothecary.mc.showcase;

import me.methodial.apothecary.anim.Easings;
import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Justify;
import me.methodial.apothecary.style.Keyframes;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.TrackSize;
import me.methodial.apothecary.style.Tr;
import me.methodial.apothecary.theme.Colors;
import me.methodial.apothecary.theme.Mixins;
import me.methodial.apothecary.theme.Token;
import me.methodial.apothecary.theme.TypeScale;

import java.util.List;

import static me.methodial.apothecary.sheet.Selector.*;
import static me.methodial.apothecary.theme.Colors.alpha;
import static me.methodial.apothecary.theme.Motion.*;
import static me.methodial.apothecary.theme.Tokens.*;

/** The showcase app's own look, layered over the default theme. */
final class ShowcaseStyles {
    private ShowcaseStyles() { }

    /** App chrome derived from the theme, so every palette (light ones too) gets matching chrome for free. */
    static final Token<Color> APP_GLOW = Token.derived("app-glow", t -> t.get(BG).lerp(t.get(ACCENT), 0.12f));
    static final Token<Color> SIDEBAR_BG = Token.derived("sidebar-bg", t -> t.get(BG).lighten(-0.08f).withAlpha(0.85f));
    static final Token<Color> TOPBAR_BG = Token.derived("topbar-bg", t -> t.get(BG).withAlpha(0.63f));

    /** Bright tile colors used by the layout demos. */
    static final String[] PALETTE = {"#ff7a90", "#ffb86b", "#ffe073", "#7ef0a8", "#6fd8ff", "#8b9dff", "#c48bff", "#ff8be0"};

    static final Keyframes LOGO_GLOW = Keyframes.of("logo-glow")
            .from(s -> s.shadow(0, 2, 8, alpha(ACCENT, 0.27f)))
            .to(s -> s.shadow(0, 2, 16, alpha(ACCENT_2, 0.53f)));
    static final Keyframes PAGE_IN = Keyframes.of("page-in").from(s -> s.opacity(0).translateY(8)).to(s -> s.opacity(1).untransformed());
    static final Keyframes SHAKE = Keyframes.of("shake")
            .at(0, s -> s.untransformed()).at(0.2f, s -> s.translateRotate(-4, 0, -6)).at(0.4f, s -> s.translateRotate(4, 0, 6))
            .at(0.6f, s -> s.translateRotate(-3, 0, -3)).at(0.8f, s -> s.translateRotate(3, 0, 3)).at(1, s -> s.untransformed());
    static final Keyframes WOBBLE = Keyframes.of("wobble")
            .at(0, s -> s.skewX(0)).at(0.25f, s -> s.skewX(-15)).at(0.5f, s -> s.skewX(10)).at(0.75f, s -> s.skewX(-5)).at(1, s -> s.skewX(0));
    static final Keyframes HUE = Keyframes.of("hue")
            .at(0, s -> s.fg(Color.hex("#ff6b6b"))).at(0.25f, s -> s.fg(Color.hex("#ffd166"))).at(0.5f, s -> s.fg(Color.hex("#06d6a0")))
            .at(0.75f, s -> s.fg(Color.hex("#4cc9f0"))).at(1, s -> s.fg(Color.hex("#ff6b6b")));
    static final Keyframes FLIP = Keyframes.of("flip").at(0, s -> s.scale(1, 1)).at(0.5f, s -> s.scale(-1, 1)).at(1, s -> s.scale(1, 1));

    static Sheet build() {
        Sheet sheet = new Sheet("apothecary:showcase");

        // ---------------------------------------------------------- app shell
        sheet.style("app", s -> s.row().items(me.methodial.apothecary.style.Align.STRETCH).full()
                .bgPaint(t -> new Paint.Radial(0.8f, 0f, 1f, List.of(new Paint.Stop(t.get(APP_GLOW), 0), new Paint.Stop(t.get(BG), 0.7f)))));
        sheet.style("sidebar", s -> s.col().w(118).noShrink().pad(8, 6).gap(2).bg(SIDEBAR_BG).borderRight(1, BORDER));
        sheet.style("brand", s -> s.row().gap(5).pad(2, 4, 10, 4));
        sheet.on("brand-logo", r -> r
                .style(s -> s.size(18).radius(5).center().gradient(135, ACCENT, ACCENT_2)
                        .animation(LOGO_GLOW.play(3).easing(Easings.EASE_IN_OUT).infinite().alternate()))
                .partStyle("icon", s -> s.fontSize(11).fg(Color.WHITE).stroke(2.4f)));
        sheet.rule(cls("brand-name"), s -> s.fontSize(10).bold());
        sheet.rule(cls("brand-version"), s -> s.fontSize(6).fg(TEXT_FAINT));

        sheet.style("nav", s -> s.col().gap(1).grow().scrollY().scrollbar(2).padRight(5));
        sheet.style("nav-group", s -> s.pad(6, 6, 2, 6).type(TypeScale.CAPS).fontSize(6));
        sheet.on("nav-item", r -> r
                .style(s -> s.row().gap(6).pad(3, 6).radius(RADIUS).fg(TEXT_DIM).pointer().transition(Tr.colors(FAST)))
                .hover(s -> s.fg(TEXT).bg(HOVER_TINT))
                .selected(s -> s.fg(TEXT))
                .partStyle("icon", s -> s.fontSize(9))
                .when(SELECTED, sel -> sel.partStyle("icon", s -> s.fg(ACCENT_HOVER))));
        sheet.style("nav-pill", s -> s.radius(RADIUS).border(1, alpha(ACCENT, 0.2f))
                .bgPaint(t -> Paint.linear(90, t.get(ACCENT).withAlpha(0.16f), t.get(ACCENT).withAlpha(0.04f))));
        sheet.style("sidebar-footer", s -> s.col().gap(5).pad(6, 4, 0, 4).borderTop(1, BORDER));

        sheet.style("main", s -> s.col().grow().shrink(1).minW(0));
        sheet.style("topbar", s -> s.row().gap(6).pad(8, 14).borderBottom(1, BORDER).bg(TOPBAR_BG));
        sheet.rule(cls("page-title"), s -> s.fontSize(12).bold());
        sheet.rule(cls("page-subtitle"), s -> s.fontSize(7).fg(TEXT_DIM));
        sheet.style("page-scroll", s -> s.grow().shrink(1).minH(0).scrollY());
        sheet.style("page", s -> s.col().gap(10).pad(12, 14, 20, 14).animation(PAGE_IN.play(0.32f).easing(OUT)));

        // ---------------------------------------------------------- page building blocks
        sheet.style("section", s -> s.col().gap(6));
        sheet.style("section-head", s -> s.col().gap(1));
        sheet.rule(cls("section-title"), s -> s.fontSize(10).semibold());
        sheet.rule(cls("section-desc"), s -> s.fg(TEXT_DIM).fontSize(7));
        sheet.rule(cls("cards"), s -> s.cols(TrackSize.fill(TrackSize.minmax(TrackSize.px(150), TrackSize.fr(1)))).gap(8));
        sheet.rule(cls("cards-wide"), s -> s.cols(TrackSize.fill(TrackSize.minmax(TrackSize.px(220), TrackSize.fr(1)))).gap(8));
        sheet.rule(cls("card-title"), s -> s.type(TypeScale.TITLE).fg(TEXT));
        sheet.rule(cls("card-desc"), s -> s.fontSize(6.5f).fg(TEXT_FAINT));
        sheet.rule(cls("demo-row"), s -> s.row().wrap().gap(5));
        sheet.rule(cls("demo-col"), s -> s.col().gap(5));
        sheet.rule(cls("control-row"), s -> s.row().gap(6));
        sheet.rule(cls("control-label"), s -> s.w(64).noShrink().fg(TEXT_DIM).fontSize(7));

        // ---------------------------------------------------------- overview
        sheet.on("hero", r -> r
                .style(s -> s.col().justify(Justify.END).gap(6).h(150).pad(16).radius(10).clip()
                        .border(1, alpha(TEXT, 0.11f)).shadow(SHADOW_LG))
                .child(is("shader"), sh -> sh.style(s -> s.cover().radius(10))));
        sheet.style("hero-scrim", s -> s.cover().bgPaint(t -> {
            Color b = t.get(BG);
            return new Paint.Linear(180, List.of(new Paint.Stop(b.withAlpha(0), 0), new Paint.Stop(b.withAlpha(0.4f), 0.45f), new Paint.Stop(b.withAlpha(0.87f), 1)));
        }));
        sheet.rule(cls("hero-title"), s -> s.type(me.methodial.apothecary.theme.TypeScale.DISPLAY).animation(RISE_IN.play(0.6f).easing(OUT).delay(0.1f).backwards()));
        sheet.rule(cls("hero-lead"), s -> s.fontSize(8.5f).fg(Colors.mix(TEXT, ACCENT, 0.15f)).maxW(260)
                .animation(RISE_IN.play(0.6f).easing(OUT).delay(0.22f).backwards()));
        sheet.rule(cls("hero-actions"), s -> s.row().gap(5).mt(4).animation(RISE_IN.play(0.6f).easing(OUT).delay(0.34f).backwards()));
        sheet.rule(is("icon").in(cls("feature")), s -> s.fontSize(13).fg(ACCENT_HOVER));
        sheet.rule(cls("feature-icon"), s -> s.size(22).radius(6).center().bg(ACCENT_SOFT).border(1, alpha(ACCENT, 0.25f)));

        // ---------------------------------------------------------- layout demos
        sheet.style("playground", s -> s.flex().minH(120).pad(6).apply(Mixins.WELL));
        sheet.on("flex-item", r -> r
                .style(s -> s.flex().center().minW(18).minH(14).pad(3, 6).radius(4).fontSize(7).bold().fg(Color.hex("#0b0b12"))
                        .layoutTransition(0.38f, Easings.cubicBezier(0.2f, 1, 0.3f, 1)).transition(Tr.transform(0.16f, OUT)))
                .hover(s -> s.scale(1.08f)));
        sheet.style("grid-demo", s -> s.grid().gap(4));
        sheet.style("grid-cell", s -> s.flex().center().minH(18).pad(3).radius(4).fontSize(7).semibold().fg(Color.hex("#0b0b12"))
                .layoutTransition(0.32f, OUT));

        // ---------------------------------------------------------- motion
        sheet.style("easing-cell", s -> s.col().gap(3).pad(6).radius(RADIUS).bg(SURFACE).border(1, BORDER));
        sheet.style("easing-graph", s -> s.h(40));
        sheet.rule(cls("easing-name"), s -> s.fontSize(6.5f).fg(TEXT_DIM).font(MONO));
        sheet.style("spring-stage", s -> s.h(90).apply(Mixins.WELL).clip());
        sheet.style("spring-ball", s -> s.abs().size(18).round().shadow(0, 4, 12, alpha(SHADOW_COLOR, 0.6f)).cursor(me.methodial.apothecary.style.Cursor.GRAB)
                .bgPaint(t -> new Paint.Radial(0.35f, 0.3f, 1, List.of(new Paint.Stop(Color.WHITE, 0), new Paint.Stop(t.get(ACCENT), 0.6f),
                        new Paint.Stop(t.get(ACCENT_PRESS).lighten(-0.3f), 1)))));
        sheet.on(cls("anim-tile"), r -> r
                .style(s -> s.col().center().gap(4).h(52).radius(RADIUS).bg(SURFACE).border(1, BORDER))
                .partStyle("icon", s -> s.fontSize(14).fg(ACCENT_HOVER)));
        animTile(sheet, "a-pulse", PULSE.play(1.2f).easing(Easings.EASE_IN_OUT).infinite());
        animTile(sheet, "a-spin", SPIN.play(1.6f).easing(Easings.LINEAR).infinite());
        animTile(sheet, "a-bounce", BOUNCE_Y.play(0.9f).easing(Easings.OUT_QUAD).infinite().alternate());
        animTile(sheet, "a-shake", SHAKE.play(0.6f).easing(Easings.EASE_IN_OUT).infinite());
        animTile(sheet, "a-float", FLOAT.play(2.4f).easing(Easings.EASE_IN_OUT).infinite().alternate());
        animTile(sheet, "a-wobble", WOBBLE.play(1.4f).easing(Easings.EASE_IN_OUT).infinite());
        animTile(sheet, "a-hue", HUE.play(3f).easing(Easings.LINEAR).infinite());
        animTile(sheet, "a-flip", FLIP.play(1.6f).easing(Easings.EASE_IN_OUT).infinite());
        sheet.style("stagger-bar", s -> s.h(6).round().gradient(90, ACCENT, ACCENT_2));

        sheet.on(cls("lift"), r -> r
                .style(s -> s.transition(Tr.transform(0.3f, BOUNCE), Tr.shadow(0.3f), Tr.colors(0.2f)))
                .hover(s -> s.transform(me.methodial.apothecary.style.Transform.translate(0, -4).withRotate(-1).withScale(1.03f, 1.03f))
                        .shadow(0, 12, 24, alpha(SHADOW_COLOR, 0.8f)).bg(SURFACE_3)));
        sheet.on(cls("glow"), r -> r
                .style(s -> s.transition(Tr.shadow(0.4f), Tr.colors(0.3f)))
                .hover(s -> s.borderColor(ACCENT).shadow(t -> List.of(new Shadow(0, 0, 0, 1, t.get(ACCENT), false),
                        new Shadow(0, 0, 24, 0, t.get(ACCENT).withAlpha(0.6f), false)))));
        sheet.on(cls("morph"), r -> r
                .style(s -> s.transition(Tr.radius(0.45f, BOUNCE), Tr.colors(0.45f), Tr.transform(0.45f, OUT)))
                .hover(s -> s.radius(24).gradient(135, ACCENT, ACCENT_2).rotate(8)));

        // ---------------------------------------------------------- shaders
        sheet.on("effect-tile", r -> r
                .style(s -> s.col().justify(Justify.END).h(80).pad(6).radius(RADIUS_LG).border(1, alpha(TEXT, 0.1f)).clip()
                        .transition(Tr.transform(0.24f, OUT), Tr.shadow(0.24f)))
                .hover(s -> s.scale(1.02f).shadow(SHADOW_LG))
                .child(is("shader"), sh -> sh.style(s -> s.cover().radius(RADIUS_LG))));
        sheet.rule(cls("effect-name"), s -> s.self(me.methodial.apothecary.style.Align.START).pad(1, 5).radius(6)
                .bg(Color.hex("#000000a0")).fg(Color.WHITE).fontSize(7).semibold());
        sheet.style("sdf-stage", s -> s.flex().center().h(130).apply(Mixins.WELL));

        // ---------------------------------------------------------- minecraft
        sheet.style("inventory", s -> s.cols(TrackSize.repeat(9, TrackSize.px(20))).gap(2));
        sheet.style("doll", s -> s.size(70, 100).radius(RADIUS_LG).border(1, BORDER_STRONG)
                .bgPaint(t -> new Paint.Radial(0.5f, 0.7f, 1, List.of(new Paint.Stop(t.get(ACCENT).withAlpha(0.25f), 0),
                        new Paint.Stop(t.get(SHADOW_COLOR).withAlpha(0.19f), 0.7f)))));

        // ---------------------------------------------------------- theming
        sheet.on("theme-swatch", r -> r
                .style(s -> s.col().gap(4).pad(5).radius(RADIUS).bg(SURFACE).border(1, BORDER).pointer()
                        .transition(Tr.colors(0.15f), Tr.transform(0.2f, OUT)))
                .hover(s -> s.translateY(-2).borderColor(BORDER_STRONG))
                .selected(s -> s.borderColor(ACCENT)));
        sheet.style("swatch-colors", s -> s.row().h(14).radius(3).clip());
        sheet.style("swatch-color", s -> s.grow().fullH());
        sheet.style("code-preview", s -> s.flex().center().h(90).apply(Mixins.WELL));
        return sheet;
    }

    private static void animTile(Sheet sheet, String cls, me.methodial.apothecary.style.AnimationSpec anim) {
        sheet.rule(is("icon").in(cls(cls)), s -> s.animation(anim));
    }
}
