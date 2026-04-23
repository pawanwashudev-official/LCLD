package com.neubofy.lcld.net

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.neubofy.lcld.utils.CellParameters
import com.neubofy.lcld.utils.PatchedVolley
import com.neubofy.lcld.utils.SingletonHolder
import com.neubofy.lcld.utils.log


// See the API docs: https://wiki.opencellid.org/wiki/API
class OpenCelliDRepository private constructor(private val spec: OpenCelliDSpec) {

    companion object :
        SingletonHolder<OpenCelliDRepository, OpenCelliDSpec>(::OpenCelliDRepository) {
        val TAG = OpenCelliDRepository::class.simpleName
    }

    private val context = spec.context
    private val requestQueue: RequestQueue = PatchedVolley.newRequestQueue(spec.context)

    fun getCellLocation(
        paras: CellParameters,
        apiAccessToken: String,
        onSuccess: (OpenCelliDSuccess) -> Unit,
        onError: (OpenCelliDError) -> Unit,
    ) {
        val url =
            "https://opencellid.org/cell/get?key=$apiAccessToken&mcc=${paras.mobileCountryCode}&mnc=${paras.mobileNetworkCode}&lac=${paras.locationAreaCode}&cellid=${paras.cellId}&radio=${paras.radio.toString().uppercase()}&format=json"

        val request = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                if (response.has("lat") && response.has("lon")) {
                    val lat = response.getString("lat")
                    val lon = response.getString("lon")
                    try {
                        onSuccess(OpenCelliDSuccess(lat.toDouble(), lon.toDouble(), url))
                    } catch (e: NumberFormatException) {
                        val msg = "Failed to format number as double"
                        context.log().w(TAG, msg)
                        onError(OpenCelliDError(msg, url))
                    }
                } else {
                    val message = if (response.has("error")) {
                        response.getString("error")
                    } else "Missing lat or lon in response"

                    context.log()
                        .w(TAG, "OpenCelliD API call failed: $message\n${paras.prettyPrint()}")
                    onError(OpenCelliDError(message, url))
                }
            },
            { error ->
                context.log().w(TAG, "Request failed: ${error.message}")
                onError(
                    OpenCelliDError(error.message ?: "", url)
                )
            },
        )
        requestQueue.add(request)
    }
}

class OpenCelliDSpec(val context: Context)

data class OpenCelliDSuccess(val lat: Double, val lon: Double, val url: String)
data class OpenCelliDError(val error: String, val url: String)
