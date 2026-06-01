package app.serenada.core.call

import app.serenada.core.ConnectionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Reconnect-reason table. Identical mapping to the web
 * (`reconnectReason.test.ts`) and iOS (`ReconnectReasonTests`) suites; parity
 * across the three tables is also guarded by `check-telemetry-parity.mjs`.
 */
class ReconnectReasonTest {
    @Test
    fun `maps only concrete recovery-abandonment codes`() {
        assertEquals(ConnectionEvent.ReconnectFailedReason.TIMEOUT, ReconnectReason.reasonForCode("JOIN_TIMEOUT"))
        assertEquals(ConnectionEvent.ReconnectFailedReason.NETWORK_CONNECTIVITY, ReconnectReason.reasonForCode("INVALID_RECONNECT_TOKEN"))
        assertEquals(ConnectionEvent.ReconnectFailedReason.NETWORK_CONNECTIVITY, ReconnectReason.reasonForCode("CONNECTION_FAILED"))
        assertEquals(ConnectionEvent.ReconnectFailedReason.NETWORK_CONNECTIVITY, ReconnectReason.reasonForCode("ICE_SERVER_FETCH_FAILED"))
    }

    @Test
    fun `returns null for arbitrary or unknown server errors`() {
        for (code in listOf("BAD_REQUEST", "UNSUPPORTED_VERSION", "ROOM_FULL", "ROOM_ENDED", "NOT_IN_ROOM", "NOT_HOST", "SERVER_NOT_CONFIGURED", "TURN_REFRESH_FAILED", "UNKNOWN", "", null)) {
            assertNull(ReconnectReason.reasonForCode(code))
        }
    }
}
