package app.serenada.core

/**
 * Immutable snapshot of aggregate call quality, computed by the SDK and
 * consumed by hosts to populate their call-ended analytics.
 * Updated live during the call and finalized at call end; remains readable
 * after the session stops.
 *
 * `mosScore`/`packetLossPct`/`medianLatencyMs`/`medianJitterMs` follow the
 * exact null policy + counter-delta loss rules in the cross-platform
 * contract: packet loss is computed from raw
 * `audioPacketsLost`/`audioPacketsReceived` deltas, never by medianing the
 * cumulative loss percentage.
 */
data class CallQualitySummary(
    /** MOS estimate. Null unless all three of latency/jitter/loss are defined. */
    val mosScore: Double? = null,
    /** Call-level audio rx packet loss percentage, from counter deltas. */
    val packetLossPct: Double? = null,
    /** Median of sampled rttMs, or null. */
    val medianLatencyMs: Int? = null,
    /** Median of sampled audioJitterMs, or null. */
    val medianJitterMs: Int? = null,
    /** Number of dropout starts while in-call. */
    val countDisconnects: Int = 0,
    /** Number of dropouts that recovered. */
    val countReconnects: Int = 0,
    /** Sum of dropout interval durations in ms. */
    val totalDropoutDurationMs: Long = 0L,
    /**
     * Count of stats samples that contributed >=1 usable quality field (a
     * latency/jitter gauge, or a loss interval that was actually accumulated).
     * Diagnostic only — the MOS null policy gates on the three medians being
     * non-null, not on this count.
     */
    val qualitySampleCount: Int = 0,
)
