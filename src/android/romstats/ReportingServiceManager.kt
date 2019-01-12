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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log

class ReportingServiceManager : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(Const.TAG, "[onReceive] BOOT_COMPLETED")

            Utilities.checkIconVisibility(context)
            if (Utilities.persistentOptOut(context)) {
                return
            }

            setAlarm(context, 0)
        } else {
            Log.d(Const.TAG, "[onReceive] CONNECTIVITY_CHANGE")
            launchService(context)
        }
    }

    companion object {

        private const val MILLIS_PER_HOUR = 60L * 60L * 1000L
        private const val MILLIS_PER_DAY = 24L * MILLIS_PER_HOUR

        // UPDATE_INTERVAL days is set in the build.prop file
        // private static final long UPDATE_INTERVAL = 1L * MILLIS_PER_DAY;

        fun setAlarm(context: Context, millisFromNow: Long) {
            var millis = millisFromNow
            val prefs = AnonymousStats.getPreferences(context)

            //prefs.edit().putBoolean(AnonymousStats.ANONYMOUS_ALARM_SET, false).apply();
            //boolean firstBoot = prefs.getBoolean(AnonymousStats.ANONYMOUS_FIRST_BOOT, true);

            // get ANONYMOUS_OPT_IN pref, defaults to true (new behavior)
            val optedIn = prefs.getBoolean(Const.ANONYMOUS_OPT_IN, true)

            if (!optedIn) {
                return
            }

            val updateInterval = Utilities.timeFrame * MILLIS_PER_DAY

            if (millis <= 0) {
                var lastSynced = prefs.getLong(Const.ANONYMOUS_LAST_CHECKED, 0)
                if (lastSynced == 0L) {
                    // never synced, so let's fake out that the last sync was just now.
                    // this will allow the user tFrame time to opt out before it will start
                    // sending up anonymous stats.
                    lastSynced = System.currentTimeMillis()
                    prefs.edit().putLong(Const.ANONYMOUS_LAST_CHECKED, lastSynced).apply()
                    Log.d(Const.TAG, "[setAlarm] Set alarm for first sync.")
                }
                millis = lastSynced + updateInterval - System.currentTimeMillis()
            }

            val intent = Intent(ConnectivityManager.CONNECTIVITY_ACTION)
            intent.setClass(context, ReportingServiceManager::class.java)

            val nextAlarm = System.currentTimeMillis() + millis

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarm, PendingIntent.getBroadcast(context, 0, intent, 0))
            Log.d(Const.TAG, "[setAlarm] Next sync attempt in : " + millis / MILLIS_PER_HOUR + " hours")

            prefs.edit().putLong(Const.ANONYMOUS_NEXT_ALARM, nextAlarm).apply()
        }

        fun launchService(context: Context, force: Boolean = false) {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val networkInfo = cm.activeNetworkInfo
            if (networkInfo == null) {
                Log.d(Const.TAG, "[launchService] Can't get network info")
                return
            }

            Log.d(Const.TAG, "[launchService] networkInfo: $networkInfo")
            if (!networkInfo.isConnected) {
                Log.d(Const.TAG, "[launchService] User is not connected")
                return
            }

            val prefs = AnonymousStats.getPreferences(context)

            val optedIn = prefs.getBoolean(Const.ANONYMOUS_OPT_IN, true)
            if (!optedIn) {
                Log.d(Const.TAG, "[launchService] User has not opted in")
                return
            }

            val firstBoot = prefs.getBoolean(Const.ANONYMOUS_FIRST_BOOT, true)
            if (firstBoot) {
                Log.d(Const.TAG, "[launchService] MODE=1 & firstBoot -> prompt user")
                // promptUser is called through a service because it cannot be called from a BroadcastReceiver
                val intent = Intent()
                intent.setClass(context, ReportingService::class.java)
                intent.putExtra("promptUser", true)
                context.startService(intent)
                return
            }

            var lastSynced = prefs.getLong(Const.ANONYMOUS_LAST_CHECKED, 0)
            if (lastSynced == 0L) {
                setAlarm(context, 0)
                return
            }

            val lastReportedVersion = prefs.getString(Const.ANONYMOUS_LAST_REPORT_VERSION, null)
            if (Utilities.romVersionHash != lastReportedVersion) {
                // if rom version has changed since last reporting, do an immediate reporting
                lastSynced = 1
            }

            val updateInterval = Utilities.timeFrame * MILLIS_PER_DAY

            val timeLeft = System.currentTimeMillis() - lastSynced
            if (timeLeft < updateInterval && !force) {
                Log.d(Const.TAG, "Waiting for next sync : " + timeLeft / MILLIS_PER_HOUR + " hours")
                return
            }

            val intent = Intent()
            intent.setClass(context, ReportingService::class.java)
            context.startService(intent)
        }
    }
}
