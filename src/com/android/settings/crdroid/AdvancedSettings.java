/*
 * Copyright (C) 2013 SlimRoms
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

package com.android.settings.crdroid;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.provider.Settings;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.util.Helpers;

import java.util.List;

public class AdvancedSettings extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {

    private static final String PREF_MEDIA_SCANNER_ON_BOOT = "media_scanner_on_boot";
    private static final String RESTART_SYSTEMUI = "restart_systemui";

    private ListPreference mMsob;
    private Preference mRestartSystemUI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.crdroid_advanced_settings);

        mMsob = (ListPreference) findPreference(PREF_MEDIA_SCANNER_ON_BOOT);
        mMsob.setValue(String.valueOf(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.MEDIA_SCANNER_ON_BOOT, 0)));
        mMsob.setSummary(mMsob.getEntry());
        mMsob.setOnPreferenceChangeListener(this);

        mRestartSystemUI = findPreference(RESTART_SYSTEMUI);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mRestartSystemUI) {
            Helpers.restartSystemUI();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String value = (String) newValue;
        if (preference == mMsob) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.MEDIA_SCANNER_ON_BOOT,
                    Integer.valueOf(value));

            mMsob.setValue(String.valueOf(value));
            mMsob.setSummary(mMsob.getEntry());
            return true;
        }
        return false;
    }

}
