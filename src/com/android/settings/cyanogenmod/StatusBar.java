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

package com.android.settings.cyanogenmod;

import android.content.Context;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.MSimTelephonyManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBar";

    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String KEY_STATUS_BAR_TICKER = "status_bar_ticker";
    private static final String STATUS_BAR_BATTERY_SHOW_PERCENT = "status_bar_battery_show_percent";

    private static final String STATUS_BAR_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_STYLE_TEXT = "6";
    private static final String SMS_BREATH = "sms_breath";
    private static final String MISSED_CALL_BREATH = "missed_call_breath";
    private static final String VOICEMAIL_BREATH = "voicemail_breath";

    private ListPreference mStatusBarBattery;
    private SystemSettingCheckBoxPreference mStatusBarBatteryShowPercent;
    private ListPreference mStatusBarCmSignal;
    private CheckBoxPreference mTicker;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mSMSBreath;
    private CheckBoxPreference mMissedCallBreath;
    private CheckBoxPreference mVoicemailBreath;

    private ContentObserver mSettingsObserver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY);
        mStatusBarBatteryShowPercent =
                (SystemSettingCheckBoxPreference) findPreference(STATUS_BAR_BATTERY_SHOW_PERCENT);
        mStatusBarCmSignal = (ListPreference) prefSet.findPreference(STATUS_BAR_SIGNAL);

        mSMSBreath = (CheckBoxPreference) findPreference(SMS_BREATH);
        mMissedCallBreath = (CheckBoxPreference) findPreference(MISSED_CALL_BREATH);
        mVoicemailBreath = (CheckBoxPreference) findPreference(VOICEMAIL_BREATH);

        Context context = getActivity();
        ConnectivityManager cm = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(cm.isNetworkSupported(ConnectivityManager.TYPE_MOBILE)) {
            mSMSBreath.setChecked(Settings.System.getInt(resolver,
                    Settings.System.KEY_SMS_BREATH, 0) == 1);
            mSMSBreath.setOnPreferenceChangeListener(this);

            mMissedCallBreath.setChecked(Settings.System.getInt(resolver,
                    Settings.System.KEY_MISSED_CALL_BREATH, 0) == 1);
            mMissedCallBreath.setOnPreferenceChangeListener(this);

            mVoicemailBreath.setChecked(Settings.System.getInt(resolver,
                    Settings.System.KEY_VOICEMAIL_BREATH, 0) == 1);
            mVoicemailBreath.setOnPreferenceChangeListener(this);
        } else {
            prefSet.removePreference(mSMSBreath);
            prefSet.removePreference(mMissedCallBreath);
            prefSet.removePreference(mVoicemailBreath);
        }

        mTicker = (CheckBoxPreference) prefSet.findPreference(KEY_STATUS_BAR_TICKER);
        mTicker.setChecked(Settings.System.getInt(
                getContentResolver(), Settings.System.TICKER_ENABLED, 1) == 1);
        mTicker.setOnPreferenceChangeListener(this);

        mStatusBarBrightnessControl = (CheckBoxPreference)
                prefSet.findPreference(Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL);
        refreshBrightnessControl();

        int batteryStyle = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        int signalStyle = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_SIGNAL_TEXT, 0);
        mStatusBarCmSignal.setValue(String.valueOf(signalStyle));
        mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntry());
        mStatusBarCmSignal.setOnPreferenceChangeListener(this);

        if (Utils.isWifiOnly(getActivity())
                || (MSimTelephonyManager.getDefault().isMultiSimEnabled())) {
            prefSet.removePreference(mStatusBarCmSignal);
        }

        enableStatusBarBatteryDependents(mStatusBarBattery.getValue());

        mSettingsObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                refreshBrightnessControl();
            }

            @Override
            public void onChange(boolean selfChange) {
                onChange(selfChange, null);
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE),
                true, mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_BATTERY, batteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);

            enableStatusBarBatteryDependents((String) newValue);
            return true;
        } else if (preference == mStatusBarCmSignal) {
            int signalStyle = Integer.valueOf((String) newValue);
            int index = mStatusBarCmSignal.findIndexOfValue((String) newValue);
            Settings.System.putInt(resolver, Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
            mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntries()[index]);
            return true;
        } else if (preference == mTicker) {
            Settings.System.putInt(getContentResolver(), Settings.System.TICKER_ENABLED,
                    (Boolean) newValue ? 1 : 0);
            return true;
        } else if (preference == mSMSBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_SMS_BREATH, value ? 1 : 0);
            return true;
	} else if (preference == mMissedCallBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_MISSED_CALL_BREATH, value ? 1 : 0);
            return true;
        } else if (preference == mVoicemailBreath) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(resolver, Settings.System.KEY_VOICEMAIL_BREATH, value ? 1 : 0);
            return true;
        }

        return false;
    }

    private void refreshBrightnessControl() {
        try {
            if (Settings.System.getInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            } else {
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_brightness_summary);
            }
        } catch (SettingNotFoundException e) {
            // Do nothing
        }
    }

    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_STYLE_TEXT)
                || value.equals(STATUS_BAR_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }
}
