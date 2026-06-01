package app.serenada.core.call

import java.util.PriorityQueue
import kotlin.math.roundToInt

/**
 * Streaming median over a numeric sample stream (telemetry §5.2) — a direct
 * port of the cross-platform **reference** implementation
 * (`client/packages/core/src/media/streamingMedian.ts`).
 *
 * Two-heap design: a max-heap holds the lower half, a min-heap the upper
 * half. `add` is O(log n); `median` is O(1). This avoids re-sorting the full
 * (unbounded, multi-hour) sample history on every recompute.
 *
 * Median definition (exact, all cores): odd count -> middle element; even
 * count -> mean of the two middle elements, rounded to nearest int; zero
 * samples -> null. Identical results to a sort-based median.
 */
internal class StreamingMedian {
    // Lower half (max at root), upper half (min at root). Invariant:
    // |lower| - |upper| in {0, 1} and every lower element <= every upper element.
    private val lower = PriorityQueue<Double>(compareByDescending { it }) // max-heap
    private val upper = PriorityQueue<Double>() // min-heap (natural order)

    fun add(value: Double) {
        if (lower.isEmpty() || value <= lower.peek()!!) {
            lower.add(value)
        } else {
            upper.add(value)
        }
        // Rebalance so lower holds the extra element on an odd total.
        if (lower.size > upper.size + 1) {
            upper.add(lower.poll()!!)
        } else if (upper.size > lower.size) {
            lower.add(upper.poll()!!)
        }
    }

    /** Median rounded to nearest int, or null when no samples were added. */
    fun median(): Int? {
        val total = lower.size + upper.size
        if (total == 0) return null
        return if (lower.size > upper.size) {
            lower.peek()!!.roundToInt()
        } else {
            ((lower.peek()!! + upper.peek()!!) / 2.0).roundToInt()
        }
    }
}
