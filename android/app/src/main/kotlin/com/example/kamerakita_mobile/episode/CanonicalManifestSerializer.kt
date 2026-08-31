package com.example.kamerakita_mobile.episode

import org.json.JSONArray
import org.json.JSONObject
import java.util.TreeMap

object CanonicalManifestSerializer {
    // Basic implementation of deterministic JSON serialization
    fun serialize(map: Map<String, Any?>): String {
        return buildCanonicalString(map)
    }

    private fun buildCanonicalString(obj: Any?): String {
        return when (obj) {
            is Map<*, *> -> {
                val sortedMap = TreeMap<String, Any?>()
                for ((k, v) in obj) {
                    sortedMap[k.toString()] = v
                }
                val sb = StringBuilder()
                sb.append("{")
                var first = true
                for ((k, v) in sortedMap) {
                    if (!first) sb.append(",")
                    sb.append("\"").append(k).append("\":").append(buildCanonicalString(v))
                    first = false
                }
                sb.append("}")
                sb.toString()
            }
            is List<*> -> {
                val sb = StringBuilder()
                sb.append("[")
                var first = true
                for (v in obj) {
                    if (!first) sb.append(",")
                    sb.append(buildCanonicalString(v))
                    first = false
                }
                sb.append("]")
                sb.toString()
            }
            is String -> JSONObject.quote(obj)
            is Number -> {
                // Ensure deterministic float/double printing without trailing zeros if whole
                val d = obj.toDouble()
                if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
            }
            is Boolean -> obj.toString()
            null -> "null"
            else -> JSONObject.quote(obj.toString())
        }
    }
}
