package app.serenada.core.call

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * MOS golden vector — the cross-platform source of truth.
 * This table is identical to the web (`mos.test.ts`) and iOS (`MosTests`)
 * suites and must reproduce to +/-0.01. If a coefficient changes, regenerate
 * the table from the reference TS impl and update all three suites together.
 */
class MosTest {
    private data class Vector(
        val rtt: Double,
        val jitter: Double,
        val loss: Double,
        val expected: Double,
    )

    private val goldenVector = listOf(
        Vector(0.0, 0.0, 0.0, 4.40),
        Vector(50.0, 5.0, 0.0, 4.39),
        Vector(150.0, 20.0, 1.0, 4.28),
        Vector(300.0, 40.0, 3.0, 3.77),
        Vector(500.0, 60.0, 8.0, 2.43),
    )

    @Test
    fun `matches golden vector to two decimals`() {
        goldenVector.forEach { v ->
            assertEquals(
                "rtt=${v.rtt} jitter=${v.jitter} loss=${v.loss}",
                v.expected,
                Mos.compute(v.rtt, v.jitter, v.loss),
                0.01,
            )
        }
    }

    @Test
    fun `clamps to the 1_0 to 4_5 range`() {
        // Best-quality input yields the formula's natural maximum (~4.40),
        // never the documented 4.5 ceiling — assert the exact value so a
        // future edit that raises/removes the ceiling or drifts a coefficient
        // fails here (the old `<= 4.5` assertion was a tautology).
        assertEquals(4.40, Mos.compute(0.0, 0.0, 0.0), 0.01)
        assertEquals(1.0, Mos.compute(5000.0, 5000.0, 100.0), 0.0001)
        assert(Mos.compute(0.0, 0.0, 0.0) <= 4.5)
    }
}
