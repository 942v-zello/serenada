import Foundation

/// Immutable snapshot of aggregate call quality, computed by the SDK and
/// consumed by hosts to populate `redacted-analytics-event` analytics (telemetry §5).
/// Updated live during the call and finalized at call end; remains readable
/// after the session stops.
///
/// `mosScore`/`packetLossPct`/`medianLatencyMs`/`medianJitterMs` follow the
/// exact null policy + counter-delta loss rules in the cross-platform
/// contract (overview §5.2/§5.4): packet loss is computed from raw
/// `audioPacketsLost`/`audioPacketsReceived` deltas, never by medianing the
/// cumulative loss percentage.
public struct CallQualitySummary: Equatable, Sendable {
    /// MOS estimate (§5.4). Nil unless all three of latency/jitter/loss are defined.
    public var mosScore: Double?
    /// Call-level audio rx packet loss percentage, from counter deltas (§5.2).
    public var packetLossPct: Double?
    /// Median of sampled rttMs (§5.2 median definition), or nil.
    public var medianLatencyMs: Int?
    /// Median of sampled audioJitterMs, or nil.
    public var medianJitterMs: Int?
    /// Number of dropout starts while in-call.
    public var countDisconnects: Int
    /// Number of dropouts that recovered.
    public var countReconnects: Int
    /// Sum of dropout interval durations in ms.
    public var totalDropoutDurationMs: Int64
    /// Count of stats samples that contributed >=1 usable quality field (a
    /// latency/jitter gauge, or a loss interval that was actually accumulated).
    /// Diagnostic only — the MOS null policy gates on the three medians being
    /// non-nil, not on this count.
    public var qualitySampleCount: Int

    public init(
        mosScore: Double? = nil,
        packetLossPct: Double? = nil,
        medianLatencyMs: Int? = nil,
        medianJitterMs: Int? = nil,
        countDisconnects: Int = 0,
        countReconnects: Int = 0,
        totalDropoutDurationMs: Int64 = 0,
        qualitySampleCount: Int = 0
    ) {
        self.mosScore = mosScore
        self.packetLossPct = packetLossPct
        self.medianLatencyMs = medianLatencyMs
        self.medianJitterMs = medianJitterMs
        self.countDisconnects = countDisconnects
        self.countReconnects = countReconnects
        self.totalDropoutDurationMs = totalDropoutDurationMs
        self.qualitySampleCount = qualitySampleCount
    }
}
