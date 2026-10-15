package com.wordcraft.storage

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** A recently opened document (name + content URI string). */
data class RecentDoc(val name: String, val uri: String, val openedAt: Long)

/**
 * Recent documents, persisted in SharedPreferences.
 * The documents themselves live wherever the user put them (SAF); this only remembers URIs.
 */
class Recents(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("wordcraft.recents", Context.MODE_PRIVATE)

    fun list(): List<RecentDoc> {
        val raw = prefs.getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                RecentDoc(o.optString("name"), o.optString("uri"), o.optLong("at"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Record an open; moves an existing entry for the same URI to the top. */
    fun record(name: String, uri: String) {
        val rest = list().filterNot { it.uri == uri }
        val all = (listOf(RecentDoc(name, uri, System.currentTimeMillis())) + rest).take(MAX)
        val arr = JSONArray()
        all.forEach { d ->
            arr.put(JSONObject().put("name", d.name).put("uri", d.uri).put("at", d.openedAt))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun remove(uri: String) {
        val all = list().filterNot { it.uri == uri }
        val arr = JSONArray()
        all.forEach { d ->
            arr.put(JSONObject().put("name", d.name).put("uri", d.uri).put("at", d.openedAt))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "recent_documents"
        private const val MAX = 20
    }
}
