package me.methodial.apothecary;

import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.ImageRef;
import me.methodial.apothecary.render.Platform;
import me.methodial.apothecary.render.ShaderParams;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.TextDecoration;

import java.util.ArrayDeque;
import java.util.Deque;

/** A canvas that draws nothing but keeps the transform, so a painted tree can be hit-tested (pointer tests). */
final class TestCanvas implements Canvas {
    private final TextMeasurer text = Platform.headless().text();
    private final Deque<Affine> saved = new ArrayDeque<>();
    private final Deque<Float> opacities = new ArrayDeque<>();
    private Affine matrix = Affine.IDENTITY;
    private float opacity = 1;

    @Override public void save() { saved.push(matrix); }
    @Override public void restore() { matrix = saved.isEmpty() ? Affine.IDENTITY : saved.pop(); }
    @Override public void transform(Affine m) { matrix = matrix.multiply(m); }
    @Override public Affine matrix() { return matrix; }
    @Override public void pushClip(Rect r) { }
    @Override public void popClip() { }
    @Override public Rect clipBounds() { return null; }
    @Override public void pushOpacity(float alpha) { opacities.push(opacity); opacity *= alpha; }
    @Override public void popOpacity() { opacity = opacities.isEmpty() ? 1 : opacities.pop(); }
    @Override public float opacity() { return opacity; }
    @Override public void drawBox(Rect r, Corners radii, Paint fill, float borderWidth, Color borderColor) { }
    @Override public void drawShadow(Rect r, Corners radii, Shadow shadow) { }
    @Override public void drawLine(float x0, float y0, float x1, float y1, float width, Color color) { }
    @Override public void drawArc(float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg, Color color) { }
    @Override public TextMeasurer text() { return text; }
    @Override public void drawText(String s, float x, float y, FontSpec font, Color color, boolean shadow, TextDecoration decoration) { }
    @Override public void drawImage(ImageRef image, Rect dst, float u0, float v0, float u1, float v1, Color tint) { }
    @Override public void drawShader(String shaderId, Rect r, ShaderParams params) { }
    @Override public boolean hasShader(String shaderId) { return false; }
    @Override public <T> T backend(Class<T> type) { return null; }
}
