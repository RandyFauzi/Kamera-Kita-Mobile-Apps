# POST-IMPLEMENTATION FORENSIC CODE REVIEW (V3.2)

## 1. Camera Preview & Rotation
*   **Requirement:** Camera preview & Camera rotation
*   **Status:** `PARTIAL`
*   **Evidence:** `main.dart` uses `AspectRatio(9.0 / 16.0)` and `RotatedBox(quarterTurns: 1)` on a `Texture` widget. While this mathematically prevents OpenGL jelly/shearing on portrait-locked Flutter UIs and displays a correct aspect ratio, it decouples the Flutter preview pipeline from CameraX's `targetRotation`.
*   **Risk:** Edge cases in OS-level screen rotation (e.g. forced orientation overrides) might cause the preview to display incorrectly, as it relies on a hardcoded 90-degree UI rotation rather than pulling matrix transformation directly from `SurfaceRequest.transformationInfo`.
*   **Required Action:** Upgrade Flutter `Texture` implementation to read and apply CameraX's native `transformationInfo` matrix, or use `PreviewView` natively via PlatformViews.

## 2. Camera2 Metadata
*   **Requirement:** Camera2 metadata (Intrinsics, FPS, Active Array)
*   **Status:** `PARTIAL`
*   **Evidence:** `CameraManager.kt` successfully extracts `cameraInfo.sensorRotationDegrees`, `request.resolution`, and `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` via `Camera2CameraInfo`. However, focal lengths, sensor physical size, active array size, and distortion coefficients are not extracted.
*   **Risk:** Physical-AI datasets (like Ego4D) require lens intrinsics for 3D reconstruction.
*   **Required Action:** Extract `LENS_INTRINSIC_CALIBRATION` and `SENSOR_INFO_ACTIVE_ARRAY_SIZE` from `CameraCharacteristics` and inject into `cameraMetadata`.

## 3. Actual FPS
*   **Requirement:** Actual FPS
*   **Status:** `MISSING`
*   **Evidence:** `MainActivity.kt` explicitly sets `"fps_observed" to cameraManager.actualFpsRequested.toDouble()` (hardcoded proxy).
*   **Risk:** Violates "DO NOT FAKE DATA" rule. If hardware drops frames, the dataset metadata lies.
*   **Required Action:** Read actual recorded frame count and duration from `VideoRecordEvent.Finalize.recordingStats` or MediaExtractor to calculate `fps_observed`.

## 4. IMU Rate & Jitter
*   **Requirement:** IMU rate & IMU jitter
*   **Status:** `PARTIAL`
*   **Evidence:** `SensorRecorder.kt` implements a statistical loop to calculate `jitter_stddev_ns` and `median_interval_ns`. However, `actualMeanRateHz` is calculated as `sampleCount / duration`. Mathematically, it should be `(sampleCount - 1) / duration`.
*   **Risk:** Minor mathematical inaccuracy in sampling frequency reporting.
*   **Required Action:** Change formula to `(sampleCount - 1).toDouble() / (durationNs / 1e9)`.

## 5. Synchronization & Timestamp Semantics
*   **Requirement:** Synchronization
*   **Status:** `PARTIAL`
*   **Evidence:** `MainActivity.kt` correctly captures `session_started_at_ns`, `video_finalized_at_ns` using `SystemClock.elapsedRealtimeNanos()`. `SensorRecorder.kt` captures `first_timestamp_ns` directly from hardware `SensorEvent.timestamp`.
*   **Risk:** `video_first_frame_timestamp` and `video_duration` are completely missing. The offset between IMU start and Video start is ambiguous without video frame timestamps.
*   **Required Action:** Extract video start timestamps from `VideoRecordEvent.Start` or use a `Camera2` capture callback to log the exact `elapsedRealtimeNanos` of the first frame.

## 6. MediaPipe Integration & Hand QC
*   **Requirement:** MediaPipe integration & Hand QC
*   **Status:** `PARTIAL`
*   **Evidence:** `HandAnalyzer.kt` is connected to CameraX `ImageAnalysis`. It tracks `totalFramesAnalyzed` and `framesWithHands` to calculate `hand_presence_percentage`. However, it does not track `frames_received`, `frames_dropped`, or `analysis_latency`.
*   **Risk:** If `ImageAnalysis` drops 90% of frames due to backpressure, the percentage is misleadingly confident.
*   **Required Action:** Track `ImageProxy` receipt vs analysis completion times.

## 7. Canonical Manifest, Hash, & Signature
*   **Requirement:** Canonical manifest, Manifest hash, Signature
*   **Status:** `IMPLEMENTED`
*   **Evidence:** `CanonicalManifestSerializer.kt` recursively sorts JSON keys alphabetically and normalizes primitives. `EpisodeManager.kt` builds the base payload, serializes it, hashes it (`manifest_sha256`), and signs it. The signature natively covers the canonical payload. Circular dependency is eliminated.
*   **Risk:** Minimal. Serialization rules must be rigorously tested against backend Python decoders.
*   **Required Action:** None immediately. Wait for backend integration tests.

## 8. Keystore & Security Level
*   **Requirement:** Keystore
*   **Status:** `IMPLEMENTED`
*   **Evidence:** `KeystoreManager.kt` uses `KeyInfo.isInsideSecureHardware` to accurately report `security_level` as `TRUSTED_ENVIRONMENT` or `SOFTWARE`, avoiding fake `StrongBox` claims on older APIs.
*   **Risk:** Minimal.
*   **Required Action:** None.

## 9. Background Hashing & Encryption
*   **Requirement:** Encryption / Background IO
*   **Status:** `IMPLEMENTED`
*   **Evidence:** `MainActivity.kt` wraps `episodeManager.finalizeEpisode` in `kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO)`. UI thread is no longer blocked during SHA-256 hashing.
*   **Risk:** `GlobalScope` is not lifecycle-aware and can leak if the Activity is destroyed mid-finalization.
*   **Required Action:** Refactor to use `lifecycleScope` or a dedicated `WorkManager` for finalization resilience.

## 10. Thermal Guard & Upload
*   **Requirement:** Thermal & Upload
*   **Status:** `MISSING`
*   **Evidence:** Thermal tracking and Tus.io refactoring were explicitly deferred per Phase 1-15 priority rules.
*   **Risk:** Overheating devices may silently throttle FPS without metadata recording.
*   **Required Action:** Implement `PowerManager.OnThermalStatusChangedListener` in Phase 16.
