package com.kmhmubin.kothagolp.source

import org.json.JSONObject

data class SourceManifest(
    val version: Int,
    val url: String,
    val sources: List<SourceEntry>
) {
    companion object {
        fun fromJson(json: String): SourceManifest {
            val obj = JSONObject(json)
            val arr = obj.getJSONArray("sources")
            val sources = (0 until arr.length()).map { i ->
                val s = arr.getJSONObject(i)
                SourceEntry(
                    id = s.getString("id"),
                    className = s.getString("class")
                )
            }
            // Version may be an integer (10) or semantic string ("9.1.0") —
            // normalise to integer by taking the major component.
            val rawVersion = obj.get("version")
            val version = when (rawVersion) {
                is Int -> rawVersion
                is Long -> rawVersion.toInt()
                else -> rawVersion.toString().substringBefore(".").toIntOrNull() ?: 0
            }
            return SourceManifest(
                version = version,
                url = obj.getString("url"),
                sources = sources
            )
        }
    }
}

data class SourceEntry(
    val id: String,
    val className: String
)
