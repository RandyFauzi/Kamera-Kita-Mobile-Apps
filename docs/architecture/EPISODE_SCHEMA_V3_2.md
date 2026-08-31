# Episode Canonical Schema V3.2

Every capture session must produce a canonical manifest matching this exact JSON structure before being signed and encrypted.

```json
{
  "schema_version": "3.2",
  "episode": {
    "episode_id": "...",
    "session_id": "...",
    "started_at_ns": 1700000000000000000,
    "ended_at_ns": 1700000005000000000
  },
  "capture": {
    "orientation_required": "LANDSCAPE",
    "orientation_integrity": "PASSED",
    "resolution": "1920x1080",
    "fps_requested": 30,
    "fps_observed": 29.97,
    "codec": "HEVC"
  },
  "camera": {
    "camera_id": "0",
    "lens_facing": "BACK",
    "sensor_orientation": 90,
    "ois_available": true,
    "intrinsics": null,
    "distortion": null
  },
  "imu": {
    "requested_rate_hz": 200,
    "actual_mean_rate_hz": 186.7,
    "median_interval_ns": 5321000,
    "jitter_stddev_ns": 820000,
    "sample_count": 933
  },
  "integrity": {
    "video_sha256": "...",
    "imu_sha256": "...",
    "manifest_sha256": "...",
    "signature_algorithm": "ES256",
    "signature": "..."
  },
  "device_trust": {
    "hardware_backed": true,
    "strongbox": true,
    "play_integrity_status": "SUCCESS"
  },
  "storage": {
    "encrypted": true
  }
}
```
