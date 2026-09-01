# CURRENT ARCHITECTURE AUDIT (V3.2 Active Snapshot)

## Overview
KameraKita Mobile is a Flutter application with a dedicated, custom native Kotlin domain-driven architecture under `android/app/src/main/kotlin/com/example/kamerakita_mobile/`.

---

## 1. Native Layer Structure & Status

| Domain | File Path | Actual Status | Implemented Features |
| :--- | :--- | :--- | :--- |
| **Camera** | `android/.../camera/CameraManager.kt` | **IMPLEMENTED** | CameraX Preview + VideoCapture, Camera2Interop (30 FPS lock, OIS disable), CameraCharacteristics extraction (Intrinsics, Focal length, Resolution, Sensor orientation), Surface leak cleanup. |
| **Orientation** | `android/.../camera/OrientationManager.kt` | **IMPLEMENTED** | Native hardware gravity/accelerometer listener enforcing `LANDSCAPE` recording rule and rejecting portrait recording. |
| **Sensors (IMU)** | `android/.../sensors/SensorRecorder.kt` | **IMPLEMENTED** | Real-time Accelerometer + Gyroscope raw logging to CSV with hardware nanosecond timestamps (`SensorEvent.timestamp`), actual mean rate `(N-1)/duration` calculation, median interval, min/max intervals, and jitter standard deviation. |
| **Integrity & Hash**| `android/.../security/HashManager.kt` | **IMPLEMENTED** | Streaming SHA-256 for video file, IMU CSV, and UTF-8 Canonical Manifest string. |
| **Signing (Keystore)**| `android/.../security/KeystoreManager.kt` | **IMPLEMENTED** | Hardware-backed ECDSA (`secp256r1` / `ES256`) key management via AndroidKeyStore with `KeyInfo` security-level extraction (`TRUSTED_ENVIRONMENT` vs `SOFTWARE`). |
| **Encryption** | `android/.../security/EncryptionManager.kt` | **IMPLEMENTED** | Local AES-256-GCM encryption support for artifacts. |
| **Perception QA** | `android/.../HandAnalyzer.kt` | **IMPLEMENTED** | MediaPipe Hand Landmarker integration on CameraX `ImageAnalysis` tracking `frames_received`, `frames_analyzed`, `frames_dropped`, `frames_with_hands`, and latency. |
| **Episode Manager**| `android/.../episode/EpisodeManager.kt`<br>`android/.../episode/CanonicalManifestSerializer.kt` | **IMPLEMENTED** | Deterministic canonical JSON serialization (RFC 8785 style), non-circular signing pipeline, monotonic session timestamps (`elapsedRealtimeNanos`). |
| **Storage** | `android/.../storage/PrivateCaptureStorage.kt` | **IMPLEMENTED** | Sandboxed internal storage for temporary video and IMU files. |
| **Coordination** | `android/.../MainActivity.kt` | **IMPLEMENTED** | MethodChannels/EventChannels bridging Flutter UI to Native, offloading heavy hashing and encryption to Kotlin Coroutines (`Dispatchers.IO`). |

---

## 2. Status of Phase 3 / Future Scope Components

| Component | Status | Note |
| :--- | :--- | :--- |
| **Play Integrity API** | **NOT IMPLEMENTED (Planned Phase 3)** | Explicitly marked as `NOT_CONFIGURED` in manifest schema. |
| **C2PA Provenance** | **NOT IMPLEMENTED (Planned Phase 3)** | Requires C2PA C++ JNI bridge; scheduled for post-pilot phase. |
| **Transparency Log / Consent Ledger** | **NOT IMPLEMENTED (Planned Phase 3)** | Cloud-side / backend ledger integration. |
| **Resumable Upload (tus.io)** | **NOT IMPLEMENTED (Planned Phase 3)** | Currently using standard multipart upload repository. |

---

## 3. Directory Mapping Clarification (Section 39 Blueprint vs Flutter Reality)
In Blueprint Section 39, the conceptual architecture was outlined as:
```text
app/
├── camera/
├── sensors/
├── integrity/
└── provenance/
```
In this Flutter production codebase, this exact native domain architecture is located at:
```text
android/app/src/main/kotlin/com/example/kamerakita_mobile/
├── camera/
│   ├── CameraManager.kt
│   └── OrientationManager.kt
├── sensors/
│   ├── SensorRecorder.kt
│   └── SensorRateAnalyzer.kt
├── security/
│   ├── HashManager.kt
│   ├── KeystoreManager.kt
│   └── EncryptionManager.kt
├── episode/
│   ├── EpisodeManager.kt
│   └── CanonicalManifestSerializer.kt
└── storage/
    └── PrivateCaptureStorage.kt
```
