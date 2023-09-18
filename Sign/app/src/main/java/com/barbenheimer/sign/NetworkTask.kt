package com.barbenheimer.sign
import android.os.AsyncTask
import com.google.mlkit.vision.pose.Pose
import okhttp3.OkHttpClient
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
class NetworkTask (private val listener: NetworkTaskListener) :
    AsyncTask<Pair<String, String>, Void, String>() {
    override fun doInBackground(vararg params: Pair<String, String>?): String {

        val client = OkHttpClient()
        val json = params[0]?.second
        val request = Request.Builder()
            .url(params[0]?.first) // URL passed as a parameter
            .post(RequestBody.create(MediaType.parse("application/json"), json))
            .build()

        try {
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                return response.body()?.string() ?: ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }
    override fun onPostExecute(result: String) {
        super.onPostExecute(result)
        listener.onNetworkTaskComplete(result)
    }
    interface NetworkTaskListener {
        fun onNetworkTaskComplete(result: String)
    }
}