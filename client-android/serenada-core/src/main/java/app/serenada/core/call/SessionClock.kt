package app.serenada.core.call

internal interface SessionClock {
    /** Wall-clock milliseconds since epoch. */
    fun nowMs(): Long

    /**
     * Monotonic milliseconds for interval math: unaffected by
     * a wall-clock / NTP step, so a backward correction during an open dropout
     * can't record a real outage as 0ms. Production uses
     * `SystemClock.elapsedRealtime()`.
     */
    fun monotonicMs(): Long
}
