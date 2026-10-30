package me.methodial.apothecary.widget;

import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.node.Node;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.ShaderParams;

import java.util.function.Consumer;

/**
 * Fills its content box with an effect shader registered in the backend (e.g. "apothecary:aurora").
 * {@link #params(Consumer)} updates uniforms every frame. Falls back to the node's background when the
 * backend doesn't know the shader. Type selector: {@code shader}.
 */
public class ShaderView extends Node {
    private String shader;
    private Consumer<ShaderParams> params = p -> { };
    private final ShaderParams values = new ShaderParams();
    private boolean contentBoxOnly;

    public ShaderView(String shaderId) {
        super("shader");
        this.shader = shaderId;
    }

    public String shader() { return shader; }

    public ShaderView shader(String id) { this.shader = id; return this; }

    /** Called before every draw to fill uniforms (time and mouse are set automatically). */
    public ShaderView params(Consumer<ShaderParams> p) { this.params = p; return this; }

    public ShaderView param(int i, float v) { values.params[i] = v; return this; }

    public ShaderView color(int i, me.methodial.apothecary.style.Color c) { values.colors[i] = c; return this; }

    /** Draw inside the padding instead of over the whole border box. */
    public ShaderView contentBoxOnly(boolean b) { this.contentBoxOnly = b; return this; }

    public ShaderParams values() { return values; }

    @Override
    protected void paintContent(Canvas c, Rect content) {
        if (shader == null || !c.hasShader(shader)) return;
        Rect r = contentBoxOnly ? content : new Rect(0, 0, width(), height());
        values.time = root() != null ? root().time() : 0;
        if (root() != null && hasState("hover")) {
            float[] l = toLocal(root().mouseX(), root().mouseY());
            values.mouseX = l[0] - r.x();
            values.mouseY = l[1] - r.y();
        } else {
            values.mouseX = values.mouseY = -1;
        }
        values.radius = Math.min(computed().radius().topLeft(), Math.min(r.width(), r.height()) / 2);
        params.accept(values);
        c.drawShader(shader, r, values);
    }
}
