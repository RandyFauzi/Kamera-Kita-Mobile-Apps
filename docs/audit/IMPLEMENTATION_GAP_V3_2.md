# V3.2 IMPLEMENTATION GAP AUDIT

## Phase 1 & 2: Camera Pipeline & Lifecycle
*   **File:** `lib/main.dart`, `camera/CameraManager.kt`
*   **Current Behavior:** Flutter UI locked to portrait, preview uses `AspectRatio(9/16)` + `RotatedBox(quarterTurns: 1)`. `CameraManager.kt` uses Camera2Interop to lock 30fps and read dynamic metadata. Surface leak fixed via `surface.release()`.
*   **V3.2 Requirement:** Native CameraX rotation, proper lifecycle, dynamic metadata, no arbitrary stretching.
*   **Status:** `IMPLEMENTED` (Addressed in previous commits `02e5d48` & `f211782`).
*   **Proposed Change:** Monitor if further native `Preview.Builder.setTargetRotation` is strictly required over Flutter `RotatedBox`, but current geometry is mathematically correct.

## Phase 3 & 4 & 6: Real Capture Session Timing & Sync
*   **File:** `MainActivity.kt`, `sensors/SensorRecorder.kt`
*   **Current Behavior:** Uses `System.nanoTime()` randomly. Video and IMU start are sequentially fired with no monotonic offset tracking.
*   **V3.2 Requirement:** Monotonic timestamps (`SystemClock.elapsedRealtimeNanos()`). Exact `video_started_at_ns`, `imu_started_at_ns`.
*   **Status:** `MISSING`
*   **Proposed Change:** Introduce a `CaptureTimingManager` or standard timing model in `MainActivity` using `elapsedRealtimeNanos`.

## Phase 5: IMU Quality Metrics
*   **File:** `sensors/SensorRecorder.kt`
*   **Current Behavior:** Records raw CSV. `stopRecording()` returns empty map or basic count.
*   **V3.2 Requirement:** Calculate `actual_mean_rate_hz`, `median_interval_ns`, `jitter_stddev_ns`.
*   **Status:** `MISSING`
*   **Proposed Change:** Inject a `SensorMetricsCalculator` inside `SensorRecorder.kt` to process timestamps on `stopRecording()`.

## Phase 7 & 8: MediaPipe Hand Analysis & QC
*   **File:** `HandAnalyzer.kt`, `MainActivity.kt`
*   **Current Behavior:** Connected to `ImageAnalysis`. Returns `hand_presence_percentage`.
*   **V3.2 Requirement:** `frames_analyzed`, `frames_with_hands`, actual metrics injected.
*   **Status:** `PARTIAL` (Percentage is injected, but full QC stats missing).
*   **Proposed Change:** Expand `HandAnalyzer.stopRecordingStats()` to return a Map of detailed stats instead of just an Int percentage.

## Phase 10 & 11: Canonical Manifest
*   **File:** `episode/EpisodeManager.kt`
*   **Current Behavior:** Uses `JSONObject(manifest).toString()`.
*   **V3.2 Requirement:** RFC 8785 style deterministic Canonical JSON.
*   **Status:** `MISSING`
*   **Proposed Change:** Create `episode/CanonicalManifestSerializer.kt` to sort keys recursively and format primitives safely.

## Phase 12: Signing Correctness
*   **File:** `episode/EpisodeManager.kt`, `security/KeystoreManager.kt`
*   **Current Behavior:** Signs the manifest, then appends the signature to it. Circular reference.
*   **V3.2 Requirement:** Deterministic payload construction -> Hash -> Sign -> Append.
*   **Status:** `BROKEN` (Circular signing violates strict verification).
*   **Proposed Change:** Isolate signing payload. Generate `manifest_sha256` first, then sign.

## Phase 13: Keystore Hardening
*   **File:** `security/KeystoreManager.kt`
*   **Current Behavior:** `generateKey()` creates a new key every time. `hardware_backed` assumed.
*   **V3.2 Requirement:** `getOrCreateSigningKey(alias)`, verify `KeyInfo.isInsideSecureHardware()`, classify `STRONGBOX/TEE/SOFTWARE`.
*   **Status:** `PARTIAL`
*   **Proposed Change:** Refactor `KeystoreManager` to inspect `KeyInfo`.

## Phase 15: Background Processing
*   **File:** `MainActivity.kt`
*   **Current Behavior:** `episodeManager.finalizeEpisode` (Hashing, Encryption) runs on `getMainExecutor`.
*   **V3.2 Requirement:** Run heavy IO/Crypto off the UI thread.
*   **Status:** `BROKEN` (Causes UI freeze on finalization).
*   **Proposed Change:** Wrap finalization in Kotlin Coroutines (`Dispatchers.IO`).

## Phase 16: Thermal Guard
*   **File:** `ThermalGuard.kt` (New)
*   **Current Behavior:** Thermal state ignored.
*   **V3.2 Requirement:** Monitor `PowerManager.getCurrentThermalStatus()`.
*   **Status:** `MISSING`
*   **Proposed Change:** Implement `ThermalGuard` and record events.
