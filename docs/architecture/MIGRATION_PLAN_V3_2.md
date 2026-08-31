# Migration Plan V3.2

## Phase 1: Core Engine Rebuild
1. **Refactor Native Layer**: Break `CameraManager` into `CameraCapabilityManager`, `CameraMetadataCollector`, `VideoEncoderManager`, and `OrientationManager`.
2. **Sensor Analytics**: Implement `SensorRateAnalyzer` for nanosecond jitter and interval calculations.
3. **Security Injection**: Implement `KeystoreManager`, `EncryptionManager`, and `IntegrityManager`.
4. **Episode Management**: Create `EpisodeManager` to orchestrate metadata, hashing, signing, and encryption.
5. **Flutter Integration**: Update MethodChannels and UI to handle `ORIENTATION_INVALID` rejection from Native layer.

## Post-Phase 1 Deployment
- **No Production Disruption**: Run this new core in parallel with existing Minute Data collection.
- **Limited Pilot**: 10-15 contributors will test the Phase 1 build. Measure IMU actual rates, thermal events, and upload reliability.
