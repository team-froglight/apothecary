package me.methodial.apothecary;

import me.methodial.apothecary.anim.Animator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimatorTest {
    @Test
    void everyRunsOncePerPeriodAndRefusesNoPeriod() {
        Animator a = new Animator();
        int[] runs = {0};
        a.every(0.1f, () -> runs[0]++);
        a.tick(0.25f);
        assertEquals(2, runs[0]);

        assertThrows(IllegalArgumentException.class, () -> a.every(0, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> a.every(-1, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> a.every(Float.NaN, () -> { }));
    }
}
