/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.romstats

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.SocketTimeoutException
import java.net.URL
import java.util.HashMap
import javax.net.ssl.HttpsURLConnection

class ReportingService : Service() {

    private var mTask: StatsUploadTask? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        var canReport = true
        if (intent.getBooleanExtra("promptUser", false)) {
            Log.d(Const.TAG, "Prompting user for opt-in.")
            promptUser()
            canReport = false
        }

        val romStatsUrl = Utilities.statsUrl
        if (romStatsUrl == null || romStatsUrl.isEmpty()) {
            Log.e(Const.TAG, "This ROM is not configured for ROM Statistics.")
            canReport = false
        }

        if (canReport) {
            Log.d(Const.TAG, "User has opted in -- reporting.")

            if (mTask == null || mTask!!.status == AsyncTask.Status.FINISHED) {
                mTask = StatsUploadTask(this)
                mTask!!.execute()
            }
        }

        return Service.START_REDELIVER_INTENT
    }

    private fun promptUser() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mainActivity = Intent(applicationContext, AnonymousStats::class.java)
        val pendingIntent = PendingIntent.getActivity(applicationContext, 0, mainActivity, 0)
        val notification = NotificationCompat.Builder(baseContext, "romstats")
            .setSmallIcon(R.drawable.ic_launcher)
            .setTicker(getString(R.string.notification_ticker))
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_desc))
            .setWhen(System.currentTimeMillis())
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(Utilities.NOTIFICATION_ID, notification)
    }

    companion object {
        private class StatsUploadTask(service: ReportingService) : AsyncTask<Void, Void, Boolean>() {
            var weakRef: WeakReference<ReportingService> = WeakReference(service)

            override fun doInBackground(vararg params: Void): Boolean? {
                val applicationContext = weakRef.get()!!.applicationContext
                val deviceId = Utilities.getUniqueID(applicationContext)
                val deviceName = Utilities.device
                val deviceVersion = Utilities.modVersion
                val deviceCountry = Utilities.getCountryCode(applicationContext)
                val deviceCarrier = Utilities.getCarrier(applicationContext)
                val deviceCarrierId = Utilities.getCarrierId(applicationContext)
                val romName = Utilities.romName
                val romVersion = Utilities.romVersion

                val romStatsUrl = Utilities.statsUrl

                Log.d(Const.TAG, "SERVICE: Report URL=$romStatsUrl")
                Log.d(Const.TAG, "SERVICE: Device ID=$deviceId")
                Log.d(Const.TAG, "SERVICE: Device Name=$deviceName")
                Log.d(Const.TAG, "SERVICE: Device Version=$deviceVersion")
                Log.d(Const.TAG, "SERVICE: Country=$deviceCountry")
                Log.d(Const.TAG, "SERVICE: Carrier=$deviceCarrier")
                Log.d(Const.TAG, "SERVICE: Carrier ID=$deviceCarrierId")
                Log.d(Const.TAG, "SERVICE: ROM Name=$romName")
                Log.d(Const.TAG, "SERVICE: ROM Version=$romVersion")

                // report to the rrstats service
                val headers = HashMap<String, String?>()
                headers["deviceHash"] = deviceId
                headers["deviceName"] = deviceName
                headers["deviceVersion"] = deviceVersion
                headers["deviceCountry"] = deviceCountry
                headers["deviceCarrier"] = deviceCarrier
                headers["deviceCarrierId"] = deviceCarrierId
                headers["romName"] = romName
                headers["romVersion"] = romVersion
                var success = false
                try {
                    val url = URL(romStatsUrl + "submit.php")
                    val client = url.openConnection() as HttpsURLConnection
                    client.requestMethod = "POST"
                    headers.forEach { key, value -> client.setRequestProperty(key, value ?: "") }
                    client.doOutput = true
                    val responseCode = client.responseCode
                    if (responseCode == HttpsURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(client.inputStream))
                        val response = reader.emitAllAndClose()
                        Log.d(Const.TAG, "server output: $response")
                        success = true
                    } else {
                        val reader = BufferedReader(InputStreamReader(client.errorStream))
                        val response = reader.emitAllAndClose()
                        Log.d(Const.TAG, "server error: $response")
                    }
                } catch (e: SocketTimeoutException) {
                    Log.d(Const.TAG, "Timed out connecting to server", e)
                } catch (e: IOException) {
                    Log.w(Const.TAG, "Could not upload stats checkin", e)
                }
                return success
            }

            override fun onPostExecute(result: Boolean?) {
                val interval: Long
                interval = if (result!!) {
                    val prefs = AnonymousStats.getPreferences(weakRef.get()!!.applicationContext)

                    // save the current date for future checkins
                    prefs.edit().putLong(Const.ANONYMOUS_LAST_CHECKED, System.currentTimeMillis()).apply()

                    // save a hashed rom version (used to to an immediate checkin in case of new rom version
                    prefs.edit().putString(Const.ANONYMOUS_LAST_REPORT_VERSION, Utilities.romVersionHash).apply()

                    // set interval = 0; this causes setAlarm to schedule next report after UPDATE_INTERVAL
                    0
                } else {
                    // error, try again in 3 hours
                    3L * 60L * 60L * 1000L
                }

                ReportingServiceManager.setAlarm(weakRef.get()!!.applicationContext, interval)
                weakRef.get()?.stopSelf()
            }
        }
    }
}
