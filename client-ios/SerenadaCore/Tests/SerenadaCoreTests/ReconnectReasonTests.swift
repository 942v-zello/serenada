@testable import SerenadaCore
import XCTest

/// Reconnect-reason table. Identical mapping to the web
/// (`reconnectReason.test.ts`) and Android (`ReconnectReasonTest.kt`) suites;
/// parity across the three tables is also guarded by
/// `check-telemetry-parity.mjs`.
final class ReconnectReasonTests: XCTestCase {
    func testMapsOnlyConcreteRecoveryAbandonmentCodes() {
        XCTAssertEqual(ReconnectReason.reasonForCode("JOIN_TIMEOUT"), .timeout)
        XCTAssertEqual(ReconnectReason.reasonForCode("INVALID_RECONNECT_TOKEN"), .networkConnectivity)
        XCTAssertEqual(ReconnectReason.reasonForCode("CONNECTION_FAILED"), .networkConnectivity)
        XCTAssertEqual(ReconnectReason.reasonForCode("ICE_SERVER_FETCH_FAILED"), .networkConnectivity)
    }

    func testReturnsNilForArbitraryOrUnknownServerErrors() {
        for code in ["BAD_REQUEST", "UNSUPPORTED_VERSION", "ROOM_FULL", "ROOM_ENDED", "NOT_IN_ROOM", "NOT_HOST", "SERVER_NOT_CONFIGURED", "TURN_REFRESH_FAILED", "UNKNOWN", ""] {
            XCTAssertNil(ReconnectReason.reasonForCode(code))
        }
        XCTAssertNil(ReconnectReason.reasonForCode(nil))
    }
}
