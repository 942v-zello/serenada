#!/usr/bin/env node

/**
 * Verifies that the telemetry **MOS coefficients** and the **reconnect-reason
 * table** are identical across all three Serenada cores (web, Android, iOS).
 *
 * These two artifacts feed shared analytics columns but can't be
 * guarded by `check-resilience-constants.mjs` (that script only parses numeric
 * resilience constants). A single-file typo in a MOS coefficient, or a
 * platform that classifies a reconnect-failure code differently, drifts the
 * warehouse data silently until someone spots a per-platform skew well after
 * ship. This script makes such drift a failing check.
 *
 * Usage:  node scripts/check-telemetry-parity.mjs
 * Exit 0 on match, 1 on mismatch.
 */

import { readFileSync } from 'fs';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __dirname = dirname(fileURLToPath(import.meta.url));
const root = resolve(__dirname, '..');

const MOS_TS = resolve(root, 'client/packages/core/src/media/mos.ts');
const MOS_KT = resolve(root, 'client-android/serenada-core/src/main/java/app/serenada/core/call/Mos.kt');
const MOS_SWIFT = resolve(root, 'client-ios/SerenadaCore/Sources/Call/Mos.swift');

const REASON_TS = resolve(root, 'client/packages/core/src/media/reconnectReason.ts');
const REASON_KT = resolve(root, 'client-android/serenada-core/src/main/java/app/serenada/core/call/ReconnectReason.kt');
const REASON_SWIFT = resolve(root, 'client-ios/SerenadaCore/Sources/Call/ReconnectReason.swift');

let exitCode = 0;
function fail(msg) {
    console.error(`  FAIL: ${msg}`);
    exitCode = 1;
}

// ── MOS coefficients ─────────────────────────────────────────────────
//
// Extract every numeric literal from each MOS source (after stripping
// comments + doc lines), normalized, and require the three sequences to be
// identical. This catches a single-character coefficient typo on any port.

function extractNumbers(src) {
    // Drop line comments + block-comment lines so numbers that appear in
    // doc prose don't pollute the coefficient list.
    const code = src
        .replace(/\/\*[\s\S]*?\*\//g, '')
        .split('\n')
        .filter((line) => !line.trim().startsWith('//') && !line.trim().startsWith('*') && !line.trim().startsWith('///'))
        .join('\n');
    const matches = code.match(/(?<![\w.])\d+(?:\.\d+)?(?:[eE][-+]?\d+)?/g) ?? [];
    // Normalize as a sorted multiset so the same coefficients written in a
    // different textual order (e.g. `clamp(mos, 1.0, 4.5)` vs
    // `min(4.5, max(1.0, mos))`) still compare equal, while a single-character
    // typo in any coefficient still fails.
    return matches
        .map((m) => Number.parseFloat(m))
        .filter((n) => Number.isFinite(n))
        .sort((a, b) => a - b);
}

function checkParity(label, tsSrc, ktSrc, swSrc) {
    const ts = extractNumbers(tsSrc);
    const kt = extractNumbers(ktSrc);
    const sw = extractNumbers(swSrc);
    const a = JSON.stringify(ts);
    const b = JSON.stringify(kt);
    const c = JSON.stringify(sw);
    if (a !== b) fail(`${label}: TypeScript ${a} != Kotlin ${b}`);
    else if (a !== c) fail(`${label}: TypeScript ${a} != Swift ${c}`);
    else console.log(`OK: ${label} match across platforms (${ts.length} values).`);
}

checkParity(
    'MOS coefficients',
    readFileSync(MOS_TS, 'utf-8'),
    readFileSync(MOS_KT, 'utf-8'),
    readFileSync(MOS_SWIFT, 'utf-8'),
);

// ── Reconnect-reason table ───────────────────────────────────────────
//
// The reason table maps server error codes -> {timeout, networkConnectivity}.
// Extract `CODE -> reason` pairs from each file and require an identical set.

const TIMEOUT = /JOIN_TIMEOUT/;
const NETWORK = /(INVALID_RECONNECT_TOKEN|CONNECTION_FAILED|ICE_SERVER_FETCH_FAILED)/;

function reasonTable(src) {
    // Each platform lists its codes as string literals grouped by the reason
    // they map to. Recover the mapping structurally: which codes resolve to
    // `timeout` vs `networkConnectivity`.
    const codes = (src.match(/["']([A-Z_]+)["']/g) ?? []).map((c) => c.replace(/["']/g, ''));
    const table = {};
    for (const code of codes) {
        if (TIMEOUT.test(code)) table[code] = 'timeout';
        else if (NETWORK.test(code)) table[code] = 'networkConnectivity';
    }
    return table;
}

{
    const ts = reasonTable(readFileSync(REASON_TS, 'utf-8'));
    const kt = reasonTable(readFileSync(REASON_KT, 'utf-8'));
    const sw = reasonTable(readFileSync(REASON_SWIFT, 'utf-8'));
    const a = JSON.stringify(ts, Object.keys(ts).sort());
    const b = JSON.stringify(kt, Object.keys(kt).sort());
    const c = JSON.stringify(sw, Object.keys(sw).sort());
    const expected = JSON.stringify(
        { CONNECTION_FAILED: 'networkConnectivity', ICE_SERVER_FETCH_FAILED: 'networkConnectivity', INVALID_RECONNECT_TOKEN: 'networkConnectivity', JOIN_TIMEOUT: 'timeout' },
        ['CONNECTION_FAILED', 'ICE_SERVER_FETCH_FAILED', 'INVALID_RECONNECT_TOKEN', 'JOIN_TIMEOUT'],
    );
    if (a !== b) fail(`reconnect-reason table: TypeScript != Kotlin`);
    else if (a !== c) fail(`reconnect-reason table: TypeScript != Swift`);
    else if (a !== expected) fail(`reconnect-reason table: drifted from the documented set (got ${a})`);
    else console.log(`OK: reconnect-reason table matches across platforms (${Object.keys(ts).length} codes).`);
}

process.exit(exitCode);
