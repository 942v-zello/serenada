import Foundation

/// Streaming median over a numeric sample stream (telemetry §5.2) — a direct
/// port of the cross-platform **reference** implementation
/// (`client/packages/core/src/media/streamingMedian.ts`).
///
/// Two-heap design: a max-heap holds the lower half, a min-heap the upper
/// half. `add` is O(log n); `median` is O(1). This avoids re-sorting the full
/// (unbounded, multi-hour) sample history on every recompute.
///
/// Median definition (exact, all cores): odd count -> middle element; even
/// count -> mean of the two middle elements, rounded to nearest int; zero
/// samples -> nil. Identical results to a sort-based median.
struct StreamingMedian {
    /// A binary heap of doubles, min or max ordered by the comparator.
    private struct NumberHeap {
        private var data: [Double] = []
        /// `compare(a, b) == true` means `a` has higher priority (sits at root).
        private let compare: (Double, Double) -> Bool

        init(compare: @escaping (Double, Double) -> Bool) {
            self.compare = compare
        }

        var count: Int { data.count }
        func peek() -> Double { data[0] }

        mutating func push(_ value: Double) {
            data.append(value)
            siftUp(data.count - 1)
        }

        mutating func pop() -> Double {
            let top = data[0]
            let last = data.removeLast()
            if !data.isEmpty {
                data[0] = last
                siftDown(0)
            }
            return top
        }

        private mutating func siftUp(_ index: Int) {
            var i = index
            while i > 0 {
                let parent = (i - 1) >> 1
                if !compare(data[i], data[parent]) { break }
                data.swapAt(i, parent)
                i = parent
            }
        }

        private mutating func siftDown(_ index: Int) {
            var i = index
            let n = data.count
            while true {
                let left = 2 * i + 1
                let right = 2 * i + 2
                var best = i
                if left < n && compare(data[left], data[best]) { best = left }
                if right < n && compare(data[right], data[best]) { best = right }
                if best == i { break }
                data.swapAt(i, best)
                i = best
            }
        }
    }

    // Lower half (max at root), upper half (min at root). Invariant:
    // |lower| - |upper| in {0, 1} and every lower element <= every upper element.
    private var lower = NumberHeap(compare: { $0 > $1 }) // max-heap
    private var upper = NumberHeap(compare: { $0 < $1 }) // min-heap

    mutating func add(_ value: Double) {
        if lower.count == 0 || value <= lower.peek() {
            lower.push(value)
        } else {
            upper.push(value)
        }
        // Rebalance so lower holds the extra element on an odd total.
        if lower.count > upper.count + 1 {
            upper.push(lower.pop())
        } else if upper.count > lower.count {
            lower.push(upper.pop())
        }
    }

    /// Median rounded to nearest int, or nil when no samples were added.
    func median() -> Int? {
        let total = lower.count + upper.count
        if total == 0 { return nil }
        if lower.count > upper.count {
            return Int(lower.peek().rounded())
        }
        return Int(((lower.peek() + upper.peek()) / 2.0).rounded())
    }
}
