# Security Architecture V3.2

## Data Flow Pipeline
1. **Capture**: Native `CameraManager` writes plain MP4 to `app-private-storage/capture_tmp/<episode_id>.partial`.
2. **Streaming Hash**: `HashManager` performs sequential streaming read to compute SHA-256 for video and IMU CSV.
3. **Manifest Generation**: `EpisodeManager` generates the Canonical JSON Manifest.
4. **Manifest Signing**: `KeystoreManager` signs the Manifest SHA-256 using an `ES256` key (P-256).
5. **Encryption**: `EncryptionManager` applies AES-256-GCM encryption to the video, IMU, and Manifest files.
6. **Cleanup**: Temporary plaintext files are aggressively deleted from private storage ONLY AFTER successful encryption.
7. **Upload**: Encrypted artifacts are passed to `UploadQueueManager`.

## Keystore Policy
- **Primary Algorithm**: `ES256` (ECDSA P-256). No `Ed25519` auto-fallback.
- **Hardware Requirement**: Prioritize `StrongBox`. If unavailable, use `Trusted Environment`. If missing, fallback to `Software (LOW TRUST)` and flag explicitly in metadata.

## Play Integrity
- App does not make final trust decisions.
- `IntegrityManager` prepares token provider, requests token, and saves it in the payload. Backend decrypts/verifies. States: `NOT_AVAILABLE`, `NOT_CONFIGURED`, `REQUESTED`, `SUCCESS`, `FAILED`, `EXPIRED`, `NETWORK_ERROR`.
