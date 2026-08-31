package com.example.kamerakita_mobile.sensors

import kotlin.math.sqrt

class SensorRateAnalyzer(private val requestedRateHz: Int) {
    private val timestamps = mutableListOf<Long>()
    
    fun recordEvent(timestampNs: Long) {
        timestamps.add(timestampNs)
    }

    fun analyze(): Map<String, Any> {
        if (timestamps.size < 2) return mapOf("error" to "Not enough samples")
        
        val intervals = mutableListOf<Long>()
        for (i in 1 until timestamps.size) {
            intervals.add(timestamps[i] - timestamps[i-1])
        }
        intervals.sort()
        
        val count = timestamps.size
        val durationNs = timestamps.last() - timestamps.first()
        val actualMeanRateHz = count.toDouble() / (durationNs / 1_000_000_000.0)
        
        val medianIntervalNs = intervals[intervals.size / 2]
        val meanIntervalNs = durationNs / (count - 1)
        
        var variance = 0.0
        for (interval in intervals) {
            val diff = interval - meanIntervalNs
            variance += diff * diff
        }
        val stddevNs = sqrt(variance / intervals.size).toLong()

        return mapOf(
            "requested_rate_hz" to requestedRateHz,
            "actual_mean_rate_hz" to actualMeanRateHz,
            "median_interval_ns" to medianIntervalNs,
            "jitter_stddev_ns" to stddevNs,
            "sample_count" to count
        )
    }
}
