# MANIFEST SIGNING SPECIFICATION V3.2

## 1. Overview
To guarantee the physical integrity of egocentric dataset captures, the KameraKita application employs a strict, deterministic Canonical JSON serialization and signing scheme. This ensures that any independent verifier can exactly reconstruct the signed bytes without ambiguity.

## 2. Signing Payload Construction
The signing payload is constructed via the following steps:

1. **Artifact Hashing**:
   - The finalized video file (MP4) is hashed via SHA-256 (`video_sha256`).
   - The raw IMU CSV file is hashed via SHA-256 (`imu_sha256`).

2. **Base Manifest Construction**:
   - A `baseManifest` object is built containing all metadata blocks: `episode`, `capture`, `camera`, `imu`, `device_trust`, and `storage`.
   - The `integrity` block is added to `baseManifest`, containing `video_sha256`, `imu_sha256`, and `signature_algorithm` (e.g., `ES256`).
   - **Crucially**, the `integrity` block does *not* yet contain the `manifest_sha256` or `signature` keys.

3. **Canonical Serialization**:
   - The `baseManifest` object is serialized into a UTF-8 string using the `CanonicalManifestSerializer`.
   - Serialization rules:
     - Map keys are sorted alphabetically (lexicographical order).
     - No whitespace is emitted between JSON tokens (e.g., `{"key":"value"}`).
     - Floating point numbers are represented consistently (e.g., stripped trailing `.0` if exact integer).

## 3. Cryptographic Signatures
4. **Hashing the Manifest**:
   - The canonical UTF-8 bytes are hashed via SHA-256 to produce `manifest_sha256`.

5. **Signing the Payload**:
   - The exact same canonical UTF-8 bytes are signed using the Android Keystore private key (`ES256` / `secp256r1`).
   - The resulting signature is hex-encoded.

6. **Final Assembly**:
   - The `manifest_sha256` and `signature` values are appended to the `integrity` block of the final JSON output.

## 4. Independent Verification
A third-party verifier must follow these exact steps:
1. Parse the final manifest JSON.
2. Extract and remove the `manifest_sha256` and `signature` keys from the `integrity` block.
3. Re-serialize the remaining object using the strict Canonical JSON rules defined above.
4. Hash the resulting UTF-8 string via SHA-256 and verify it matches the extracted `manifest_sha256`.
5. Verify the extracted `signature` against the canonical UTF-8 bytes using the public key defined for the episode/device.

## 5. Security Properties
- **Does the signature cover video_sha256?** YES.
- **Does the signature cover imu_sha256?** YES.
- **Does the signature cover manifest_sha256?** NO.
- **Does the signature cover itself?** NO.
- **Is there a circular dependency?** NO. The payload is sealed prior to hashing/signing.
- **Tamper Resistance:** Modifying any value in the final manifest (except `signature` or `manifest_sha256`) will alter the canonical payload and invalidate the ECDSA signature. Modifying `manifest_sha256` will break the cryptographic hash match against the payload.
