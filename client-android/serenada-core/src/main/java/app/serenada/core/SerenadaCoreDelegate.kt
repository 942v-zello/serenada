package app.serenada.core

/**
 * Delegate interface for receiving SDK lifecycle callbacks.
 * All methods have default no-op implementations so only relevant callbacks need overriding.
 */
interface SerenadaCoreDelegate {
    /**
     * Called when a session requires permissions before joining.
     * The host app or call-ui should request permissions and then call session.resumeJoin().
     */
    fun onPermissionsRequired(session: SerenadaSession, permissions: List<MediaCapability>) {}

    /**
     * Called when the session state changes.
     */
    fun onSessionStateChanged(session: SerenadaSession, state: CallState) {}

    /**
     * Called when a session ends.
     */
    fun onSessionEnded(session: SerenadaSession, reason: EndReason) {}

    /**
     * Called when the SDK raises a connection-quality event (telemetry §5.1).
     * Hosts map these to the `redacted-analytics-event` /
     * `redacted-analytics-event` analytics events. Additive, default no-op
     * — read aggregate quality via [SerenadaSession.qualitySummary].
     */
    fun onConnectionEvent(session: SerenadaSession, event: ConnectionEvent) {}
}

sealed class EndReason {
    object LocalLeft : EndReason()
    object RemoteEnded : EndReason()
    data class Error(val error: CallError) : EndReason()
}

/** Reason a dropout began, carried so hosts can distinguish recovery causes (telemetry §5.1). */
enum class DropoutTrigger {
    /** Dropout began with signaling/network loss. */
    NETWORK_LOST,

    /** Dropout cause could not be attributed to network loss (e.g. ICE/peer-level). */
    UNKNOWN,
}

/**
 * Connection-quality event emitted by the SDK through
 * [SerenadaCoreDelegate.onConnectionEvent] (telemetry §5.1).
 */
sealed class ConnectionEvent {
    /** A dropout recovered. Maps to `redacted-analytics-event`. */
    data class Reconnected(
        /** Downtime of the recovered dropout, in ms. */
        val downtimeMs: Long,
        /** `NETWORK_LOST` if the dropout began with signaling/network loss, else `UNKNOWN`. */
        val reason: DropoutTrigger,
    ) : ConnectionEvent()

    /** Recovery was abandoned. Maps to `redacted-analytics-event`. */
    data class ReconnectFailed(
        val reason: ReconnectFailedReason,
    ) : ConnectionEvent()

    enum class ReconnectFailedReason {
        /** Recovery window elapsed. */
        TIMEOUT,

        /** No network / transport available. */
        NETWORK_CONNECTIVITY,
    }
}
