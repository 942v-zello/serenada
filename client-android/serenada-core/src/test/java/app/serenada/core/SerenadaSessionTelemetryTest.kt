package app.serenada.core

import android.os.Looper
import app.serenada.core.call.CallPhase
import app.serenada.core.call.ConnectionStatus
import app.serenada.core.fakes.TestSessionFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

/**
 * Session-level telemetry integration test — exercises the
 * hand-ported wiring (CallQualityTracker feed from phase + connection-status
 * transitions, ConnectionEvent dispatch, finalize-before-teardown ordering)
 * end-to-end through the real `SerenadaSession`, not the tracker in isolation.
 * This is where the #1 phantom-reconnect and #5 callback-ordering risks live.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SerenadaSessionTelemetryTest {

    /** Recording delegate that captures connection events + terminal callbacks. */
    private class RecordingDelegate : SerenadaCoreDelegate {
        val connectionEvents = mutableListOf<ConnectionEvent>()
        val endReasons = mutableListOf<EndReason>()
        var stateChanges = 0
        override fun onConnectionEvent(session: SerenadaSession, event: ConnectionEvent) {
            connectionEvents.add(event)
        }
        override fun onSessionEnded(session: SerenadaSession, reason: EndReason) {
            endReasons.add(reason)
        }
        override fun onSessionStateChanged(session: SerenadaSession, state: CallState) {
            stateChanges += 1
        }
    }

    private lateinit var delegate: RecordingDelegate
    private lateinit var factory: TestSessionFactory

    private fun start(delegateOverride: SerenadaCoreDelegate? = null) {
        delegate = RecordingDelegate()
        factory = TestSessionFactory(delegate = delegateOverride ?: delegate)
    }

    @After
    fun tearDown() {
        if (::factory.isInitialized) factory.tearDown()
    }

    private fun idle() = ShadowLooper.idleMainLooper()

    @Test
    fun `dropout then recovery emits reconnected and counts via the summary`() {
        start()
        factory.advanceToInCallWithTurn()
        assertEquals(CallPhase.InCall, factory.session.state.value.phase)

        // Signaling drops while in-call -> dropout opens.
        factory.fakeProvider.simulateDisconnected(reason = "test")
        idle()
        assertEquals(ConnectionStatus.Recovering, factory.session.state.value.connectionStatus)

        // Reconnect -> recovery closes the dropout, emits reconnected.
        factory.fakeProvider.simulateConnected()
        idle()

        val reconnects = delegate.connectionEvents.filterIsInstance<ConnectionEvent.Reconnected>()
        assertEquals(1, reconnects.size)
        val summary = factory.session.qualitySummary
        assertNotNull(summary)
        assertEquals(1, summary!!.countDisconnects)
        assertEquals(1, summary.countReconnects)
    }

    // #1 — phantom reconnect on remote-leave.
    @Test
    fun `peer leaving mid-dropout does not emit a phantom reconnected`() {
        start()
        factory.advanceToInCallWithTurn(localCid = "local", remoteCid = "remote")
        assertEquals(CallPhase.InCall, factory.session.state.value.phase)

        // Dropout opens.
        factory.fakeProvider.simulateDisconnected(reason = "test")
        idle()
        assertEquals(ConnectionStatus.Recovering, factory.session.state.value.connectionStatus)

        // Re-establish signaling (still degraded media), then the peer leaves:
        // phase -> Waiting forces the status machine back to Connected.
        factory.fakeProvider.simulateConnected()
        idle()
        // The reconnect above closed the first dropout; clear and re-open to
        // model a dropout still open when the peer departs.
        factory.fakeProvider.simulateDisconnected(reason = "test")
        idle()
        factory.fakeProvider.simulatePeerLeft("remote")
        idle()

        // Phase left InCall -> the forced status reset is a peer-departure, not
        // a recovery. No phantom reconnected beyond the single real one.
        val reconnects = delegate.connectionEvents.filterIsInstance<ConnectionEvent.Reconnected>()
        assertEquals(1, reconnects.size)
        assertEquals(CallPhase.Waiting, factory.session.state.value.phase)
    }

    // #5 — a throwing onConnectionEvent handler must not skip terminal callbacks.
    @Test
    fun `throwing onConnectionEvent handler does not abort terminal callbacks`() {
        val throwingDelegate = object : SerenadaCoreDelegate {
            var ended = false
            override fun onConnectionEvent(session: SerenadaSession, event: ConnectionEvent) {
                throw RuntimeException("host handler boom")
            }
            override fun onSessionEnded(session: SerenadaSession, reason: EndReason) {
                ended = true
            }
        }
        start(delegateOverride = throwingDelegate)
        factory.advanceToInCallWithTurn()

        // A server error that is a recovery-abandonment (transport exhaustion)
        // emits reconnectFailed (the throwing handler runs) THEN onSessionEnded.
        factory.fakeProvider.simulateError(code = "CONNECTION_FAILED", message = "boom")
        idle()

        assertTrue("onSessionEnded must still run after a throwing handler", throwingDelegate.ended)
    }

    @Test
    fun `summary is finalized and readable after close (teardown)`() {
        start()
        factory.advanceToInCallWithTurn()
        factory.fakeClock.advance(1000)
        factory.session.close()
        idle()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(1, TimeUnit.SECONDS)
        // Summary survives teardown (finalized snapshot).
        assertNotNull(factory.session.qualitySummary)
    }
}
