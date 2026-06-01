import Foundation

/// MOS (Mean Opinion Score) heuristic — a direct port of the cross-platform
/// **reference** implementation (`client/packages/core/src/media/mos.ts`).
/// Parity is locked by a checked-in golden test vector asserted identically
/// in all three core test suites (see `docs/telemetry/00-overview.md` §5.4).
///
/// Simplified ITU-T G.107 E-model -> MOS mapping, computed at call end from
/// the call-level medians. Do not change a coefficient without regenerating
/// the golden vector from the reference TS impl and updating all three suites
/// in lockstep.
enum Mos {
    /// Compute a MOS estimate from call-level quality medians.
    ///
    /// - Parameters:
    ///   - rttMs: median round-trip time in ms (0 if unknown)
    ///   - jitterMs: median jitter in ms (0 if unknown)
    ///   - lossPct: audio packet-loss percentage (0 if unknown)
    /// - Returns: MOS clamped to `[1.0, 4.5]`, rounded to 2 decimals.
    static func compute(rttMs: Double, jitterMs: Double, lossPct: Double) -> Double {
        let effLatency = rttMs / 2.0 + 2.0 * jitterMs + 10.0
        var r = effLatency < 160.0
            ? 93.2 - effLatency / 40.0
            : 93.2 - (effLatency - 120.0) / 10.0
        r -= 2.5 * lossPct
        r = min(100.0, max(0.0, r))
        var mos = 1.0 + 0.035 * r + r * (r - 60.0) * (100.0 - r) * 0.000007
        mos = min(4.5, max(1.0, mos))
        return (mos * 100.0).rounded() / 100.0
    }
}
