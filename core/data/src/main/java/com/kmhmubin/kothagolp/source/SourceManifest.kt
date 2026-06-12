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
            return SourceManifest(
                version = obj.getInt("version"),
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
