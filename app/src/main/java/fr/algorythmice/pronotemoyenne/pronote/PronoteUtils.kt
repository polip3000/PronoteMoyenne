@file:Suppress("UNCHECKED_CAST")

package fr.algorythmice.pronotemoyenne.pronote

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import fr.algorythmice.pronotekt.Client
import fr.algorythmice.pronotekt.EntFunction
import fr.algorythmice.pronotekt.ent.*
import fr.algorythmice.pronotemoyenne.LoginStorage
import fr.algorythmice.pronotemoyenne.Utils.isLoginComplete
import fr.algorythmice.pronotemoyenne.grades.AveragesCacheStorageCacheStorage
import fr.algorythmice.pronotemoyenne.grades.GradesCacheStorage
import fr.algorythmice.pronotemoyenne.homeworks.HomeworksCacheStorage
import fr.algorythmice.pronotemoyenne.infos.InfosCacheStorage
import java.time.LocalDate
import java.util.Locale
import kotlin.reflect.full.memberProperties

object PronoteUtils {

    /* ------------------ CALL API ------------------ */
    data class NotesResult(
        val notes: Map<String, List<NoteEntry>>,
        val homework: Map<String, Map<String, List<String>>> = emptyMap(),
        val average: Map<String, List<Pair<Double, Double>>> = emptyMap(),
        val error: String? = null
    )

    data class NoteEntry(val note: Double, val outOf: Double, val coef: Double)

    @RequiresApi(Build.VERSION_CODES.O)
    @Suppress("unused")
    fun syncPronoteData(context: Context): NotesResult {

        val user = LoginStorage.getUser(context)
        val pass = LoginStorage.getPass(context)
        val ent = LoginStorage.getEnt(context)
        val pronoteUrl = LoginStorage.getUrlPronote(context)

        if (!isLoginComplete(user, pass, ent, pronoteUrl)) {
            return NotesResult(notes = emptyMap(), homework = emptyMap(), average = emptyMap(), error = "Identifiants incomplets")
        }

        return try {
            val entFunc = getEntFromString(ent)

            val client = Client(
                pronoteUrl.toString(),
                username = user.toString(),
                password = pass.toString(),
                ent = entFunc
            )

            val period = client.currentPeriod

            val gradeText = buildString {
                period.grades.groupBy { it.subject.name }.forEach { (subject, grades) ->
                    append("\nMatière : $subject\n")
                    grades.forEach { g ->
                        append("${g.grade}/${g.out_of}  (coef: ${g.coefficient})\n")
                    }
                }
            }

            val averageText = buildString {
                period.averages.groupBy { it.subject.name }.forEach { (subject, average) ->
                    append("\nMatière : $subject\n")
                    average.forEach { g ->
                        append("${g.student}/${g.out_of}")
                    }
                }
            }

            val homeworkText = buildString {
                client.homework(LocalDate.now()).forEach { hw ->
                    append("\nDate : ${hw.date}\n")
                    append("Matière : ${hw.subject.name}\n")
                    append(hw.description.trim()).append('\n')
                }
            }

            val parsedNotes = parseNotes(gradeText)
            val parsedHomeworks = parseHomeworks(homeworkText)
            val parsedAverages = parseAverages(averageText)

            GradesCacheStorage.saveNotes(context, parsedNotes)
            AveragesCacheStorageCacheStorage.saveAverages(context, parsedAverages)
            HomeworksCacheStorage.saveHomeworks(context, homeworkText)
            InfosCacheStorage.save(context, client.info.class_name, client.info.establishment, client.info.name)

            NotesResult(notes = parsedNotes, homework = parsedHomeworks, average = parsedAverages)
        } catch (e: Exception) {
            NotesResult(notes = emptyMap(), homework = emptyMap(), average = emptyMap(), error = e.toString())
        }
    }

    private fun getEntFromString(entName: String?): EntFunction? {
        if (entName.isNullOrBlank() || entName.equals("no_ent", ignoreCase = true)) return null
        val normalized = entName.lowercase(Locale.ROOT)
        val candidates = listOf(normalized, "ent_${normalized}")

        fun findInEntKt(): EntFunction? {
            return runCatching {
                val entKt = Class.forName("fr.algorythmice.pronotekt.ent.EntKt")
                entKt.declaredFields.firstOrNull { field ->
                    val name = field.name.lowercase(Locale.ROOT)
                    name in candidates && Function3::class.java.isAssignableFrom(field.type)
                }?.let { field ->
                    field.isAccessible = true
                    field.get(null) as? EntFunction
                }
            }.getOrNull()
        }

        fun findInEntObject(): EntFunction? {
            return runCatching {
                Ent::class.memberProperties
                    .firstOrNull { prop -> prop.name.lowercase(Locale.ROOT) in candidates }
                    ?.getter
                    ?.call(Ent) as? EntFunction
            }.getOrNull()
        }

        val entFunc = findInEntKt() ?: findInEntObject()
        if (entFunc == null) Log.e("PronoteUtils", "ENT '$entName' inconnu, fallback null")
        return entFunc
    }

    /* ------------------ PARSE DATA ------------------ */
    private fun parseNotes(raw: String): Map<String, List<NoteEntry>> {
        val result = mutableMapOf<String, MutableList<NoteEntry>>()
        val lines = raw.lines()
        var currentSubject = ""
        var notes = mutableListOf<NoteEntry>()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("Matière :")) {
                if (notes.isNotEmpty()) {
                    result[currentSubject] = notes
                    notes = mutableListOf()
                }
                currentSubject = trimmed.removePrefix("Matière :").trim()

            } else if (trimmed.isNotEmpty()
                && !trimmed.contains("abs", true)) {

                val match =
                    Regex("""([\d.,]+)/([\d]+)\s*\(coef:\s*([\d.,]+)\)""")
                        .find(trimmed)

                if (match != null) {
                    val (noteStr, outOfStr, coefStr) = match.destructured
                    val note = noteStr.replace(",", ".").toDouble()
                    val outOf = outOfStr.toDouble()
                    val coef = coefStr.replace(",", ".").toDouble()

                    notes.add(NoteEntry(note = note, outOf = outOf, coef = coef))
                }
            }
        }

        if (notes.isNotEmpty()) {
            result[currentSubject] = notes
        }

        return result
    }

    private fun parseAverages(raw: String): Map<String, List<Pair<Double, Double>>> {
        val result = mutableMapOf<String, MutableList<Pair<Double, Double>>>()
        val lines = raw.lines()
        var currentSubject = ""
        var averages = mutableListOf<Pair<Double, Double>>()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("Matière :")) {
                if (averages.isNotEmpty()) {
                    result[currentSubject] = averages
                    averages = mutableListOf()
                }
                currentSubject = trimmed.removePrefix("Matière :").trim()
            } else if (trimmed.isNotEmpty() && !trimmed.contains("abs", true)) {
                val match = Regex("""([\d.,]+)/([\d]+)""").find(trimmed)

                if (match != null) {
                    val (avgStr, surStr) = match.destructured
                    val avg = avgStr.replace(",", ".").toDouble()
                    val sur = surStr.toDouble()

                    val avg20 = if (sur != 20.0) avg * 20 / sur else avg
                    averages.add(avg20 to 1.0)
                }
            }
        }

        if (averages.isNotEmpty()) {
            result[currentSubject] = averages
        }

        return result
    }

    fun parseHomeworks(
        raw: String
    ): Map<String, Map<String, List<String>>> {

        val result = mutableMapOf<String, MutableMap<String, MutableList<String>>>()

        var currentDate = ""
        var currentSubject = ""

        raw.lines().forEach { line ->
            val trimmed = line.trim()

            when {
                trimmed.startsWith("Date :") -> {
                    currentDate = trimmed.removePrefix("Date :").trim()
                    result.putIfAbsent(currentDate, mutableMapOf())
                }

                trimmed.startsWith("Matière :") -> {
                    currentSubject = trimmed.removePrefix("Matière :").trim()
                    result[currentDate]?.putIfAbsent(currentSubject, mutableListOf())
                }

                trimmed.isNotEmpty() -> {
                    result[currentDate]?.get(currentSubject)?.add(trimmed)
                }
            }
        }

        return result
    }

}