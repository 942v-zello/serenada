@testable import SerenadaCore
import XCTest

/// MOS golden vector — the cross-platform source of truth.
/// This table is identical to the web (`mos.test.ts`) and Android
/// (`MosTest.kt`) suites and must reproduce to +/-0.01. If a coefficient
/// changes, regenerate the table from the reference TS impl and update all
/// three suites together.
final class MosTests: XCTestCase {
    private struct Vector {
        let rtt: Double
        let jitter: Double
        let loss: Double
        let expected: Double
    }

    private let goldenVector: [Vector] = [
        Vector(rtt: 0, jitter: 0, loss: 0, expected: 4.40),
        Vector(rtt: 50, jitter: 5, loss: 0, expected: 4.39),
        Vector(rtt: 150, jitter: 20, loss: 1, expected: 4.28),
        Vector(rtt: 300, jitter: 40, loss: 3, expected: 3.77),
        Vector(rtt: 500, jitter: 60, loss: 8, expected: 2.43),
    ]

    func testGoldenVector() {
        for v in goldenVector {
            XCTAssertEqual(
                Mos.compute(rttMs: v.rtt, jitterMs: v.jitter, lossPct: v.loss),
                v.expected,
                accuracy: 0.01,
                "rtt=\(v.rtt) jitter=\(v.jitter) loss=\(v.loss)"
            )
        }
    }

    func testClampsToRange() {
        // Best-quality input yields the formula's natural maximum (~4.40),
        // never the documented 4.5 ceiling — assert the exact value so a
        // future edit that raises/removes the ceiling or drifts a coefficient
        // fails here (the old `<= 4.5` assertion was a tautology).
        XCTAssertEqual(Mos.compute(rttMs: 0, jitterMs: 0, lossPct: 0), 4.40, accuracy: 0.01)
        XCTAssertEqual(Mos.compute(rttMs: 5000, jitterMs: 5000, lossPct: 100), 1.0, accuracy: 0.0001)
        XCTAssertLessThanOrEqual(Mos.compute(rttMs: 0, jitterMs: 0, lossPct: 0), 4.5)
    }
}
