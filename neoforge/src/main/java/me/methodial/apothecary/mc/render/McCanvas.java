package me.methodial.apothecary.mc.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import me.methodial.apothecary.geom.Affine;
import me.methodial.apothecary.geom.Corners;
import me.methodial.apothecary.geom.Rect;
import me.methodial.apothecary.render.Canvas;
import me.methodial.apothecary.render.FontSpec;
import me.methodial.apothecary.render.ImageRef;
import me.methodial.apothecary.render.ShaderParams;
import me.methodial.apothecary.render.TextMeasurer;
import me.methodial.apothecary.style.Color;
import me.methodial.apothecary.style.Paint;
import me.methodial.apothecary.style.Shadow;
import me.methodial.apothecary.style.TextDecoration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import me.methodial.apothecary.mc.render.text.TtfText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Minecraft 1.21.1 implementation of {@link Canvas} on top of {@link GuiGraphics}. Shapes use the
 * Apothecary SDF shaders; text uses vanilla font rendering with any registered font; images use the
 * texture/sprite pipelines. One instance per frame.
 */
public final class McCanvas implements Canvas {
    private final GuiGraphics g;
    private Affine m = Affine.IDENTITY;
    private final Deque<Affine> stack = new ArrayDeque<>();
    private final Deque<Rect> clips = new ArrayDeque<>();
    private final Deque<Float> opacities = new ArrayDeque<>();
    private float opacity = 1;

    /** Root units → GUI units. The UI tree works in root units; vanilla GUI code works in GUI units. */
    private final float base;
    private Affine devFor, dev;

    public McCanvas(GuiGraphics g) { this(g, 1); }

    public McCanvas(GuiGraphics g, float base) {
        this.g = g;
        this.base = base;
    }

    public GuiGraphics graphics() { return g; }

    /** Factor from root units to GUI units. */
    public float base() { return base; }

    /** The current transform including the root→GUI scale (cached per transform). */
    private Affine dm() {
        if (devFor != m) {
            devFor = m;
            dev = base == 1 ? m : Affine.scaling(base, base).multiply(m);
        }
        return dev;
    }

    /** A local rect's bounds in GUI units (for vanilla calls that take screen coordinates). */
    public Rect toGui(Rect local) { return dm().mapBounds(local); }

    /** Call when done drawing: flushes and releases the scissor. */
    public void finish() {
        g.flush();
        if (!clips.isEmpty()) {
            clips.clear();
            RenderSystem.disableScissor();
        }
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ================================================================== state

    @Override public void save() { stack.push(m); }

    @Override public void restore() { m = stack.isEmpty() ? Affine.IDENTITY : stack.pop(); }

    @Override public void transform(Affine t) { m = m.multiply(t); }

    @Override public Affine matrix() { return m; }

    @Override
    public void pushClip(Rect r) {
        Rect gui = m.mapBounds(r);
        Rect top = clips.peek();
        if (top != null) gui = gui.intersect(top);
        clips.push(gui);
        applyScissor();
    }

    @Override
    public void popClip() {
        if (!clips.isEmpty()) clips.pop();
        applyScissor();
    }

    @Override public Rect clipBounds() { return clips.peek(); }

    private void applyScissor() {
        g.flush();
        Rect r = clips.peek();
        if (r == null) {
            RenderSystem.disableScissor();
            return;
        }
        Window w = Minecraft.getInstance().getWindow();
        double s = w.getGuiScale() * base;
        int x0 = (int) Math.floor(r.x() * s), y0 = (int) Math.floor(r.y() * s);
        int x1 = (int) Math.ceil(r.right() * s), y1 = (int) Math.ceil(r.bottom() * s);
        RenderSystem.enableScissor(x0, w.getHeight() - y1, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    /** Re-applies the clip after vanilla code changed the scissor. */
    public void reapplyClip() { applyScissor(); }

    @Override
    public void pushOpacity(float alpha) {
        opacities.push(opacity);
        opacity *= alpha;
    }

    @Override
    public void popOpacity() { opacity = opacities.isEmpty() ? 1 : opacities.pop(); }

    // ================================================================== groups (offscreen layers)

    /** Offscreen layers reused by nesting depth, sized like the main target. */
    private static final List<TextureTarget> LAYERS = new java.util.ArrayList<>();

    private record Group(RenderTarget target, RenderTarget previous, float alpha, float outerOpacity) { }

    private final Deque<Group> groups = new ArrayDeque<>();

    private RenderTarget boundTarget() {
        return groups.isEmpty() ? Minecraft.getInstance().getMainRenderTarget() : groups.peek().target();
    }

    @Override
    public void beginGroup(float alpha) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget previous = boundTarget();
        int depth = groups.size();
        while (LAYERS.size() <= depth) LAYERS.add(new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX));
        TextureTarget layer = LAYERS.get(depth);
        if (layer.width != main.width || layer.height != main.height) layer.resize(main.width, main.height, Minecraft.ON_OSX);
        g.flush();
        layer.setClearColor(0, 0, 0, 0);
        layer.clear(Minecraft.ON_OSX);
        layer.bindWrite(true);
        groups.push(new Group(layer, previous, alpha * opacity, opacity));
        // Drawn solid inside; the group's alpha applies once, when composited.
        opacity = 1;
        applyScissor();
    }

    @Override
    public void endGroup() {
        if (groups.isEmpty()) return;
        Group grp = groups.pop();
        g.flush();
        grp.previous().bindWrite(true);
        opacity = grp.outerOpacity();
        RenderSystem.disableScissor();
        Window w = Minecraft.getInstance().getWindow();
        float gw = w.getGuiScaledWidth(), gh = w.getGuiScaledHeight();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, grp.target().getColorTextureId());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(1, 1, 1, grp.alpha());
        Matrix4f id = new Matrix4f();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        b.addVertex(id, 0, gh, 0).setUv(0, 0);
        b.addVertex(id, gw, gh, 0).setUv(1, 0);
        b.addVertex(id, gw, 0, 0).setUv(1, 1);
        b.addVertex(id, 0, 0, 0).setUv(0, 1);
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableDepthTest();
        applyScissor();
    }

    // ================================================================== pixel-exact geometry

    @Override
    public Affine deviceMatrix() {
        float gs = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return Affine.scaling(gs, gs).multiply(dm());
    }

    @Override
    public void fillRects(float[] rects, int[] colors, int count) {
        if (count <= 0 || opacity <= 0.001f) return;
        g.flush();
        // Painted back to front, so later fills simply cover earlier ones: no depth test. Vanilla text sits a little in front
        // of 0 and writes depth, so with the test on, a tooltip's box drawn later let earlier text (a stack count) show through.
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f pose = g.pose().last().pose();
        Affine d = dm();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < count; i++) {
            float x0 = rects[i * 4], y0 = rects[i * 4 + 1], x1 = rects[i * 4 + 2], y1 = rects[i * 4 + 3];
            int top = fade(colors[i * 2]), bottom = fade(colors[i * 2 + 1]);
            b.addVertex(pose, d.mapX(x0, y0), d.mapY(x0, y0), 0).setColor(top);
            b.addVertex(pose, d.mapX(x0, y1), d.mapY(x0, y1), 0).setColor(bottom);
            b.addVertex(pose, d.mapX(x1, y1), d.mapY(x1, y1), 0).setColor(bottom);
            b.addVertex(pose, d.mapX(x1, y0), d.mapY(x1, y0), 0).setColor(top);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    private int fade(int argb) {
        if (opacity >= 0.999f) return argb;
        int a = Math.round(((argb >>> 24) & 0xFF) * opacity);
        return (a << 24) | (argb & 0xFFFFFF);
    }

    @Override public float opacity() { return opacity; }

    // ================================================================== geometry helpers

    private static Matrix4f toMatrix(Affine a) {
        return new Matrix4f().m00(a.a()).m01(a.b()).m10(a.c()).m11(a.d()).m30(a.tx()).m31(a.ty());
    }

    private float pad() {
        float s = m.approxScale() * base;
        return s <= 0.0001f ? 1 : Math.max(1f, 1.5f / s);
    }

    /** Begins a custom-shader draw: flushes vanilla batches and sets common state. */
    private boolean begin(ShaderInstance sh) {
        if (sh == null) return false;
        g.flush();
        // As in fillRects: shapes are painted back to front and must cover earlier text, whatever its depth.
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(() -> sh);
        RenderSystem.setShaderColor(1, 1, 1, opacity);
        return true;
    }

    private void end() {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
    }

    /** Emits a quad: local corners (x, y) with matching uv, mapped through the current transform. */
    private void quad(float[] xy, float[] uv) {
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        for (int i = 0; i < 4; i++) {
            float x = xy[i * 2], y = xy[i * 2 + 1];
            b.addVertex(pose, dm().mapX(x, y), dm().mapY(x, y), 0).setUv(uv[i * 2], uv[i * 2 + 1]);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    private void rectQuad(float x0, float y0, float x1, float y1, float ox, float oy) {
        quad(new float[]{x0, y0, x0, y1, x1, y1, x1, y0},
                new float[]{x0 - ox, y0 - oy, x0 - ox, y1 - oy, x1 - ox, y1 - oy, x1 - ox, y0 - oy});
    }

    private static void color(ShaderInstance sh, String name, Color c) {
        sh.safeGetUniform(name).set(c.r(), c.g(), c.b(), c.a());
    }

    // ================================================================== shapes

    // ------------------------------------------------------------------ pixel-exact path for square shapes

    /** Square shapes on an axis-aligned transform are drawn as whole-pixel rects, not antialiased SDF quads. */
    private boolean crisp(Corners radii) {
        Affine d = dm();
        return radii.isZero() && d.b() == 0 && d.c() == 0;
    }

    /** Pending rects for {@link #flushCrisp}: x0, y0, x1, y1 in local units, one ARGB each. */
    private float[] crispRects = new float[64];
    private int[] crispColors = new int[32];
    private int crispCount;

    private void crispRect(float x0, float y0, float x1, float y1, Color c) {
        if (x1 <= x0 || y1 <= y0 || c.isTransparent()) return;
        if (crispCount * 4 + 4 > crispRects.length) {
            crispRects = java.util.Arrays.copyOf(crispRects, crispRects.length * 2);
            crispColors = java.util.Arrays.copyOf(crispColors, crispColors.length * 2);
        }
        // Snap each edge to the device pixel grid: neighbouring rects that share an edge snap to the same pixel.
        Affine dev = deviceMatrix(), inv = dev.invert();
        float ax = Math.round(dev.mapX(x0, y0)), ay = Math.round(dev.mapY(x0, y0));
        float bx = Math.round(dev.mapX(x1, y1)), by = Math.round(dev.mapY(x1, y1));
        if (bx <= ax || by <= ay) return;
        int i = crispCount * 4;
        crispRects[i] = inv.mapX(ax, ay);
        crispRects[i + 1] = inv.mapY(ax, ay);
        crispRects[i + 2] = inv.mapX(bx, by);
        crispRects[i + 3] = inv.mapY(bx, by);
        crispColors[crispCount * 2] = crispColors[crispCount * 2 + 1] = c.argb();
        crispCount++;
    }

    /** The parts of {@code a} outside {@code b} (up to four rects). */
    private void crispSubtract(float ax0, float ay0, float ax1, float ay1, float bx0, float by0, float bx1, float by1, Color c) {
        bx0 = Math.max(bx0, ax0); by0 = Math.max(by0, ay0);
        bx1 = Math.min(bx1, ax1); by1 = Math.min(by1, ay1);
        if (bx1 <= bx0 || by1 <= by0) { crispRect(ax0, ay0, ax1, ay1, c); return; }
        crispRect(ax0, ay0, ax1, by0, c);      // above
        crispRect(ax0, by1, ax1, ay1, c);      // below
        crispRect(ax0, by0, bx0, by1, c);      // left
        crispRect(bx1, by0, ax1, by1, c);      // right
    }

    private void flushCrisp() {
        if (crispCount == 0) return;
        int n = crispCount;
        crispCount = 0;
        fillRects(crispRects, crispColors, n);
    }

    @Override
    public void drawBox(Rect r, Corners radii, Paint fill, float borderWidth, Color borderColor) {
        if (r.width() <= 0 || r.height() <= 0 || opacity <= 0.001f) return;
        if (crisp(radii) && fill instanceof Paint.Solid solid) {
            crispRect(r.x(), r.y(), r.right(), r.bottom(), solid.color());
            float b = borderColor.isTransparent() ? 0 : Math.min(borderWidth, Math.min(r.width(), r.height()) / 2);
            if (b > 0) {
                crispRect(r.x(), r.y(), r.right(), r.y() + b, borderColor);
                crispRect(r.x(), r.bottom() - b, r.right(), r.bottom(), borderColor);
                crispRect(r.x(), r.y() + b, r.x() + b, r.bottom() - b, borderColor);
                crispRect(r.right() - b, r.y() + b, r.right(), r.bottom() - b, borderColor);
            }
            flushCrisp();
            return;
        }
        ShaderInstance sh = ApothecaryShaders.roundedRect;
        if (!begin(sh)) return;
        Corners k = radii.fit(r.width(), r.height());
        sh.safeGetUniform("Size").set(r.width(), r.height());
        sh.safeGetUniform("Radii").set(k.topLeft(), k.topRight(), k.bottomRight(), k.bottomLeft());
        sh.safeGetUniform("BorderWidth").set(borderColor.isTransparent() ? 0f : borderWidth);
        color(sh, "BorderColor", borderColor);
        sh.safeGetUniform("Mode").set(0);
        setPaint(sh, fill, r);
        float p = pad();
        rectQuad(r.x() - p, r.y() - p, r.right() + p, r.bottom() + p, r.x(), r.y());
        end();
    }

    private void setPaint(ShaderInstance sh, Paint paint, Rect r) {
        List<Paint.Stop> stops = paint.stops();
        int n = Math.min(4, stops.size());
        float[] pos = new float[4];
        for (int i = 0; i < 4; i++) {
            Paint.Stop s = stops.get(Math.min(i, stops.size() - 1));
            color(sh, "Color" + i, s.color());
            pos[i] = s.position();
        }
        sh.safeGetUniform("Stops").set(pos[0], pos[1], pos[2], pos[3]);
        sh.safeGetUniform("StopCount").set(n);
        switch (paint) {
            case Paint.Solid s -> sh.safeGetUniform("FillMode").set(0);
            case Paint.Linear l -> {
                double a = Math.toRadians(l.angle());
                float dx = (float) Math.sin(a), dy = (float) -Math.cos(a);
                float len = Math.abs(r.width() * dx) + Math.abs(r.height() * dy);
                sh.safeGetUniform("FillMode").set(1);
                sh.safeGetUniform("Gradient").set(dx, dy, len, 0);
            }
            case Paint.Radial rd -> {
                float cx = rd.cx() * r.width(), cy = rd.cy() * r.height();
                float fx = Math.max(cx, r.width() - cx), fy = Math.max(cy, r.height() - cy);
                float radius = (float) Math.sqrt(fx * fx + fy * fy) * rd.radius();
                sh.safeGetUniform("FillMode").set(2);
                sh.safeGetUniform("Gradient").set(cx, cy, radius, 0);
            }
        }
    }

    @Override
    public void drawShadow(Rect r, Corners radii, Shadow s) {
        if (r.width() <= 0 || r.height() <= 0 || opacity <= 0.001f || s.color().isTransparent()) return;
        if (crisp(radii) && s.blur() <= 0) {
            float ox = s.offsetX(), oy = s.offsetY(), sp = s.spread();
            if (s.inset()) {
                // Inside the box, outside the box shifted by the offset and shrunk by the spread.
                crispSubtract(r.x(), r.y(), r.right(), r.bottom(),
                        r.x() + ox + sp, r.y() + oy + sp, r.right() + ox - sp, r.bottom() + oy - sp, s.color());
            } else {
                // The box grown by the spread and shifted, outside the box itself.
                crispSubtract(r.x() + ox - sp, r.y() + oy - sp, r.right() + ox + sp, r.bottom() + oy + sp,
                        r.x(), r.y(), r.right(), r.bottom(), s.color());
            }
            flushCrisp();
            return;
        }
        ShaderInstance sh = ApothecaryShaders.roundedRect;
        if (!begin(sh)) return;
        Corners k = radii.fit(r.width(), r.height());
        sh.safeGetUniform("Size").set(r.width(), r.height());
        sh.safeGetUniform("Radii").set(k.topLeft(), k.topRight(), k.bottomRight(), k.bottomLeft());
        sh.safeGetUniform("Mode").set(s.inset() ? 2 : 1);
        sh.safeGetUniform("Shadow").set(s.blur(), s.spread(), s.offsetX(), s.offsetY());
        color(sh, "Color0", s.color());
        sh.safeGetUniform("FillMode").set(0);
        sh.safeGetUniform("BorderWidth").set(0f);
        float ext = s.inset() ? pad() : s.blur() * 1.5f + Math.max(0, s.spread()) + Math.max(Math.abs(s.offsetX()), Math.abs(s.offsetY())) + pad();
        rectQuad(r.x() - ext, r.y() - ext, r.right() + ext, r.bottom() + ext, r.x(), r.y());
        end();
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1, float width, Color color) {
        if (opacity <= 0.001f || color.isTransparent()) return;
        ShaderInstance sh = ApothecaryShaders.line;
        if (!begin(sh)) return;
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float ux = len < 1e-4f ? 1 : dx / len, uy = len < 1e-4f ? 0 : dy / len;
        float nx = -uy, ny = ux;
        float hw = width / 2, e = Math.max(hw, 0.5f) + pad();
        sh.safeGetUniform("Length").set(len);
        sh.safeGetUniform("HalfWidth").set(hw);
        color(sh, "Color0", color);
        float ax = x0 - ux * e, ay = y0 - uy * e, bx = x1 + ux * e, by = y1 + uy * e;
        quad(new float[]{ax + nx * e, ay + ny * e, ax - nx * e, ay - ny * e, bx - nx * e, by - ny * e, bx + nx * e, by + ny * e},
                new float[]{-e, e, -e, -e, len + e, -e, len + e, e});
        end();
    }

    @Override
    public void drawArc(float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg, Color color) {
        if (opacity <= 0.001f || color.isTransparent() || sweepDeg <= 0) return;
        ShaderInstance sh = ApothecaryShaders.arc;
        if (!begin(sh)) return;
        sh.safeGetUniform("Radius").set(radius);
        sh.safeGetUniform("Thickness").set(thickness);
        sh.safeGetUniform("Start").set((float) Math.toRadians(((startDeg % 360) + 360) % 360));
        sh.safeGetUniform("Sweep").set((float) Math.toRadians(Math.min(360, sweepDeg)));
        color(sh, "Color0", color);
        float e = radius + thickness / 2 + pad();
        rectQuad(cx - e, cy - e, cx + e, cy + e, cx, cy);
        end();
    }

    // ================================================================== text

    @Override public TextMeasurer text() { return McFonts.INSTANCE; }

    @Override
    public void drawText(String text, float x, float y, FontSpec spec, Color color, boolean shadow, TextDecoration deco) {
        if (text.isEmpty()) return;
        float alpha = color.a() * opacity;
        if (alpha < 0.02f) return;
        int argb = (Math.round(alpha * 255) << 24) | (color.argb() & 0xFFFFFF);
        McFonts.Family fam = McFonts.INSTANCE.family(spec);
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        g.flush();
        if (fam.isTtf()) {
            TtfText.Extent ext = TtfText.INSTANCE.draw(McFonts.INSTANCE.ttfRequest(fam, spec), text, x, y, dm(), guiScale,
                    g.pose().last().pose(), argb, shadow, fam.synthBold(spec.weight()), spec.italic());
            if (deco.underline() || deco.strikethrough()) {
                float thick = Math.max(1f / ext.devPx(), spec.size() * 0.07f);
                Color c = color.multiplyAlpha(1);
                if (deco.underline()) fillRect(new Rect(x, ext.baseline() + spec.size() * 0.12f, ext.width(), thick), c);
                if (deco.strikethrough()) fillRect(new Rect(x, ext.baseline() - spec.size() * 0.3f, ext.width(), thick), c);
            }
            return;
        }
        drawVanilla(Component.literal(text).withStyle(McFonts.INSTANCE.vanillaStyle(fam, spec)
                .withUnderlined(deco.underline()).withStrikethrough(deco.strikethrough())).getVisualOrderText(),
                x, y, spec.size(), spec.letterSpacing(), argb, shadow);
    }

    /**
     * Vanilla text (any Component) with its line box top-left at (x, y), scaled from font size and
     * snapped so bitmap-font texels land on whole physical pixels.
     */
    public void drawVanilla(FormattedCharSequence seq, float x, float y, float fontSize, float letterSpacing, int argb, boolean shadow) {
        if ((argb >>> 24) < 5) return;
        Font font = Minecraft.getInstance().font;
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        Affine d = dm();
        float devPx = d.approxScale() * guiScale;
        float scale = McFonts.vanillaScale(fontSize, devPx);
        PoseStack pose = g.pose();
        pose.pushPose();
        boolean axis = Math.abs(d.b()) < 1e-5f && Math.abs(d.c()) < 1e-5f;
        // Center the (possibly snapped-larger) 9-unit line in the fontSize box. Minecraft glyphs put capitals in
        // rows 0-6 of the 9 and descenders below, so one font pixel lower centers the capitals in the box.
        float yo = (fontSize - 9 * scale) / 2 + scale;
        if (axis) {
            float px = Math.round(d.mapX(x, y + yo) * guiScale) / guiScale;
            float py = Math.round(d.mapY(x, y + yo) * guiScale) / guiScale;
            pose.translate(px, py, 0);
            pose.scale(d.a() * scale, d.d() * scale, 1);
        } else {
            pose.mulPose(toMatrix(d));
            pose.translate(x, y + yo, 0);
            pose.scale(scale, scale, 1);
        }
        if (letterSpacing == 0) {
            g.drawString(font, seq, 0, 0, argb, shadow);
        } else {
            float[] cx = {0};
            float extra = letterSpacing / scale;
            seq.accept((idx, style, cp) -> {
                FormattedCharSequence one = FormattedCharSequence.codepoint(cp, style);
                g.drawString(font, one, cx[0], 0, argb, shadow);
                cx[0] += font.getSplitter().stringWidth(one) + extra;
                return true;
            });
        }
        pose.popPose();
    }

    // ================================================================== images

    @Override
    public void drawImage(ImageRef image, Rect dst, float u0, float v0, float u1, float v1, Color tint) {
        if (dst.isEmpty() || opacity <= 0.001f) return;
        ResourceLocation rl = ResourceLocation.parse(image.location());
        float a = tint.a() * opacity;
        if (image.kind() == ImageRef.Kind.SPRITE) {
            g.flush();
            PoseStack pose = g.pose();
            pose.pushPose();
            pose.mulPose(toMatrix(dm()));
            pose.translate(dst.x(), dst.y(), 0);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(tint.r(), tint.g(), tint.b(), a);
            g.blitSprite(rl, 0, 0, Math.max(1, Math.round(dst.width())), Math.max(1, Math.round(dst.height())));
            RenderSystem.setShaderColor(1, 1, 1, 1);
            pose.popPose();
            return;
        }
        g.flush();
        RenderSystem.setShaderTexture(0, rl);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f pose = g.pose().last().pose();
        BufferBuilder b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float[] xs = {dst.x(), dst.x(), dst.right(), dst.right()}, ys = {dst.y(), dst.bottom(), dst.bottom(), dst.y()};
        float[] us = {u0, u0, u1, u1}, vs = {v0, v1, v1, v0};
        for (int i = 0; i < 4; i++) {
            b.addVertex(pose, dm().mapX(xs[i], ys[i]), dm().mapY(xs[i], ys[i]), 0).setUv(us[i], vs[i]).setColor(tint.r(), tint.g(), tint.b(), a);
        }
        BufferUploader.drawWithShader(b.buildOrThrow());
    }

    /** Draws an item stack (with count/durability decorations) fitted into {@code size} px. */
    public void drawItem(ItemStack stack, float x, float y, float size, boolean decorations) {
        if (stack.isEmpty() || opacity <= 0.05f) return;
        g.flush();
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.mulPose(toMatrix(dm()));
        pose.translate(x, y, 0);
        // Scale depth by the same amount as width and height (the device matrix scales only x and y): block models are
        // lit from their normals, and a non-uniform scale skews them, which drew blocks with dark sides and a
        // blown-out top whenever an item was drawn larger than 16 units.
        float k = size / 16f;
        pose.scale(k, k, k * dm().approxScale());
        g.renderItem(stack, 0, 0);
        if (decorations) g.renderItemDecorations(Minecraft.getInstance().font, stack, 0, 0);
        pose.popPose();
        afterVanilla3D();
    }

    /**
     * Restores 2D state after vanilla code drew with depth (items, entities): clears depth so later UI
     * draws over it, and re-applies the scissor in case vanilla changed it.
     */
    public void afterVanilla3D() {
        g.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        applyScissor();
    }

    /** Runs vanilla drawing code with the current transform applied to the pose stack. */
    public void withPose(Runnable r) {
        g.flush();
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.mulPose(toMatrix(dm()));
        r.run();
        pose.popPose();
        afterVanilla3D();
    }

    // ================================================================== effects

    @Override
    public void drawShader(String id, Rect r, ShaderParams p) {
        if (r.isEmpty() || opacity <= 0.001f) return;
        ShaderInstance sh = ApothecaryShaders.effect(id);
        if (!begin(sh)) return;
        sh.safeGetUniform("Size").set(r.width(), r.height());
        sh.safeGetUniform("Time").set(p.time);
        sh.safeGetUniform("Mouse").set(p.mouseX, p.mouseY);
        sh.safeGetUniform("Params0").set(p.params[0], p.params[1], p.params[2], p.params[3]);
        sh.safeGetUniform("Params1").set(p.params[4], p.params[5], p.params[6], p.params[7]);
        for (int i = 0; i < 4; i++) color(sh, "Color" + i, p.colors[i]);
        sh.safeGetUniform("Radius").set(p.radius);
        rectQuad(r.x(), r.y(), r.right(), r.bottom(), r.x(), r.y());
        end();
    }

    @Override public boolean hasShader(String id) { return ApothecaryShaders.effect(id) != null; }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T backend(Class<T> type) {
        if (type.isInstance(this)) return (T) this;
        if (type.isInstance(g)) return (T) g;
        return null;
    }

    @Override public void flush() { g.flush(); }
}
