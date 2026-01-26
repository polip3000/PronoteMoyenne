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
import com.google.android.gms.location.Priority
import kotlinx.parcelize.Parcelize
import kotlin.math.*
import fr.algorythmice.pronotemoyenne.pronote.PronoteUtils.NoteEntry

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

    fun getEstablishmentsWithinRadius(
        etablissements: List<Establishment>,
        latitude: Double,
        longitude: Double,
        rayonKm: Double = 5.0
    ): List<Establishment> =
        etablissements.filter {
            it.latitude?.let { lat ->
                it.longitude?.let { lon ->
                    distanceKm(latitude, longitude, lat, lon) <= rayonKm
                }
            } ?: false
        }

    fun parseEstablishments(json: String): List<Establishment> {
        val type = object : TypeToken<List<Establishment>>() {}.type
        return Gson().fromJson(json, type)
    }

    /* ------------------ ASSETS / FILES ------------------ */

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

    fun isTurboSelfLoginComplete(
        user: String?,
        pass: String?,
    ): Boolean {
        return !user.isNullOrBlank()
                && !pass.isNullOrBlank()
    }

    /* ------------------ DATE / FORMAT ------------------ */

    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDateFrench(dateStr: String): String {
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
}

/* ------------------ DATA ------------------ */
@Parcelize
data class Establishment(
    @SerializedName("appellation_officielle")
    val officialName: String,

    @SerializedName("latitude")
    val latitude: Double? = null,

    @SerializedName("longitude")
    val longitude: Double? = null,

    @SerializedName("url_pronote")
    val pronoteUrl: String
): Parcelable
