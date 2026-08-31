# KAMERAKITA
## ENGINEERING MASTER DIRECTIVE
### Upgrade Capture System → Physical Intelligence Data Infrastructure V3.2

**Document Type:** Engineering Implementation Specification  
**Source of Truth:** KameraKita Blueprint V3.2  
**Implementation Target:** Existing KameraKita system → Production-grade Physical AI Capture Infrastructure  
**Primary Platform:** Android  
**Critical Requirement:** **ALL CAMERA RECORDINGS MUST BE LANDSCAPE**

---

# 1. EXECUTIVE DIRECTIVE

You are not being asked to build a generic camera application.

You are upgrading KameraKita into a **trusted Physical AI data capture infrastructure**.

The application must capture real-world egocentric human activity together with enough technical evidence to allow downstream systems to evaluate:

- temporal quality
- device integrity
- capture provenance
- consent / jurisdiction
- sensor quality
- recording reliability
- dataset suitability

The system must therefore treat a recording as a structured **capture episode**, not simply an MP4 file.

The target architecture is:

```text
CONTRIBUTOR
    │
    ▼
CAMERA + IMU
    │
    ▼
CAPTURE SESSION
    │
    ├── Video
    ├── IMU
    ├── Timestamp
    ├── Device metadata
    ├── Camera metadata
    └── Session metadata
    │
    ▼
LOCAL INTEGRITY
    │
    ├── SHA-256
    ├── Hardware-backed signing
    └── Encrypted local storage
    │
    ▼
DEVICE / APP INTEGRITY
    │
    └── Play Integrity
    │
    ▼
PROVENANCE
    │
    └── C2PA
    │
    ▼
CONSENT / LINEAGE
    │
    └── Transparency Log
    │
    ▼
RESUMABLE UPLOAD
    │
    └── tus.io
    │
    ▼
BACKEND
```

This architecture is based directly on Blueprint V3.2.

---

# 2. MOST IMPORTANT BUSINESS CONSTRAINT

## DO NOT BREAK CURRENT PRODUCTION OPERATIONS

KameraKita currently has:

- active Centific AI contract
- execution beginning September 2026
- target 20,500 recording hours
- Indonesia ≈80%
- Malaysia ≈20%
- Minute Data currently used for capture
- 200+ active contributors
- approximately 8,000+ hours/month

Therefore:

> **The new KameraKita app MUST NOT replace Minute Data immediately.**

Build the application as a parallel system.

The existing production pipeline must remain operational while the new application is tested.



---

# 3. MIGRATION STRATEGY

Implement exactly this progression:

```text
CURRENT PRODUCTION
Minute Data
     │
     │
     ├───────────────┐
     │               │
     ▼               ▼
Production       KameraKita
Contract         New Capture App
     │               │
     │               ▼
     │           Pilot 10–15 users
     │               │
     │               ▼
     │        Compare quality/reliability
     │               │
     └───────────────┘
                     │
                     ▼
              Gradual Migration
```

### Phase 1

Build:

- Camera
- IMU
- local hashing
- Keystore signing
- Play Integrity

Do not disturb production.

Target:

**4–8 weeks.**



### Phase 2

Pilot:

**10–15 contributors**

Run:

```text
Minute Data
     VS
KameraKita App
```

Compare:

- upload reliability
- actual IMU rate
- failed recordings
- missing recordings
- capture stability
- thermal behavior
- data integrity



### Phase 3

Add:

- C2PA
- transparency log
- jurisdiction-aware consent
- resumable upload



### Phase 4

Only after September commitment is safe:

> gradually migrate contributors.



---

# 4. HARD REQUIREMENT — LANDSCAPE ONLY

## THIS REQUIREMENT IS NON-NEGOTIABLE

Every recording must be captured in:

```text
LANDSCAPE
```

Portrait recording is not accepted.

Valid:

```text
1920 × 1080
2560 × 1440
3840 × 2160
```

Invalid:

```text
1080 × 1920
1440 × 2560
2160 × 3840
```

---

# 5. LANDSCAPE UX

When camera screen opens:

### Landscape

Show:

```text
READY
Landscape Mode
```

Recording button:

```text
ENABLED
```

---

### Portrait

Show a large orientation instruction:

```text
ROTATE YOUR PHONE

Landscape recording is required
```

Recording button:

```text
DISABLED
```

The user must not be able to start a valid capture session in portrait.

---

# 6. DO NOT FAKE LANDSCAPE

Do NOT solve portrait recording by:

- cropping
- stretching
- rotating the final video
- changing EXIF only
- changing metadata only

The actual capture pipeline must operate in landscape.

Canonical validation:

```text
encoded_width > encoded_height
```

If the resulting video violates this:

```text
capture_validation = FAILED
```

---

# 7. ORIENTATION STATE MACHINE

Create:

```text
OrientationManager
```

States:

```text
UNKNOWN
PORTRAIT
LANDSCAPE_LEFT
LANDSCAPE_RIGHT
```

Rules:

```text
UNKNOWN
   ↓
PORTRAIT
   ↓
BLOCK RECORDING
```

```text
UNKNOWN
   ↓
LANDSCAPE_LEFT
   ↓
ALLOW RECORDING
```

```text
UNKNOWN
   ↓
LANDSCAPE_RIGHT
   ↓
ALLOW RECORDING
```

Both landscape directions are valid.

---

# 8. RECORDING ORIENTATION DURING ACTIVE SESSION

If the user rotates the phone during recording:

```text
LANDSCAPE
    ↓
ORIENTATION CHANGE
    ↓
PORTRAIT
```

The system must not silently continue as if nothing happened.

Implement a clearly defined policy:

```text
Orientation violation
       ↓
Recording state protected
       ↓
Warn contributor
       ↓
Mark episode orientation integrity
```

Do not silently transform frames.

The final episode must expose orientation integrity.

---

# 9. CAMERA ARCHITECTURE

Use:

```text
CameraX
+
Camera2Interop
```

CameraX should manage:

- lifecycle
- camera state
- threading
- normal capture operations

Camera2Interop should expose lower-level metadata needed for temporal and camera-quality analysis.

Blueprint V3.2 specifically identifies:

- sensor timestamp
- OIS
- rolling-shutter information

as important metadata.

---

# 10. CAMERA METADATA

Do not throw away available camera metadata.

Create a canonical camera metadata object.

Minimum conceptual fields:

```json
{
  "camera_id": "...",
  "camera_facing": "...",
  "sensor_orientation": "...",
  "resolution": "...",
  "fps_requested": 30,
  "fps_actual": "...",
  "timestamp_source": "...",
  "ois": "...",
  "rolling_shutter": "..."
}
```

The exact implementation may differ by device capability.

Never assume every Android device exposes identical metadata.

---

# 11. IMU CAPTURE

Use:

```text
Android SensorManager
```

Capture relevant IMU streams supported by the device.

Critical rule:

> **Requested sampling rate is NOT the actual sampling rate.**

The system must measure the actual received sensor rate in real time.

Example:

```text
Requested:
200 Hz

Actual:
157 Hz
```

The dataset must record:

```text
requested_rate = 200
actual_rate = 157
```

Do not report 200 Hz simply because the application requested 200 Hz.

Blueprint V3.2 explicitly requires actual-rate measurement because Android devices may restrict rates at the HAL level.

---

# 12. IMU DATA MODEL

Each IMU sample should contain conceptually:

```json
{
  "timestamp": "...",
  "sensor_type": "...",
  "x": "...",
  "y": "...",
  "z": "...",
  "accuracy": "..."
}
```

Episode-level metadata:

```json
{
  "requested_sampling_rate_hz": 200,
  "actual_sampling_rate_hz": 157,
  "sample_count": 123456,
  "drop_count": 17
}
```

---

# 13. VIDEO ENCODING

Use:

```text
Android MediaCodec
```

Prefer:

```text
hardware encoder
```

Default:

```text
HEVC
```

AV1:

```text
OPTIONAL
```

Do not require AV1 on every device.

Blueprint V3.2 explicitly recommends HEVC as the default while treating AV1 hardware encoding as device-profile dependent.

---

# 14. FPS POLICY

Default:

```text
30 FPS
```

Use:

```text
60 FPS
```

only for tasks requiring higher temporal resolution, especially:

- dexterous manipulation
- fast hand movement
- detailed object interaction

60 FPS should also depend on device capability.

Blueprint V3.2 specifically recommends 60 FPS only for dexterous manipulation and devices that pass the required device profile.

---

# 15. GOP

Recommended:

```text
1–2 seconds
```

Do NOT make:

```text
ALL-I
```

mandatory.

Reason:

```text
Storage efficiency
+
Random frame access
```

must be balanced.



---

# 16. BITRATE PROFILES

Recommended starting profiles:

### Manipulation

```text
1080p60 HEVC
≈40–50 Mbps
```

### Navigation / scene understanding

```text
≈15–20 Mbps
```

These are starting engineering profiles, not universal guarantees.

The app should expose device capability rather than blindly forcing impossible encoder settings.

---

# 17. THERMAL GUARD

Thermal management is a system function.

Do not treat thermal information as passive metadata only.

Implement:

```text
Thermal Monitor
      ↓
NORMAL
      ↓
ELEVATED
      ↓
CRITICAL
```

When thermal state becomes unsafe:

```text
reduce capture load
        OR
reduce resolution
        OR
stop capture safely
```

depending on the severity.

The system must protect:

- device
- recording integrity
- contributor
- battery
- data completeness

Blueprint V3.2 explicitly requires thermal guard enforcement.

---

# 18. EDGE PERCEPTION

Perception is OPTIONAL.

It exists primarily for:

> LIVE QA / CONTRIBUTOR GUIDANCE

It is NOT the authoritative annotation pipeline.

---

## Hand Tracking

Use:

```text
MediaPipe Hand Landmarker
```

Purpose:

```text
21 hand keypoints
+
real-time guidance
```

Do not treat live hand tracking as final ground-truth annotation.

Blueprint V3.2 explicitly positions it as live guidance rather than final annotation.

---

# 19. VIO / 6DoF

For mobile VIO:

```text
ARCore
```

is preferred over implementing ORB-SLAM3 directly for the first production version.

Reason:

```text
Production maturity
+
mobile integration
+
smaller engineering burden
```



Do not build a custom SLAM system during the first migration phase.

---

# 20. DEVICE TRUST MODEL

Do not represent trust as:

```text
device_verified = true
```

Instead create a trust structure.

Conceptually:

```json
{
  "app_integrity": "...",
  "device_integrity": "...",
  "play_integrity": "...",
  "key_security_level": "...",
  "hardware_backed": true,
  "strongbox_available": true
}
```

---

# 21. ANDROID KEYSTORE

Signing key must be generated inside:

```text
Android Keystore
```

Prefer:

```text
hardware-backed
```

and:

```text
StrongBox
```

when supported.

Key must be:

```text
non-exportable
```

The purpose is to bind cryptographic signing to the device security environment rather than relying solely on application-level signing.

Blueprint V3.2 explicitly moves signing from application-level security to hardware-backed Android Keystore.

---

# 22. IMPORTANT SECURITY LANGUAGE

Do NOT implement or document this as:

> “Hardware key proves the physical camera sensor is authentic.”

Instead:

> Hardware-backed signing provides evidence about the security state and key provenance of the capture device.

The system reduces provenance spoofing risk.

It does not make manipulation mathematically impossible.

---

# 23. PLAY INTEGRITY

Before a session can become eligible for high-trust certification:

```text
Play Integrity
      ↓
Verdict
      ↓
Trust evaluation
      ↓
Capture certification
```

V3.2 specifies:

```text
MEETS_STRONG_INTEGRITY
```

as the desired condition for sessions intended to receive the highest certification.

Do not pretend this is an absolute anti-spoofing guarantee.

It is a risk-reduction mechanism.

---

# 24. CONTENT PROVENANCE

Use:

```text
c2pa-android
```

Do not implement C2PA from scratch.

C2PA should be responsible for:

```text
content provenance
+
capture manifest
+
cryptographically verifiable provenance
```

Do not use C2PA as a replacement for:

```text
privacy
+
consent
+
legal compliance
```

Blueprint V3.2 deliberately separates these concerns.

---

# 25. CONSENT / LINEAGE

Do not build a blockchain-based smart contract system for the initial implementation.

Use:

```text
Transparency Log
```

with:

```text
Sigstore Rekor-compatible architecture
```

and append-only / verifiable log semantics.

Purpose:

```text
Consent event
+
data lineage
+
release history
+
auditability
```

V3.2 explicitly replaces the earlier smart-contract approach with transparency logging because it is more practical for a small engineering team.

---

# 26. JURISDICTION-AWARE CONSENT

Because the current operation spans:

```text
Indonesia ≈80%
Malaysia ≈20%
```

the consent model must include jurisdiction.

Do not implement:

```text
global_consent_policy
```

only.

Instead:

```json
{
  "jurisdiction": "ID",
  "consent_version": "...",
  "consent_timestamp": "...",
  "policy_version": "..."
}
```

or:

```text
ID
MY
```

depending on the collection.

V3.2 explicitly requires jurisdiction-specific privacy handling because Indonesia's UU PDP and Malaysia's PDPA are different regimes.

---

# 27. LOCAL ENCRYPTION

Before upload:

```text
RAW CAPTURE
     ↓
ENCRYPTED LOCAL STORAGE
```

Use:

```text
Jetpack Security
+
EncryptedFile
```

Keys should be protected through:

```text
Android Keystore
```

The goal:

> A stolen phone/storage location should not trivially expose unuploaded raw capture data.



---

# 28. STREAMING HASH

Do not wait until the entire video has finished writing and then re-read a multi-gigabyte file just to hash it.

Implement:

```text
Capture
   ↓
Chunk
   ↓
SHA-256
   ↓
Next chunk
```

Maintain:

```text
episode_hash
```

during capture/finalization.

V3.2 explicitly recommends chunked streaming SHA-256.

---

# 29. UPLOAD

Use:

```text
tus.io
```

for resumable uploads.

The system must tolerate:

- mobile network drops
- app backgrounding
- intermittent connectivity
- partial upload
- retry
- device reconnection

Do NOT implement:

```text
single giant HTTP upload
```

as the only mechanism.



---

# 30. EPISODE CONCEPT

A recording must become an:

```text
EPISODE
```

not merely:

```text
video.mp4
```

Conceptually:

```text
EPISODE
│
├── Identity
├── Contributor
├── Device
├── Camera
├── Orientation
├── Video
├── IMU
├── Timing
├── Integrity
├── Provenance
├── Consent
├── Upload
└── Quality
```

---

# 31. MINIMUM EPISODE IDENTITY

Every episode must have:

```text
episode_id
session_id
contributor_id
device_id
capture_started_at
capture_ended_at
```

Never use filename as the primary identity.

---

# 32. TEMPORAL INTEGRITY

Every relevant stream must preserve timing information.

At minimum:

```text
timestamp
clock/source information
sample/frame index
```

For IMU:

```text
actual sampling rate
```

For video:

```text
actual frame behavior
```

The system must preserve evidence rather than fabricate synchronization precision.

---

# 33. QUALITY MODEL

Do not reduce quality to:

```text
quality_score = 87
```

Store measurable properties.

Examples:

```text
video_integrity
imu_integrity
timestamp_integrity
orientation_integrity
device_trust
upload_integrity
thermal_events
```

The buyer/backend should be able to understand **why** an episode is accepted or rejected.

---

# 34. CAPTURE CERTIFICATION

At the end of recording:

```text
STOP
 ↓
Validate
 ↓
Calculate integrity
 ↓
Collect device evidence
 ↓
Finalize manifest
 ↓
Encrypt / persist
 ↓
Upload
```

The episode should receive a certification state.

Example:

```text
CAPTURED
↓
VALIDATING
↓
VALID
```

or:

```text
CAPTURED
↓
VALIDATING
↓
REJECTED
```

Reasons must be machine-readable.

---

# 35. FAILURE CODES

Create explicit failure codes.

Examples:

```text
ORIENTATION_INVALID
CAMERA_FAILURE
IMU_UNAVAILABLE
IMU_RATE_LOW
ENCODER_FAILURE
THERMAL_ABORT
STORAGE_FAILURE
HASH_FAILURE
SIGNING_FAILURE
ATTESTATION_FAILURE
UPLOAD_INTERRUPTED
UPLOAD_FAILED
CORRUPTED_EPISODE
```

Do not return only:

```text
Upload failed
```

The engineering/backend system needs actionable failure reasons.

---

# 36. DEVICE PROFILE

Every supported Android device should have a capability profile.

Conceptually:

```json
{
  "device_model": "...",
  "camera_capabilities": {},
  "encoder_capabilities": {},
  "imu_capabilities": {},
  "integrity_capabilities": {},
  "thermal_capabilities": {}
}
```

This profile determines:

```text
30 vs 60 FPS
HEVC availability
AV1 availability
StrongBox availability
sensor rates
camera resolution
```

Do not assume all Android devices behave identically.

---

# 37. DO NOT BUILD THESE FEATURES YET

This is critical.

For the current Centific-driven phase, DO NOT spend engineering resources on:

```text
❌ Full LeRobot compiler
❌ Full RLDS compiler
❌ Full MCAP export system
❌ Independent buyer portal
❌ Marketplace
❌ Campaign engine
❌ Large annotation marketplace
❌ Complex VLA training pipeline
❌ Custom SLAM engine
❌ Custom C2PA implementation
❌ Blockchain consent system
```

These are future capabilities.

Blueprint V3.2 explicitly says dataset compiler, campaign engine, and independent buyer portal should not be built yet unless KameraKita later moves toward direct buyers.

---

# 38. CURRENT ENGINEERING PRIORITY

The priority order is:

```text
1. RELIABLE CAPTURE
        ↓
2. LANDSCAPE ENFORCEMENT
        ↓
3. IMU
        ↓
4. TEMPORAL DATA
        ↓
5. LOCAL HASHING
        ↓
6. HARDWARE-BACKED SIGNING
        ↓
7. PLAY INTEGRITY
        ↓
8. C2PA
        ↓
9. CONSENT / TRANSPARENCY LOG
        ↓
10. RESUMABLE UPLOAD
        ↓
11. PILOT VALIDATION
        ↓
12. GRADUAL MIGRATION
```

---

# 39. RECOMMENDED APPLICATION ARCHITECTURE

Keep modules isolated.

```text
app/
│
├── camera/
│   ├── CameraController
│   ├── CameraCapabilities
│   ├── OrientationManager
│   └── VideoEncoder
│
├── sensors/
│   ├── IMUManager
│   ├── SensorRateMonitor
│   └── SensorRecorder
│
├── integrity/
│   ├── HashManager
│   ├── SigningManager
│   ├── DeviceAttestation
│   └── PlayIntegrityManager
│
├── provenance/
│   └── C2PAManager
│
├── consent/
│   ├── ConsentManager
│   └── JurisdictionManager
│
├── storage/
│   ├── EncryptedStorage
│   ├── EpisodeStorage
│   └── ManifestStorage
│
├── upload/
│   ├── UploadManager
│   ├── TusClient
│   └── RetryManager
│
├── quality/
│   ├── CaptureValidator
│   ├── OrientationValidator
│   ├── SensorValidator
│   └── EpisodeValidator
│
└── episode/
    ├── EpisodeManager
    ├── EpisodeManifest
    └── EpisodeFinalizer
```

The exact package names may differ according to the existing repository.

---

# 40. BACKEND RESPONSIBILITIES

The backend must never blindly trust the client.

Client:

```text
capture
+
collect evidence
+
sign
+
hash
+
upload
```

Backend:

```text
receive
+
verify
+
validate
+
store
+
audit
```

The backend should verify:

```text
episode identity
hash
signature
attestation information
manifest
upload completeness
consent metadata
jurisdiction
```

---

# 41. TRUST BOUNDARY

Architecture:

```text
              DEVICE
┌──────────────────────────────┐
│ Camera                       │
│ IMU                          │
│ Capture App                  │
│ Keystore                     │
│ Local Encryption             │
│ Hashing                      │
└──────────────┬───────────────┘
               │
          UNTRUSTED NETWORK
               │
               ▼
┌──────────────────────────────┐
│            BACKEND           │
│                              │
│ Verification                │
│ Signature validation         │
│ Integrity validation         │
│ Episode validation           │
│ Consent/lineage              │
│ Storage                      │
└──────────────────────────────┘
```

Never assume:

> “The client says it is valid, therefore it is valid.”

---

# 42. OBSERVABILITY

Engineering must be able to answer:

```text
How many recordings started?
How many completed?
How many failed?
Why did they fail?
Average upload time?
Upload retry rate?
Actual IMU rate?
Average FPS?
Thermal abort rate?
Device model distribution?
Integrity failure rate?
Attestation failure rate?
```

Create telemetry around the capture pipeline.

But:

> **Do not collect unnecessary personal content through telemetry.**

Telemetry should describe system behavior, not secretly duplicate the user's captured content.

---

# 43. PILOT BENCHMARK

The 10–15 contributor pilot must compare KameraKita against Minute Data.

Create a benchmark dashboard:

| Metric | Minute Data | KameraKita |
|---|---:|---:|
| Capture success | — | — |
| Upload success | — | — |
| Retry rate | — | — |
| Missing episodes | — | — |
| IMU actual Hz | — | — |
| Thermal failures | — | — |
| Video corruption | — | — |
| Average upload duration | — | — |
| Integrity verification | — | — |
| Contributor friction | — | — |

Do not migrate based on subjective impressions.

Migrate based on measured results.

---

# 44. DEFINITION OF DONE — PHASE 1

Phase 1 is complete only when:

```text
[ ] Existing production remains untouched
[ ] Android app builds reliably
[ ] Landscape-only capture works
[ ] Portrait capture is blocked
[ ] Camera capture works across target devices
[ ] IMU capture works
[ ] Actual IMU rate is measured
[ ] Video is encoded using supported hardware encoder
[ ] SHA-256 hashing works
[ ] Local encrypted storage works
[ ] Android Keystore signing works
[ ] Play Integrity integration works
[ ] Episode manifest is generated
[ ] Thermal guard works
[ ] Failed recordings are recoverable/diagnosable
[ ] Upload architecture is prepared
```

---

# 45. DEFINITION OF DONE — PHASE 2

Pilot-ready when:

```text
[ ] 10–15 contributors successfully onboarded
[ ] Minute Data comparison completed
[ ] No critical production disruption
[ ] Capture reliability measured
[ ] Upload reliability measured
[ ] IMU rate measured
[ ] Device compatibility matrix created
[ ] Thermal behavior measured
[ ] Failure codes validated
[ ] Contributor UX validated
```

---

# 46. DEFINITION OF DONE — PHASE 3

Integrity-ready when:

```text
[ ] C2PA manifest generated
[ ] C2PA verification tested
[ ] Hardware-backed signing tested
[ ] Play Integrity verdict recorded
[ ] Consent ledger implemented
[ ] Jurisdiction field implemented
[ ] Indonesia/Malaysia flows separated where required
[ ] Resumable tus.io upload operational
[ ] Backend verification operational
```

---

# 47. SECURITY DISCLAIMER

The implementation must never claim:

```text
"Impossible to spoof"
"100% authentic"
"Guaranteed sensor authenticity"
"Machine unlearning guaranteed"
```

Instead use:

```text
verified
attested
hardware-backed
cryptographically verifiable
risk-reduced
auditable
```

Blueprint V3.2 explicitly recognizes that hardware attestation and Play Integrity reduce risk but do not eliminate spoofing completely.

---

# 48. MACHINE UNLEARNING

Do not implement a product promise:

> "If contributor revokes consent, all trained AI models will forget the data."

That is not guaranteed.

The system must instead guarantee what it can actually control:

```text
future dataset release
+
future training pipeline
+
data lineage
+
revocation record
```

Revocation does not automatically mean an already-trained model has forgotten the data.

Blueprint V3.2 explicitly corrects this assumption.

---

# 49. DATA MODEL PHILOSOPHY

Do not prematurely lock the system to:

```text
LeRobot
RLDS
MCAP
```

during this phase.

Instead create:

```text
KameraKita Episode Model
```

as the internal canonical representation.

Future:

```text
KameraKita Episode
       │
       ├── LeRobot
       ├── RLDS
       ├── MCAP
       └── Buyer-specific export
```

But the compiler is **future scope**, not current MVP scope.

---

# 50. ENGINEERING RULE

Whenever implementing a feature, ask:

### Question 1

Does this improve:

```text
capture reliability?
```

### Question 2

Does this improve:

```text
data integrity?
```

### Question 3

Does this improve:

```text
trust / provenance?
```

### Question 4

Does this improve:

```text
measurable data quality?
```

### Question 5

Does this reduce:

```text
production risk?
```

If the answer is no to all five:

> **Do not build it during this phase.**

---

# 51. FINAL SYSTEM TARGET

The resulting application should conceptually become:

```text
                 KAMERAKITA
                      │
          PHYSICAL AI CAPTURE APP
                      │
        ┌─────────────┴─────────────┐
        │                           │
     CAPTURE                    TRUST
        │                           │
   ┌────┼────┐             ┌───────┼───────┐
   │    │    │             │       │       │
 Video IMU Camera       Keystore Integrity C2PA
   │    │    │             │       │       │
   └────┼────┘             └───────┼───────┘
        │                          │
        └──────────┬───────────────┘
                   │
             EPISODE PACKAGE
                   │
        ┌──────────┼───────────┐
        │          │           │
      HASH      CONSENT     METADATA
        │          │           │
        └──────────┼───────────┘
                   │
             RESUMABLE UPLOAD
                   │
                   ▼
                BACKEND
                   │
                   ▼
             VERIFIED DATA
```

---

# 52. MOST IMPORTANT PRODUCT PRINCIPLE

KameraKita should NOT compete with:

```text
Build AI
Claru
large global data vendors
```

on:

```text
number of videos
number of contributors
raw hours alone
```

V3.2 explicitly identifies generic egocentric data as highly competitive/commoditized and positions KameraKita around commissioned collection and verifiable quality.

Therefore the engineering system should optimize for:

```text
RELIABILITY
+
MEASURABLE QUALITY
+
DEVICE TRUST
+
TEMPORAL INTEGRITY
+
PROVENANCE
+
AUDITABILITY
+
LOCAL/JURISDICTIONAL COMPLIANCE
```

not simply:

```text
MORE VIDEO
```

---

# 53. IMMEDIATE TASK FOR AI CODING AGENT

Before writing implementation code:

### STEP 01

Audit the existing repository.

Return:

```text
Architecture
Framework
Modules
Database
Storage
Authentication
Camera implementation
Upload implementation
Existing APIs
Existing data model
Existing dependencies
Build system
```

### STEP 02

Create:

```text
CURRENT_ARCHITECTURE.md
```

### STEP 03

Create:

```text
MIGRATION_PLAN_V3_2.md
```

### STEP 04

Create:

```text
DEVICE_CAPABILITY_MATRIX.md
```

### STEP 05

Create:

```text
EPISODE_SCHEMA.md
```

### STEP 06

Create:

```text
CAPTURE_INTEGRITY_SPEC.md
```

### STEP 07

Only after the above is reviewed:

```text
IMPLEMENT PHASE 1
```

Do NOT immediately rewrite the application.

---

# 54. FINAL ENGINEERING COMMAND

**Preserve the existing production system.**

**Build the new capture infrastructure in parallel.**

**Enforce landscape-only recording at the capture layer.**

**Measure actual sensor behavior rather than trusting requested configuration.**

**Use hardware-backed Android Keystore where available.**

**Use Play Integrity as a trust signal, not an absolute guarantee.**

**Use C2PA for provenance.**

**Use transparency logging for consent/lineage.**

**Encrypt local data before upload.**

**Hash data during capture/finalization using streaming SHA-256.**

**Use resumable uploads.**

**Measure every critical failure.**

**Pilot with 10–15 contributors.**

**Compare against Minute Data.**

**Only migrate after the new system demonstrates measurable reliability and quality.**

**Do not build future-platform features before the capture infrastructure is production-grade.**

The objective of V3.2 is not to build the largest data platform immediately.

The objective is to build a **trustworthy, measurable, production-ready Physical AI capture node** that can safely enter the existing enterprise data supply chain.