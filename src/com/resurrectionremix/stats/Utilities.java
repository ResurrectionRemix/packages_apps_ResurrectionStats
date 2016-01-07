/*
 * Copyright (C) 2016 ResurrectionRemix
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.resurrectionremix.stats;

import java.io.File;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Locale;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.os.SystemProperties;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Utilities {
	public static final String SETTINGS_PREF_NAME = "rrstats";
	public static final int NOTIFICATION_ID = 1;

	// For the Unique ID, I still use the IMEI or WiFi MAC address
	// CyanogenMod switched to use the Settings.Secure.ANDROID_ID
	// This is because the ANDROID_ID could change on hard reset, while IMEI remains equal
	public static String getUniqueID(Context ctx) {
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);

		String device_id = digest(tm.getDeviceId());
		if (device_id == null) {
			String wifiInterface = SystemProperties.get("wifi.interface");
			try {
				String wifiMac = new String(NetworkInterface.getByName(wifiInterface).getHardwareAddress());
				device_id = digest(wifiMac);
			} catch (Exception e) {
				device_id = null;
			}
		}

		return device_id;
	}

	public static String getCarrier(Context ctx) {
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		String carrier = tm.getNetworkOperatorName();
		if ("".equals(carrier)) {
			carrier = "Unknown";
		}
		return carrier;
	}

	public static String getCarrierId(Context ctx) {
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		String carrierId = tm.getNetworkOperator();
		if ("".equals(carrierId)) {
			carrierId = "0";
		}
		return carrierId;
	}

	public static String getCountryCode(Context ctx) {
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		String countryCode = tm.getNetworkCountryIso();
		if (countryCode.equals("")) {
			countryCode = "Unknown";
		}
		return countryCode;
	}

	public static String getDevice() {
		return SystemProperties.get("ro.product.model");
	}

	public static String getModVersion() {
		return SystemProperties.get("ro.build.display.id");
	}

	public static String getRomVersion() {
		return SystemProperties.get("ro.rrstats.version"); // version of this build
	}

	public static String getStatsUrl() {
		return SystemProperties.get("ro.rrstats.reportingUrl"); // reporting server address
	}

	public static String getRomVersionHash() {
		String romHash = getRomVersion();
		return digest(romHash);
	}

	public static long getTimeFrame() {
		String tFrameStr = SystemProperties.get("ro.rrstats.tframe", "1");
		return Long.valueOf(tFrameStr);
	}

	public static String digest(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			return new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase(Locale.US);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getSigningCert(Context context) {
		PackageInfo packageInfo = null;

		try {
			packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		Signature[] signatures = packageInfo.signatures;

		String signingCertHash = digest(signatures[0].toCharsString());

		return signingCertHash;
	}

	/**
	 * Gets the Ask First value
	 * 0: rrstats will behave like CMStats, starts reporting automatically after the tframe (default)
	 * 1: rrstats will behave like the old CMStats, asks the user on first boot
	 *
	 * @return boolean
	 */
	public static int getReportingMode() {
		return Const.RRSTATS_REPORTING_MODE_NEW;
	}

	/**
	 *
	 * @param context
	 * @return
	 * 	false: opt out cookie not present, work normally
	 * 	true: opt out cookie present, disable and close
	 */

	public static boolean persistentOptOut(Context context) {
		SharedPreferences prefs = AnonymousStats.getPreferences(context);

		Log.d(Const.TAG, "[checkPersistentOptOut] Check prefs exist: " + prefs.contains(Const.ANONYMOUS_OPT_IN));
		if (!prefs.contains(Const.ANONYMOUS_OPT_IN)) {
			Log.d(Const.TAG, "[checkPersistentOptOut] New install, check for 'Persistent cookie'");

			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/.ResurrectionStats");
			File cookieFile = new File(dir, "optout");

			if (cookieFile.exists()) {
				// if cookie exists, disable everything by setting:
				//   OPT_IN = false
				//   FIRST_BOOT = false
				Log.d(Const.TAG, "[checkPersistentOptOut] Persistent cookie exists -> Disable everything");

				prefs.edit().putBoolean(Const.ANONYMOUS_OPT_IN, false).apply();
				prefs.edit().putBoolean(Const.ANONYMOUS_FIRST_BOOT, false).apply();

				SharedPreferences mainPrefs = PreferenceManager.getDefaultSharedPreferences(context);
				mainPrefs.edit().putBoolean(Const.ANONYMOUS_OPT_IN, false).apply();
				mainPrefs.edit().putBoolean(Const.ANONYMOUS_OPT_OUT_PERSIST, true).apply();

				return true;
			} else {
				Log.d(Const.TAG, "[checkPersistentOptOut] No persistent cookie found");
			}
		};

		return false;
	}

	public static void checkIconVisibility(Context context) {
		PackageManager p = context.getPackageManager();
		ComponentName componentToDisable = new ComponentName("com.resurrectionremix.stats", "com.resurrectionremix.stats.AnonymousStats");

		p.setComponentEnabledSetting(componentToDisable, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
	}


}
