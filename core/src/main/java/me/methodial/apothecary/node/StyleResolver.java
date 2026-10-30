package me.methodial.apothecary.node;

import me.methodial.apothecary.anim.Easing;
import me.methodial.apothecary.sheet.Sheet;
import me.methodial.apothecary.style.Keyframes;
import me.methodial.apothecary.style.Keyword;
import me.methodial.apothecary.style.TokenMap;
import me.methodial.apothecary.theme.Ref;
import me.methodial.apothecary.theme.TokenScope;
import me.methodial.apothecary.style.AnimationSpec;
import me.methodial.apothecary.style.ComputedStyle;
import me.methodial.apothecary.style.Prop;
import me.methodial.apothecary.style.Props;
import me.methodial.apothecary.style.Style;
import me.methodial.apothecary.style.TransitionSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The cascade: matches rules, resolves var(), inherits, then layers transitions, keyframe animations
 * and programmatic animations on top to produce what is displayed.
 */
final class StyleResolver {
    private StyleResolver() { }

    private record Match(int specificity, int sheet, int order, Style style) { }

    /** A running per-property transition. */
    static final class PropTransition {
        final Prop<?> prop;
        final Object from, to;
        final float start, duration;
        final Easing easing;

        PropTransition(Prop<?> prop, Object from, Object to, float start, float duration, Easing easing) {
            this.prop = prop;
            this.from = from;
            this.to = to;
            this.start = start;
            this.duration = duration;
            this.easing = easing;
        }

        Object sample(float now) {
            float t = (now - start) / duration;
            if (t <= 0) return from;
            if (t >= 1) return to;
            return prop.interpolate(from, to, easing.apply(t));
        }

        boolean done(float now) { return now - start >= duration; }
    }

    /** A running keyframe animation, frames resolved for one element. */
    static final class RunningKeyframes {
        final AnimationSpec spec;
        final float start;
        final List<float[]> offsets = new ArrayList<>();          // per frame: {offset}
        final List<Map<Integer, Object>> frames = new ArrayList<>();
        boolean finished;

        RunningKeyframes(AnimationSpec spec, float start) {
            this.spec = spec;
            this.start = start;
        }
    }

    /** A programmatic animation of one property (Node#animate). */
    static final class PropTween {
        final Prop<?> prop;
        Object from;
        final Object to;
        final float start, duration;
        final Easing easing;
        final Runnable onDone;

        PropTween(Prop<?> prop, Object from, Object to, float start, float duration, Easing easing, Runnable onDone) {
            this.prop = prop;
            this.from = from;
            this.to = to;
            this.start = start;
            this.duration = Math.max(1e-4f, duration);
            this.easing = easing;
            this.onDone = onDone;
        }
    }

    // ------------------------------------------------------------------ cascade

    static ComputedStyle cascade(Node n, List<Sheet> sheets, float rootW, float rootH) {
        Node parent = n.parent();
        ComputedStyle pc = parent != null ? parent.computed() : null;

        List<Match> matches = new ArrayList<>();
        for (int si = 0; si < sheets.size(); si++) {
            for (Sheet.Rule r : sheets.get(si).rules()) {
                if (r.media() != null && !r.media().matches(rootW, rootH)) continue;
                String kt = r.selector().keyType();
                if (kt != null && !kt.equals(n.typeName())) continue;
                if (r.selector().matches(n)) matches.add(new Match(r.selector().specificity(), si, r.order(), r.style()));
            }
        }
        matches.sort((a, b) -> a.specificity != b.specificity ? Integer.compare(a.specificity, b.specificity)
                : a.sheet != b.sheet ? Integer.compare(a.sheet, b.sheet) : Integer.compare(a.order, b.order));

        // Layers, lowest priority first: base style, sheet rules, inline style, inline state styles.
        List<Style> layers = new ArrayList<>(matches.size() + 4);
        layers.add(n.baseStyle());
        for (Match m : matches) layers.add(m.style);
        layers.add(n.style());
        for (Map.Entry<String, Style> e : n.stateStyleMap().entrySet()) {
            if (n.hasState(e.getKey())) layers.add(e.getValue());
        }

        // Token scope first: values set by any layer apply to this element and its subtree.
        Map<me.methodial.apothecary.theme.Token<?>, Object> own = new LinkedHashMap<>();
        for (Style s : layers) own.putAll(s.tokens());
        TokenMap tokens = (pc != null ? pc.tokens() : TokenMap.EMPTY).with(own);

        Object[] spec = new Object[Props.count()];
        for (Style s : layers) apply(spec, s, tokens, n);

        ComputedStyle cs = new ComputedStyle();
        cs.setTokens(tokens);
        for (Prop<?> p : Props.all()) {
            int id = p.id();
            Object v = spec[id];
            if (v == Keyword.INHERIT) v = pc != null ? pc.raw(id) : p.initial();
            else if (v == Keyword.INITIAL) v = p.initial();
            else if (v == null) {
                v = p.inherited() && pc != null ? pc.raw(id) : p.initial();
                cs.setRaw(id, v);
                continue;
            }
            cs.setRaw(id, v);
            cs.markSpecified(id);
        }
        return cs;
    }

    /** Copies a layer's values into {@code spec}, resolving token references for this element. */
    private static void apply(Object[] spec, Style st, TokenScope tokens, Node n) {
        for (Map.Entry<Prop<?>, Object> e : st.values().entrySet()) {
            Object v = e.getValue();
            if (v instanceof Ref<?> r) {
                try {
                    v = r.resolve(tokens);
                } catch (RuntimeException ex) {
                    n.warnOnce(e.getKey().name() + ": " + ex);
                    continue;
                }
                if (v == null) continue;
            }
            spec[e.getKey().id()] = v;
        }
    }

    // ------------------------------------------------------------------ transitions & animations

    /** Starts transitions for every animatable property whose target changed. */
    static void startTransitions(Node n, ComputedStyle oldTarget, ComputedStyle newTarget, ComputedStyle displayed, float now) {
        List<TransitionSpec> specs = newTarget.get(Props.TRANSITION);
        for (Prop<?> p : Props.all()) {
            int id = p.id();
            if (Objects.equals(oldTarget.raw(id), newTarget.raw(id))) continue;
            // Inherited values that this element doesn't set follow the parent's (possibly animating)
            // value every frame; transitioning them too would freeze them at a mid-animation snapshot.
            if (p.inherited() && !newTarget.isSpecified(p)) {
                n.transitions.remove(id);
                continue;
            }
            TransitionSpec match = null;
            if (p.animatable()) {
                for (TransitionSpec s : specs) if (s.matches(p)) match = s;
            }
            if (match == null || match.duration() <= 0) {
                n.transitions.remove(id);
                continue;
            }
            Object from = displayed != null ? displayed.raw(id) : oldTarget.raw(id);
            n.transitions.put(id, new PropTransition(p, from, newTarget.raw(id), now + match.delay(), match.duration(), match.easing()));
        }
    }

    /** Syncs running keyframe animations with the {@code animation} property. */
    static void syncKeyframes(Node n, ComputedStyle target, float now) {
        List<AnimationSpec> specs = target.get(Props.ANIMATION);
        Map<String, RunningKeyframes> old = new LinkedHashMap<>();
        for (RunningKeyframes r : n.keyframes) old.put(r.spec.key(), r);
        n.keyframes.clear();
        for (AnimationSpec s : specs) {
            RunningKeyframes existing = old.get(s.key());
            if (existing != null) { n.keyframes.add(existing); continue; }
            RunningKeyframes r = new RunningKeyframes(s, now);
            for (Keyframes.Frame f : s.keyframes().frames()) {
                Object[] spec = new Object[Props.count()];
                apply(spec, f.style(), target.tokens(), n);
                Map<Integer, Object> values = new HashMap<>();
                for (int i = 0; i < spec.length; i++) if (spec[i] != null && !(spec[i] instanceof Keyword)) values.put(i, spec[i]);
                r.offsets.add(new float[]{f.offset()});
                r.frames.add(values);
            }
            n.keyframes.add(r);
        }
    }

    /** Writes keyframe values for time {@code now} into {@code out}. Returns false once every animation finished. */
    static boolean sampleKeyframes(Node n, ComputedStyle base, ComputedStyle out, float now) {
        boolean anyRunning = false;
        for (RunningKeyframes r : n.keyframes) {
            AnimationSpec s = r.spec;
            float local = now - r.start - s.delay();
            float dur = Math.max(1e-4f, s.duration());
            float progress;
            boolean fillB = s.fillMode() == AnimationSpec.FillMode.BACKWARDS || s.fillMode() == AnimationSpec.FillMode.BOTH;
            boolean fillF = s.fillMode() == AnimationSpec.FillMode.FORWARDS || s.fillMode() == AnimationSpec.FillMode.BOTH;
            if (s.paused()) {
                progress = 0;
            } else if (local < 0) {
                if (!fillB) { anyRunning = true; continue; }
                progress = 0;
                anyRunning = true;
            } else {
                float iterTotal = s.iterations();
                float iterF = local / dur;
                if (iterF >= iterTotal) {
                    r.finished = true;
                    if (!fillF) continue;
                    float end = iterTotal;
                    int last = (int) Math.ceil(end) - 1;
                    progress = end - Math.max(0, last);
                    if (progress == 0) progress = 1;
                    progress = directed(s.direction(), last, progress);
                } else {
                    int iter = (int) Math.floor(iterF);
                    progress = directed(s.direction(), iter, iterF - iter);
                    anyRunning = true;
                }
            }
            applyFrames(r, base, out, progress);
        }
        return anyRunning;
    }

    private static float directed(AnimationSpec.Direction d, int iter, float p) {
        return switch (d) {
            case NORMAL -> p;
            case REVERSE -> 1 - p;
            case ALTERNATE -> iter % 2 == 0 ? p : 1 - p;
            case ALTERNATE_REVERSE -> iter % 2 == 0 ? 1 - p : p;
        };
    }

    private static void applyFrames(RunningKeyframes r, ComputedStyle base, ComputedStyle out, float p) {
        java.util.Set<Integer> props = new java.util.HashSet<>();
        for (Map<Integer, Object> f : r.frames) props.addAll(f.keySet());
        for (int id : props) {
            Prop<?> prop = Props.all().get(id);
            // Find the frames around p that set this property; missing ends use the element's own value.
            float o0 = 0, o1 = 1;
            Object v0 = base.raw(id), v1 = base.raw(id);
            for (int i = 0; i < r.frames.size(); i++) {
                Object v = r.frames.get(i).get(id);
                if (v == null) continue;
                float off = r.offsets.get(i)[0];
                if (off <= p) { o0 = off; v0 = v; }
            }
            for (int i = r.frames.size() - 1; i >= 0; i--) {
                Object v = r.frames.get(i).get(id);
                if (v == null) continue;
                float off = r.offsets.get(i)[0];
                if (off >= p) { o1 = off; v1 = v; }
            }
            float span = o1 - o0;
            float t = span <= 0 ? 1 : (p - o0) / span;
            out.setRaw(id, prop.interpolate(v0, v1, r.spec.easing().apply(t)));
        }
    }

    /** Applies running transitions; removes finished ones. Returns true if any remain. */
    static boolean sampleTransitions(Node n, ComputedStyle out, float now) {
        Iterator<Map.Entry<Integer, PropTransition>> it = n.transitions.entrySet().iterator();
        while (it.hasNext()) {
            PropTransition t = it.next().getValue();
            out.setRaw(t.prop.id(), t.sample(now));
            if (t.done(now)) it.remove();
        }
        return !n.transitions.isEmpty();
    }

    static boolean sampleTweens(Node n, ComputedStyle out, float now) {
        Iterator<PropTween> it = n.tweens.iterator();
        List<Runnable> done = new ArrayList<>();
        while (it.hasNext()) {
            PropTween tw = it.next();
            float t = Math.max(0, Math.min(1, (now - tw.start) / tw.duration));
            if (tw.from == null) tw.from = out.raw(tw.prop.id());
            out.setRaw(tw.prop.id(), tw.prop.interpolate(tw.from, tw.to, tw.easing.apply(t)));
            if (t >= 1) {
                it.remove();
                if (tw.onDone != null) done.add(tw.onDone);
            }
        }
        done.forEach(Runnable::run);
        return !n.tweens.isEmpty();
    }
}
