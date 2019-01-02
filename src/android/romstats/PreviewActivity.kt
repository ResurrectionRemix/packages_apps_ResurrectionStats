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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class PreviewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (supportFragmentManager.findFragmentById(android.R.id.content) == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }

    class SettingsFragment: PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preview_data)

            val prefSet = preferenceScreen
            val context = context!!

            prefSet.findPreference(UNIQUE_ID).summary = Utilities.getUniqueID(context)
            prefSet.findPreference(DEVICE).summary = Utilities.device
            prefSet.findPreference(VERSION).summary = Utilities.modVersion
            prefSet.findPreference(COUNTRY).summary = Utilities.getCountryCode(context)
            prefSet.findPreference(CARRIER).summary = Utilities.getCarrier(context)
            prefSet.findPreference(ROMNAME).summary = Utilities.romName
            prefSet.findPreference(ROMVERSION).summary = Utilities.romVersion

        }
    }

    companion object {

        private const val UNIQUE_ID = "preview_id"
        private const val DEVICE = "preview_device"
        private const val VERSION = "preview_version"
        private const val COUNTRY = "preview_country"
        private const val CARRIER = "preview_carrier"
        private const val ROMNAME = "preview_romname"
        private const val ROMVERSION = "preview_romversion"
    }
}
