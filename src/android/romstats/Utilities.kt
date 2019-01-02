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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.SystemProperties
import android.preference.PreferenceManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log

import java.io.File
import java.math.BigInteger
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.Locale

object Utilities {
    const val SETTINGS_PREF_NAME = "ROMStats"
    const val NOTIFICATION_ID = 1

    // if the last char of the link is not /, add it
    val statsUrl: String?
        get() {
            var returnUrl = Const.STATS_URL

            if (returnUrl.isEmpty()) {
                return null
            }
            if (returnUrl.substring(returnUrl.length - 1) != "/") {
                returnUrl += "/"
            }

            return returnUrl
        }

    val device: String = SystemProperties.get("ro.rr.device")

    val modVersion: String = SystemProperties.get("ro.build.display.id")

    val romName: String = Const.ROMNAME

    val romVersion: String = Const.RR_VERSION

    val romVersionHash: String?
        get() {
            val romHash = romName + romVersion
            return digest(romHash)
        }

    val timeFrame: Long
        get() = Const.TIMEFRAME.toLong()

    /**
     * Gets the Ask First value
     * 0: RomStats will behave like CMStats, starts reporting automatically after the tframe (default)
     * 1: RomStats will behave like the old CMStats, asks the user on first boot
     *
     * @return boolean
     */
    const val reportingMode: Int = 0

    @SuppressLint("HardwareIds")
    fun getUniqueID(ctx: Context): String? {
        var deviceId = digest(Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID))
        if (deviceId == null) {
            val wifiInterface = SystemProperties.get("wifi.interface")
            deviceId = try {
                val wifiMac = String(NetworkInterface.getByName(wifiInterface).hardwareAddress)
                digest(wifiMac)
            } catch (e: Exception) {
                null
            }
        }

        return deviceId
    }

    fun getCarrier(ctx: Context): String? {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var carrier = tm.networkOperatorName
        if ("" == carrier) {
            carrier = "Unknown"
        }
        return carrier
    }

    fun getCarrierId(ctx: Context): String? {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var carrierId = tm.networkOperator
        if ("" == carrierId) {
            carrierId = "0"
        }
        return carrierId
    }

    fun getCountryCode(ctx: Context): String {
        val tm = ctx.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var countryCode = tm.networkCountryIso
        if (countryCode == "") {
            countryCode = "Unknown"
        }
        return countryCode
    }

    private fun digest(input: String): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            BigInteger(1, md.digest(input.toByteArray())).toString(16).toUpperCase(Locale.US)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * @param context
     * @return false: opt out cookie not present, work normally
     * true: opt out cookie present, disable and close
     */
    fun persistentOptOut(context: Context): Boolean {
        val prefs = AnonymousStats.getPreferences(context)

        Log.d(Const.TAG, "[checkPersistentOptOut] Check prefs exist: " + prefs.contains(Const.ANONYMOUS_OPT_IN))
        if (!prefs.contains(Const.ANONYMOUS_OPT_IN)) {
            Log.d(Const.TAG, "[checkPersistentOptOut] New install, check for 'Persistent cookie'")

            val sdCard = Environment.getExternalStorageDirectory()
            val dir = File(sdCard.absolutePath + "/.ROMStats")
            val cookieFile = File(dir, "optout")

            if (cookieFile.exists()) {
                // if cookie exists, disable everything by setting:
                //   OPT_IN = false
                //   FIRST_BOOT = false
                Log.d(Const.TAG, "[checkPersistentOptOut] Persistent cookie exists -> Disable everything")

                prefs.edit().putBoolean(Const.ANONYMOUS_OPT_IN, false).apply()
                prefs.edit().putBoolean(Const.ANONYMOUS_FIRST_BOOT, false).apply()

                val mainPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                mainPrefs.edit().putBoolean(Const.ANONYMOUS_OPT_IN, false).apply()
                mainPrefs.edit().putBoolean(Const.ANONYMOUS_OPT_OUT_PERSIST, true).apply()

                return true
            } else {
                Log.d(Const.TAG, "[checkPersistentOptOut] No persistent cookie found")
            }
        }

        return false
    }

    fun checkIconVisibility(context: Context) {
        val sdCard = Environment.getExternalStorageDirectory()
        val dir = File(sdCard.absolutePath + "/.ROMStats")
        val cookieFile = File(dir, "hide_icon")

        val p = context.packageManager
        val componentToDisable = ComponentName("android.romstats", "android.romstats.AnonymousStats")
        if (cookieFile.exists()) {
            // exist, hide icon
            p.setComponentEnabledSetting(
                componentToDisable,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } else {
            // does not exist, show icon
            p.setComponentEnabledSetting(
                componentToDisable,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
