package app.serenada.core.call

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * MOS (Mean Opinion Score) heuristic — a direct port of the cross-platform
 * **reference** implementation (`client/packages/core/src/media/mos.ts`).
 * Parity is locked by a checked-in golden test vector asserted identically
 * in all three core test suites (see `docs/telemetry/00-overview.md` §5.4).
 *
 * Simplified ITU-T G.107 E-model -> MOS mapping, computed at call end from
 * the call-level medians. Do not change a coefficient without regenerating
 * the golden vector from the reference TS impl and updating all three suites
 * in lockstep.
 */
internal object Mos {
    /**
     * Compute a MOS estimate from call-level quality medians.
     *
     * @param rttMs    median round-trip time in ms (0 if unknown)
     * @param jitterMs median jitter in ms (0 if unknown)
     * @param lossPct  audio packet-loss percentage (0 if unknown)
     * @return MOS clamped to `[1.0, 4.5]`, rounded to 2 decimals.
     */
    fun compute(rttMs: Double, jitterMs: Double, lossPct: Double): Double {
        val effLatency = rttMs / 2.0 + 2.0 * jitterMs + 10.0
        var r = if (effLatency < 160.0) {
            93.2 - effLatency / 40.0
        } else {
            93.2 - (effLatency - 120.0) / 10.0
        }
        r -= 2.5 * lossPct
        r = r.coerceIn(0.0, 100.0)
        var mos = 1.0 + 0.035 * r + r * (r - 60.0) * (100.0 - r) * 0.000007
        mos = min(4.5, max(1.0, mos))
        return (mos * 100.0).roundToInt() / 100.0
    }
}
