import Foundation

/// Reconnect-reason mapping — a direct port of the cross-platform **reference**
/// table (`client/packages/core/src/media/reconnectReason.ts`, telemetry §5.1).
/// Keyed on the **server signaling error code** so the single shared
/// `reconnectFailed.reason` analytics column is classified identically on every
/// platform. A code not in the table is *not* a recovery-abandonment path and
/// produces no reliability event (nil).
///
/// Parity across the three reason tables is guarded by
/// `scripts/check-telemetry-parity.mjs`.
///
/// Only these four codes are recovery-abandonment paths; every other server
/// error (BAD_REQUEST, UNSUPPORTED_VERSION, ROOM_FULL, NOT_IN_ROOM, ...) is an
/// arbitrary/unknown signaling error and must NOT report a failed reconnect.
enum ReconnectReason {
    static func reasonForCode(_ serverCode: String?) -> ConnectionEvent.ReconnectFailedReason? {
        switch serverCode {
        case "JOIN_TIMEOUT":
            return .timeout
        case "INVALID_RECONNECT_TOKEN", "CONNECTION_FAILED", "ICE_SERVER_FETCH_FAILED":
            return .networkConnectivity
        default:
            return nil
        }
    }
}
