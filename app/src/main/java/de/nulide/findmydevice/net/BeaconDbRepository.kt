package de.nulide.findmydevice.net

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import de.nulide.findmydevice.utils.CellParameters
import de.nulide.findmydevice.utils.PatchedVolley
import de.nulide.findmydevice.utils.SingletonHolder
import de.nulide.findmydevice.utils.log
import de.nulide.findmydevice.utils.prettyPrint
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject


class BeaconDbRepository private constructor(private val context: Context) {

    companion object :
        SingletonHolder<BeaconDbRepository, Context>(::BeaconDbRepository) {
        val TAG = BeaconDbRepository::class.simpleName
    }

    private val requestQueue: RequestQueue = PatchedVolley.newRequestQueue(context)

    fun getCellLocation(
        paras: List<CellParameters>,
        onSuccess: (BeaconDbSuccess) -> Unit,
        onError: (BeaconDbError) -> Unit,
    ) {
        // https://ichnaea.readthedocs.io/en/latest/api/geolocate.html
        val url = "https://api.beacondb.net/v1/geolocate"

        val cellTowers: List<Map<String, Any>> = paras.mapNotNull {
            val mcc = it.mobileCountryCode?.toInt() ?: return@mapNotNull null
            val mnc = it.mobileNetworkCode?.toInt() ?: return@mapNotNull null
            val lac = it.locationAreaCode ?: return@mapNotNull null
            val cid = it.cellId ?: return@mapNotNull null

            mapOf<String, Any>(
                Pair("radioType", it.radio.toString().lowercase()),
                Pair("mobileCountryCode", mcc),
                Pair("mobileNetworkCode", mnc),
                Pair("locationAreaCode", lac),
                Pair("cellId", cid),
            )
        }.toList()

        // Fallbacks can be very imprecise. Also, we want the cell location and nothing else.
        val fallbacks = mapOf<String, Boolean>(
            Pair("lacf", false),
            Pair("ipf", false),
        )
        val jsonObject = JSONObject().apply {
            put("considerIp", false)
            put("fallbacks", JSONObject(fallbacks))
            put("cellTowers", JSONArray(cellTowers))
        }

        val request = JsonObjectRequest(
            Request.Method.POST,
            url,
            jsonObject,
            { response ->
                // Filter fallback, just to be sure
                if (response.has("location") && !response.has("fallback")) {
                    val locObj = response.getJSONObject("location")
                    if (locObj.has("lat") && locObj.has("lng")) {
                        val lat = locObj.getDouble("lat")
                        val lon = locObj.getDouble("lng")

                        var accuracy: Double? = null
                        if (response.has("accuracy")) {
                            accuracy = response.getDouble("accuracy")
                        }

                        onSuccess(BeaconDbSuccess(lat, lon, accuracy, url))
                        return@JsonObjectRequest
                    }
                }

                val message = getErrorMessage(response)
                context.log().w(TAG, "BeaconDB API call failed: $message\n${paras.prettyPrint()}")
                onError(BeaconDbError(message, url))
            },
            { error ->
                val message = try {
                    val body = String(error.networkResponse.data, Charsets.UTF_8)
                    val json = JSONObject(body)
                    getErrorMessage(json)
                } catch (e: JSONException) {
                    error.message ?: ""
                } catch (e: NullPointerException) {
                    error.message ?: ""
                }
                context.log().w(TAG, "BeaconDB API call failed: $message\n${paras.prettyPrint()}")
                onError(BeaconDbError(message, url))
            },
        )
        requestQueue.add(request)
    }
}

private fun getErrorMessage(response: JSONObject): String {
    if (response.has("error")) {
        val errObj = response.getJSONObject("error")
        if (errObj.has("message")) {
            return errObj.getString("message")
        }
    }
    return "Missing lat or lon in response"
}

data class BeaconDbSuccess(
    val lat: Double,
    val lon: Double,
    /** Accuracy in meter */
    val accuracy: Double?,
    val url: String,
)

data class BeaconDbError(val error: String, val url: String)
