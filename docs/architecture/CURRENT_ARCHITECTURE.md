# Current Architecture Audit

## Overview
The current repository uses a very flat, simplistic architecture lacking separation of concerns, robust security, and deep hardware metadata extraction.

## Existing Native Kotlin Layer
- `MainActivity.kt`: God class managing MethodChannels and coordinating `CameraManager`, `SensorRecorder`, and `HandAnalyzer`.
- `CameraManager.kt`: Monolithic class managing CameraX lifecycle, UI surface textures, and video recording. It does not enforce orientation at the hardware level, nor does it extract rich `Camera2Interop` metadata.
- `SensorRecorder.kt`: Basic IMU data logger. Lacks nanosecond-level jitter analysis and exact `actual_rate` extraction.
- `HandAnalyzer.kt`: MediaPipe wrapper for hand detection (Live QA).

## Deficiencies Identified (Pre-V3.2)
1. **Security**: No Keystore implementation, no Play Integrity API, no local encryption.
2. **Architecture**: Monolithic managers instead of domain-driven structures (e.g., `episode`, `security`, `storage`).
3. **Orientation**: Missing native-level landscape enforcement; relies on Flutter UI.
4. **IMU Metrics**: Missing granular timestamps, stddev jitter, and true rate calculation.
5. **Storage Policy**: Lacks strict private temporary storage definitions.
