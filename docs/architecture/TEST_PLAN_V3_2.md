# Test Plan V3.2

## 1. Native Landscape Enforcement
- **Action**: Call `startRecording()` from Flutter while holding the device in portrait mode.
- **Expected**: Native layer explicitly rejects with `ORIENTATION_INVALID`.

## 2. In-Progress Rotation Violation
- **Action**: Start recording in Landscape. Rotate device to Portrait.
- **Expected**: Native `OrientationManager` halts recording safely, finalizes partial episode, and flags `orientation_integrity: "FAILED_ORIENTATION_POLICY"`. File is NOT discarded.

## 3. Cryptographic Pipeline Validation
- **Action**: Complete a capture.
- **Expected**: 
  - `ES256` Signature successfully generated over the Canonical Manifest.
  - Temporary files in `capture_tmp` are permanently deleted.
  - Final artifacts are AES-256-GCM encrypted.

## 4. IMU Rate Precision
- **Action**: Record 5 seconds of IMU at 200 Hz.
- **Expected**: Metadata shows `actual_mean_rate_hz`, `median_interval_ns`, and `jitter_stddev_ns`. `requested_rate_hz` is recorded separately.
