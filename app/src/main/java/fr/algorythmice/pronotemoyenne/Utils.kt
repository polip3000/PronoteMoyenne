package fr.algorythmice.pronotemoyenne

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import android.os.Parcelable
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
    ): List<Etablissement> {
        return etablissements.filter {
            distanceKm(latitude, longitude, it.latitude, it.longitude) <= rayonKm
        }
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
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getLastLocation(
        context: Context,
        onSuccess: (lat: Double, lon: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

        if (!hasLocationPermission(context)) {
            onError("Permission localisation non accordée")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location.latitude, location.longitude)
                } else {
                    onError("Localisation indisponible")
                }
            }
            .addOnFailureListener {
                onError(it.message ?: "Erreur localisation")
            }
    }

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

}

/* ------------------ DATA ------------------ */
@Parcelize
data class Etablissement(
    @SerializedName("appellation_officielle")
    val appellationOfficielle: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double,

    @SerializedName("url_pronote")
    val urlPronote: String
): Parcelable
