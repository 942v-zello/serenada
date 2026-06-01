package app.serenada.core.call

import android.os.SystemClock

internal class LiveSessionClock : SessionClock {
    override fun nowMs(): Long = System.currentTimeMillis()
    override fun monotonicMs(): Long = SystemClock.elapsedRealtime()
}
