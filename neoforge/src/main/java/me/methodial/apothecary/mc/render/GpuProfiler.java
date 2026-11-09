package me.methodial.apothecary.mc.render;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Where the frame's time goes, on the GPU and the CPU, for whoever is drawing something heavy. Code marks the end of
 * each step with {@link #mark}; the time between two marks of the same scope is that step's. GPU times come from
 * timestamp queries read back a few frames later (so nothing waits on the GPU), averaged until {@link #reset}.
 * Off unless {@link #enable}d (or {@code -Dapothecary.profile=true}): then a mark costs nothing.
 */
public final class GpuProfiler {
    private static boolean enabled = Boolean.getBoolean("apothecary.profile");

    private record Mark(String scope, String step, int query, long cpu) {}

    /** One step's totals: GPU and CPU nanoseconds over {@code count} times. */
    public static final class Step {
        public long gpu, cpu;
        public int count;

        public double gpuMs() { return count == 0 ? 0 : gpu / 1e6 / count; }

        public double cpuMs() { return count == 0 ? 0 : cpu / 1e6 / count; }
    }

    private static final Deque<List<Mark>> PENDING = new ArrayDeque<>();
    private static final Deque<Integer> POOL = new ArrayDeque<>();
    private static final Map<String, Map<String, Step>> STATS = new LinkedHashMap<>();
    private static List<Mark> current = new ArrayList<>();

    private GpuProfiler() {}

    public static void enable(boolean on) {
        enabled = on;
        if (on) return;
        // Marks still waiting on the GPU would be read into the next session's averages; drop them, and keep their
        // queries for reuse (a query is issued afresh each time it is marked).
        for (List<Mark> frame : PENDING) for (Mark m : frame) POOL.push(m.query());
        for (Mark m : current) POOL.push(m.query());
        PENDING.clear();
        current = new ArrayList<>();
        reset();
    }

    public static boolean enabled() { return enabled; }

    /** The end of {@code step} in {@code scope} (its start: the scope's mark before it, or a "start" mark). */
    public static void mark(String scope, String step) {
        if (!enabled) return;
        RenderSystem.assertOnRenderThread();
        int query = POOL.isEmpty() ? GL15.glGenQueries() : POOL.pop();
        GL33.glQueryCounter(query, GL33.GL_TIMESTAMP);
        current.add(new Mark(scope, step, query, System.nanoTime()));
    }

    /** Closes the frame's marks and folds in every earlier frame whose GPU times have come back. */
    public static void endFrame() {
        if (!enabled) return;
        if (!current.isEmpty()) PENDING.add(current);
        current = new ArrayList<>();
        while (!PENDING.isEmpty()) {
            List<Mark> frame = PENDING.peek();
            int last = frame.get(frame.size() - 1).query();
            if (GL15.glGetQueryObjecti(last, GL15.GL_QUERY_RESULT_AVAILABLE) == 0 && PENDING.size() < 8) break;
            PENDING.poll();
            // per scope, the GPU and CPU time of its mark before
            Map<String, long[]> previous = new LinkedHashMap<>();
            for (Mark m : frame) {
                long gpu = GL33.glGetQueryObjecti64(m.query(), GL15.GL_QUERY_RESULT);
                long[] before = previous.put(m.scope(), new long[]{gpu, m.cpu()});
                if (before != null) {
                    Step step = STATS.computeIfAbsent(m.scope(), s -> new LinkedHashMap<>()).computeIfAbsent(m.step(), s -> new Step());
                    step.gpu += gpu - before[0];
                    step.cpu += m.cpu() - before[1];
                    step.count++;
                }
                POOL.push(m.query());
            }
        }
    }

    /** Every scope's steps, in the order first seen, averaged so far. */
    public static Map<String, Map<String, Step>> stats() { return STATS; }

    public static void reset() {
        STATS.clear();
    }
}
