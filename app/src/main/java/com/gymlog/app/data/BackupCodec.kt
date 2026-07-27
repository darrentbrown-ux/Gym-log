package com.gymlog.app.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a (de)serialised JSON dump of the entire user database for backup/restore
 * and a CSV of every logged set for spreadsheet export.
 *
 * The format is intentionally simple (plain types + enum names) so users can inspect
 * the file outside the app.
 */
object BackupCodec {

    data class Dump(
        val exercises: List<Exercise>,
        val settingDefs: List<MachineSettingDef>,
        val presets: List<Preset>,
        val presetExercises: List<PresetExercise>,
        val sessions: List<Session>,
        val sessionExercises: List<SessionExercise>,
        val sessionSets: List<SessionSet>
    )

    fun toJson(dump: Dump): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("exercises", JSONArray().also { a -> dump.exercises.forEach { e ->
            a.put(JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("category", e.category.name)
                put("notes", e.notes)
            })
        }})
        root.put("settingDefs", JSONArray().also { a -> dump.settingDefs.forEach { d ->
            a.put(JSONObject().apply {
                put("id", d.id)
                put("exerciseId", d.exerciseId)
                put("name", d.name)
            })
        }})
        root.put("presets", JSONArray().also { a -> dump.presets.forEach { p ->
            a.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
            })
        }})
        root.put("presetExercises", JSONArray().also { a -> dump.presetExercises.forEach { pe ->
            a.put(JSONObject().apply {
                put("id", pe.id)
                put("presetId", pe.presetId)
                put("exerciseId", pe.exerciseId)
                put("defaultWeight", pe.defaultWeight ?: JSONObject.NULL)
                put("defaultReps", pe.defaultReps ?: JSONObject.NULL)
                put("defaultSets", pe.defaultSets)
                put("position", pe.position)
            })
        }})
        root.put("sessions", JSONArray().also { a -> dump.sessions.forEach { s ->
            a.put(JSONObject().apply {
                put("id", s.id)
                put("date", s.date)
                put("name", s.name)
                put("presetId", s.presetId ?: JSONObject.NULL)
            })
        }})
        root.put("sessionExercises", JSONArray().also { a -> dump.sessionExercises.forEach { se ->
            a.put(JSONObject().apply {
                put("id", se.id)
                put("sessionId", se.sessionId)
                put("exerciseId", se.exerciseId)
                put("position", se.position)
                put("notes", se.notes)
            })
        }})
        root.put("sessionSets", JSONArray().also { a -> dump.sessionSets.forEach { ss ->
            a.put(JSONObject().apply {
                put("id", ss.id)
                put("sessionExerciseId", ss.sessionExerciseId)
                put("setNumber", ss.setNumber)
                put("reps", ss.reps ?: JSONObject.NULL)
                put("weight", ss.weight ?: JSONObject.NULL)
                put("settingsValues", ss.settingsValues)
                put("durationSeconds", ss.durationSeconds ?: JSONObject.NULL)
                put("distance", ss.distance ?: JSONObject.NULL)
                put("completed", ss.completed)
            })
        }})

        return root.toString(2)
    }

    /** CSV row for one set with all useful fields flattened. */
    fun toCsv(
        sessions: List<Session>,
        sessionExercises: List<SessionExercise>,
        sessionSets: List<SessionSet>,
        exercises: List<Exercise>
    ): String {
        val exById = exercises.associateBy { it.id }
        val exByRowSE = sessionExercises.associateBy { it.id }
        val sessById = sessions.associateBy { it.id }

        val sb = StringBuilder()
        sb.append("date,session_name,exercise,category,set,reps,weight,duration_sec,distance,settings,notes\n")
        val iso = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        for (set in sessionSets.sortedWith(compareBy({ sessById[exByRowSE[it.sessionExerciseId]?.sessionId]?.date ?: 0 }, { it.sessionExerciseId }, { it.setNumber }))) {
            val se = exByRowSE[set.sessionExerciseId] ?: continue
            val s = sessById[se.sessionId] ?: continue
            val ex = exById[se.exerciseId] ?: continue
            sb.append(iso.format(java.util.Date(s.date))).append(",")
            sb.append(csvField(s.name)).append(",")
            sb.append(csvField(ex.name)).append(",")
            sb.append(csvField(ex.category.label)).append(",")
            sb.append(set.setNumber).append(",")
            sb.append(set.reps?.toString().orEmpty()).append(",")
            sb.append(set.weight?.toString().orEmpty()).append(",")
            sb.append(set.durationSeconds?.toString().orEmpty()).append(",")
            sb.append(set.distance?.toString().orEmpty()).append(",")
            sb.append(csvField(set.settingsValues)).append(",")
            sb.append(csvField(se.notes)).append("\n")
        }
        return sb.toString()
    }

    private fun csvField(s: String): String {
        val needsQuote = s.contains(',') || s.contains('"') || s.contains('\n')
        val escaped = s.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    // ===== JSON → Dump (v1.5.4 import) =====

    /**
     * Parse a backup-JSON string back into a [Dump]. Mirrors [toJson] exactly
     * and is tolerant of missing optional fields (uses empty/default values
     * rather than throwing). Unknown top-level keys are ignored so future
     * versions of the export schema can extend the format without breaking
     * older imports.
     */
    fun fromJson(json: String): Dump {
        val root = org.json.JSONObject(json)
        return Dump(
            exercises = parseArray(root.optJSONArray("exercises")) { o ->
                Exercise(
                    // The original primary key is preserved here so the repository
                    // can build an old-id → new-id mapping. It is explicitly zeroed
                    // at insert time in Repository.importBackup so Room auto-generates
                    // a fresh primary key.
                    id = o.optLong("id", 0L),
                    name = o.getString("name"),
                    category = ExerciseCategory.valueOf(o.getString("category")),
                    notes = o.optString("notes", "")
                )
            },
            settingDefs = parseArray(root.optJSONArray("settingDefs")) { o ->
                MachineSettingDef(
                    id = o.optLong("id", 0L),
                    exerciseId = o.getLong("exerciseId"),
                    name = o.getString("name")
                )
            },
            presets = parseArray(root.optJSONArray("presets")) { o ->
                Preset(id = o.optLong("id", 0L), name = o.getString("name"))
            },
            presetExercises = parseArray(root.optJSONArray("presetExercises")) { o ->
                PresetExercise(
                    id = o.optLong("id", 0L),
                    presetId = o.getLong("presetId"),
                    exerciseId = o.getLong("exerciseId"),
                    defaultWeight = o.optDoubleOrNull("defaultWeight"),
                    defaultReps = o.optIntOrNull("defaultReps"),
                    defaultSets = o.optInt("defaultSets", 3),
                    position = o.optInt("position", 0),
                    notes = o.optString("notes", "")
                )
            },
            sessions = parseArray(root.optJSONArray("sessions")) { o ->
                Session(
                    id = o.optLong("id", 0L),
                    date = o.getLong("date"),
                    name = o.getString("name"),
                    presetId = o.optLongOrNull("presetId")
                )
            },
            sessionExercises = parseArray(root.optJSONArray("sessionExercises")) { o ->
                SessionExercise(
                    id = o.optLong("id", 0L),
                    sessionId = o.getLong("sessionId"),
                    exerciseId = o.getLong("exerciseId"),
                    position = o.optInt("position", 0),
                    notes = o.optString("notes", "")
                )
            },
            sessionSets = parseArray(root.optJSONArray("sessionSets")) { o ->
                SessionSet(
                    id = o.optLong("id", 0L),
                    sessionExerciseId = o.getLong("sessionExerciseId"),
                    setNumber = o.getInt("setNumber"),
                    reps = o.optIntOrNull("reps"),
                    weight = o.optDoubleOrNull("weight"),
                    settingsValues = o.optString("settingsValues", "{}"),
                    durationSeconds = o.optIntOrNull("durationSeconds"),
                    distance = o.optDoubleOrNull("distance"),
                    completed = o.optBoolean("completed", true)
                )
            }
        )
    }

    private inline fun <T> parseArray(arr: org.json.JSONArray?, block: (org.json.JSONObject) -> T): List<T> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { block(arr.getJSONObject(i)) }
                .onFailure { android.util.Log.w("BackupCodec", "Skipping malformed row $i: ${it.message}") }
                .getOrNull()
        }
    }

    private fun org.json.JSONObject.optLongOrNull(name: String): Long? =
        if (isNull(name)) null else optLong(name, 0L).takeIf { has(name) && !isNull(name) }

    private fun org.json.JSONObject.optIntOrNull(name: String): Int? =
        if (isNull(name)) null else optInt(name, 0).takeIf { has(name) && !isNull(name) }

    private fun org.json.JSONObject.optDoubleOrNull(name: String): Double? =
        if (isNull(name)) null else optDouble(name, 0.0).takeIf { has(name) && !isNull(name) }
}
