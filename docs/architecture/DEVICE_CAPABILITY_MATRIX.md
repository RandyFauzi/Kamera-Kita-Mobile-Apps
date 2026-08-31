# Device Capability Matrix

This matrix defines how the application behaves based on device hardware capabilities, specifically regarding Keystore and Camera features.

## Security Capability Level
| Level | Condition | Keystore Hardware | Fallback Logic |
|---|---|---|---|
| **STRONGBOX** | `hasSystemFeature(FEATURE_STRONGBOX_KEYSTORE)` | Hardware-backed (StrongBox) | Primary target. |
| **TRUSTED_ENV** | TEE available | Hardware-backed (TEE) | Used if StrongBox is unavailable. |
| **LOW_TRUST** | No hardware keystore | Software only | Logged as LOW_TRUST. |

## Camera Capability Level
| Feature | Check via CameraCharacteristics | Fallback if missing |
|---|---|---|
| **OIS** | `LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION` | `ois_available: false` |
| **Intrinsics** | `LENS_INTRINSIC_CALIBRATION` | `null` |
| **Distortion** | `LENS_DISTORTION` | `null` |
| **Readout Timestamp** | `SENSOR_READOUT_TIMESTAMP` | `readout_timestamp_supported: false` |

## IMU Capability
| Sensor | Rate | Action |
|---|---|---|
| **Accelerometer / Gyro** | 200 Hz requested | System limits to HAL max; compute `actual_mean_rate_hz` |
