import Combine
import Foundation

@MainActor
protocol SessionClock: AnyObject {
    /// Wall-clock milliseconds since epoch.
    func nowMs() -> Int64
    /// Monotonic milliseconds for interval math: unaffected
    /// by a wall-clock / NTP step, so a backward correction during an open
    /// dropout can't record a real outage as 0ms. Production uses
    /// `DispatchTime` (CLOCK_UPTIME / mach absolute time).
    func monotonicMs() -> Int64
    func sleep(nanoseconds: UInt64) async throws
    func scheduleRepeating(intervalSeconds: TimeInterval, action: @escaping @MainActor () -> Void) -> AnyCancellable
}
