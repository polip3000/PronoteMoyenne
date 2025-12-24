package fr.algorythmice.pronotemoyenne

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import android.os.Parcelable
import androidx.annotation.RequiresApi
import com.chaquo.python.Python
import com.google.android.gms.location.Priority
import fr.algorythmice.pronotemoyenne.grades.GradesCacheStorage
import fr.algorythmice.pronotemoyenne.homeworks.HomeworksCacheStorage
import fr.algorythmice.pronotemoyenne.infos.InfosCacheStorage
import fr.algorythmice.pronotemoyenne.turboself.LoginTurboSelfStorage
import fr.algorythmice.pronotemoyenne.turboself.TurboSelfCacheStorage
import kotlinx.parcelize.Parcelize
import kotlin.math.*

object Utils {

    /* ------------------ DISTANCE ------------------ */

     private fun distanceKm(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    /* ------------------ ETABLISSEMENTS ------------------ */

    fun etablissementsDansUnRayon(
        etablissements: List<Etablissement>,
        latitude: Double,
        longitude: Double,
        rayonKm: Double = 5.0
    ): List<Etablissement> =
        etablissements.filter {
            it.latitude?.let { lat ->
                it.longitude?.let { lon ->
                    distanceKm(latitude, longitude, lat, lon) <= rayonKm
                }
            } ?: false
        }

    fun parseEtablissements(json: String): List<Etablissement> {
        val type = object : TypeToken<List<Etablissement>>() {}.type
        return Gson().fromJson(json, type)
    }

    fun loadJsonFromAssets(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    /* ------------------ LOCALISATION ------------------ */

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fine || coarse
    }


    @SuppressLint("MissingPermission")
    fun getLastLocation(
        context: Context,
        onSuccess: (lat: Double, lon: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onError("Permission localisation non accordée")
            return
        }

        val client = LocationServices.getFusedLocationProviderClient(context)

        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    // Fallback fiable
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        null
                    ).addOnSuccessListener { loc ->
                        if (loc != null) {
                            onSuccess(loc.latitude, loc.longitude)
                        } else {
                            onError("Localisation indisponible")
                        }
                    }
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Erreur localisation")
            }
    }


    /* ------------------ DETECTION IDENTIFIANTS ------------------ */

    fun isLoginComplete(
        user: String?,
        pass: String?,
        ent: String?,
        urlPronote: String?
    ): Boolean {
        return !user.isNullOrBlank()
                && !pass.isNullOrBlank()
                && !ent.isNullOrBlank()
                && !urlPronote.isNullOrBlank()
    }

    fun isLoginCompleteTurboSelf(
        user: String?,
        pass: String?,
    ): Boolean {
        return !user.isNullOrBlank()
                && !pass.isNullOrBlank()
    }

    /* ------------------ CALL API ------------------ */

    data class NotesResult(
        val notes: Map<String, List<Pair<Double, Double>>>,
        val homework: Map<String, Map<String, List<String>>> = emptyMap(),
        val error: String? = null
    )

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchAndParseNotes(context: Context): NotesResult {

        val user = LoginStorage.getUser(context)
        val pass = LoginStorage.getPass(context)
        val ent = LoginStorage.getEnt(context)
        val pronoteUrl = LoginStorage.getUrlPronote(context)

        if (!isLoginComplete(user, pass, ent, pronoteUrl)) {
            return NotesResult(emptyMap(), error = "Identifiants incomplets")
        }

        return try {
            val py = Python.getInstance()
            val module = py.getModule("pronote_fetch")

            val result = module.callAttr(
                "get_notes",
                pronoteUrl,
                user,
                pass,
                ent
            )

            val resultList = result.asList()

            val rawGrades = resultList[0].toString()
            val className = resultList[1].toString()
            val establishment = resultList[2].toString()
            val studentName = resultList[3].toString()
            val rawHomeworks = resultList[4].toString()

            val parsedNotes = parseAndComputeNotes(rawGrades)
            val parsedHomeworks = parseHomeworks(rawHomeworks)

            GradesCacheStorage.saveNotes(context, parsedNotes)
            HomeworksCacheStorage.saveHomeworks(context, rawHomeworks)
            InfosCacheStorage.save(context, className, establishment, studentName)

            NotesResult(
                notes = parsedNotes,
                homework = parsedHomeworks
            )

        } catch (e: Exception) {
            NotesResult(emptyMap(), error = e.toString())
        }
    }

    data class FetchQRcodeResult(
        val qrcode: String = "",
        val error: String? = null
    )


    fun fetchQRcode(context: Context):FetchQRcodeResult {
        val user = LoginTurboSelfStorage.getUser(context)
        val pass = LoginTurboSelfStorage.getPass(context)

        if (!isLoginCompleteTurboSelf(user, pass)) {
            return FetchQRcodeResult(error = "Identifiants incomplets")
        }

        return try {
            val py = Python.getInstance()
            val module = py.getModule("turboself_fetch")




            val result = module.callAttr(
                "get_qr_code",
                user,
                pass,
                context.filesDir.absolutePath
            )

            TurboSelfCacheStorage.save(context, result.toString())

            FetchQRcodeResult(qrcode = result.toString())


        } catch (e: Exception) {
            FetchQRcodeResult(error = e.toString())
        }

    }

    /* ------------------ PARSE DATA ------------------ */

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


    private fun parseAndComputeNotes(raw: String): Map<String, List<Pair<Double, Double>>> {
        val result = mutableMapOf<String, MutableList<Pair<Double, Double>>>()
        val lines = raw.lines()
        var currentSubject = ""
        var notes = mutableListOf<Pair<Double, Double>>()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("Matière :")) {
                if (notes.isNotEmpty()) {
                    result[currentSubject] = notes
                    notes = mutableListOf()
                }
                currentSubject = trimmed.removePrefix("Matière :").trim()

            } else if (trimmed.isNotEmpty() && !trimmed.contains("abs", true)) {

                val match =
                    Regex("""([\d.,]+)/(\d+)\s*\(coef:\s*([\d.,]+)\)""")
                        .find(trimmed)

                if (match != null) {
                    val (noteStr, surStr, coefStr) = match.destructured
                    val note = noteStr.replace(",", ".").toDouble()
                    val sur = surStr.toDouble()
                    val coef = coefStr.replace(",", ".").toDouble()

                    val note20 = if (sur != 20.0) note * 20 / sur else note
                    val coefFinal = if (sur != 20.0) coef * sur / 20 else coef

                    notes.add(note20 to coefFinal)
                }
            }
        }

        if (notes.isNotEmpty()) {
            result[currentSubject] = notes
        }

        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateFr(dateStr: String): String {
        return try {
            val inputFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val outputFormatter = java.time.format.DateTimeFormatter.ofPattern(
                "d MMMM yyyy",
                java.util.Locale.FRENCH
            )

            val date = java.time.LocalDate.parse(dateStr, inputFormatter)
            date.format(outputFormatter)
        } catch (_: Exception) {
            dateStr
        }
    }

    /* ------------------ COMPUTE GENERAL AVERAGE ------------------ */

    fun computeGeneralAverage(parsed: Map<String, List<Pair<Double, Double>>>): Double {
        return parsed.map { (_, notes) ->
            notes.sumOf { it.first * it.second } / notes.sumOf { it.second }
        }.average()
    }
}

/* ------------------ DATA ------------------ */
@Parcelize
data class Etablissement(
    @SerializedName("appellation_officielle")
    val appellationOfficielle: String,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("url_pronote")
    val urlPronote: String
): Parcelable
