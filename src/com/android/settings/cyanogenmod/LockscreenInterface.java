/*
 * Copyright (C) 2012-2014 The CyanogenMod Project
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

import android.app.admin.DevicePolicyManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.ChooseLockSettingsHelper;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.settings.crdroid.SeekBarPreferenceCHOS;

public class LockscreenInterface extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "LockscreenInterface";

    private static final String LOCKSCREEN_GENERAL_CATEGORY = "lockscreen_general_category";
    private static final String LOCKSCREEN_WIDGETS_CATEGORY = "lockscreen_widgets_category";
    private static final String KEY_LOCKSCREEN_ALL_WIDGETS = "lockscreen_all_widgets";
    private static final String KEY_BATTERY_STATUS = "lockscreen_battery_status";
    private static final String KEY_LOCKSCREEN_BUTTONS = "lockscreen_buttons";
    private static final String KEY_ENABLE_WIDGETS = "keyguard_enable_widgets";
    private static final String KEY_LOCK_CLOCK = "lock_clock";
    private static final String KEY_ENABLE_CAMERA = "keyguard_enable_camera";
    private static final String KEY_ENABLE_APPLICATION_WIDGET =
            "keyguard_enable_application_widget";
    private static final String KEY_ENABLE_MAXIMIZE_WIGETS = "lockscreen_maximize_widgets";
    private static final String KEY_LOCKSCREEN_MODLOCK_ENABLED = "lockscreen_modlock_enabled";
    private static final String KEY_LOCKSCREEN_TARGETS = "lockscreen_targets";
    private static final String LOCK_BEFORE_UNLOCK = "lock_before_unlock";
    private static final String KEY_DISABLE_FRAME = "lockscreen_disable_frame";
    private static final String KEY_SEE_THROUGH = "see_through";
    private static final String KEY_BLUR_RADIUS = "blur_radius";
    private static final String KEY_ENABLE_POWER_MENU = "lockscreen_enable_power_menu";
    private static final String KEY_LOCKSCREEN_MUSIC_CONTROLLER ="keyguard_enable_musiccontroller";

    private static final int DLG_ALL_WIDGETS = 0;

    private CheckBoxPreference mEnableKeyguardWidgets;
    private CheckBoxPreference mAllWidgets;
    private CheckBoxPreference mEnableCameraWidget;
    private CheckBoxPreference mEnableApplicationWidget;
    private CheckBoxPreference mEnableModLock;
    private CheckBoxPreference mEnableMaximizeWidgets;
    private ListPreference mBatteryStatus;
    private Preference mLockscreenTargets;
    private CheckBoxPreference mLockBeforeUnlock;
    private CheckBoxPreference mDisableFrame;
    private CheckBoxPreference mSeeThrough;
    private SeekBarPreferenceCHOS mBlurRadius;
    private CheckBoxPreference mEnablePowerMenu;
    private CheckBoxPreference mLockscreenMusicController;

    private ChooseLockSettingsHelper mChooseLockSettingsHelper;
    private LockPatternUtils mLockUtils;
    private DevicePolicyManager mDPM;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.lockscreen_interface_settings);

        mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
        mLockUtils = mChooseLockSettingsHelper.utils();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Find categories
        PreferenceCategory generalCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_GENERAL_CATEGORY);
        PreferenceCategory widgetsCategory = (PreferenceCategory)
                findPreference(LOCKSCREEN_WIDGETS_CATEGORY);

        // Find preferences
        mEnableKeyguardWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_WIDGETS);
        mAllWidgets = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ALL_WIDGETS);
        mEnableCameraWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_CAMERA);
        mEnableApplicationWidget = (CheckBoxPreference) findPreference(KEY_ENABLE_APPLICATION_WIDGET);
        mEnableMaximizeWidgets = (CheckBoxPreference) findPreference(KEY_ENABLE_MAXIMIZE_WIGETS);
        mLockscreenTargets = findPreference(KEY_LOCKSCREEN_TARGETS);

        mEnableModLock = (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_MODLOCK_ENABLED);
        if (mEnableModLock != null) {
            mEnableModLock.setOnPreferenceChangeListener(this);
        }

        // Keyguard widget frame
        mDisableFrame = (CheckBoxPreference) findPreference(KEY_DISABLE_FRAME);

        // Enable/disable power menu on secure lockscreen
        mEnablePowerMenu = (CheckBoxPreference) findPreference(KEY_ENABLE_POWER_MENU);
        mEnablePowerMenu.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_ENABLE_POWER_MENU, 0) == 1);
        mEnablePowerMenu.setOnPreferenceChangeListener(this);

        mBatteryStatus = (ListPreference) findPreference(KEY_BATTERY_STATUS);
        if (mBatteryStatus != null) {
            mBatteryStatus.setOnPreferenceChangeListener(this);
        }

        // Remove lockscreen button actions if device doesn't have hardware keys
        if (!hasButtons()) {
            generalCategory.removePreference(findPreference(KEY_LOCKSCREEN_BUTTONS));
        }

        // Enable or disable lockscreen widgets based on policy
        checkDisabledByPolicy(mEnableKeyguardWidgets,
                DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL);

        // Enable all widgets
        mAllWidgets.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0) == 1);
        mAllWidgets.setOnPreferenceChangeListener(this);

        // Lock before Unlock
        mLockBeforeUnlock = (CheckBoxPreference) findPreference(LOCK_BEFORE_UNLOCK);

        // lockscreen see through
        mSeeThrough = (CheckBoxPreference) findPreference(KEY_SEE_THROUGH);
        if (mSeeThrough != null) {
            mSeeThrough.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH, 0) == 1);
        }

        // Blur radius
        mBlurRadius = (SeekBarPreferenceCHOS) findPreference(KEY_BLUR_RADIUS);
        if (mBlurRadius != null) {
            mBlurRadius.setValue(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_BLUR_RADIUS, 12));
            mBlurRadius.setOnPreferenceChangeListener(this);
        }
        
        // Enable or disable camera widget based on device and policy
        if (Camera.getNumberOfCameras() == 0) {
            widgetsCategory.removePreference(mEnableCameraWidget);
            mEnableCameraWidget = null;
            mLockUtils.setCameraEnabled(false);
        } else if (mLockUtils.isSecure()) {
            checkDisabledByPolicy(mEnableCameraWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_SECURE_CAMERA);
        }

        boolean canEnableModLockscreen = false;
        final String keyguardPackage = getActivity().getString(
                com.android.internal.R.string.config_keyguardPackage);
        final Bundle keyguard_metadata = Utils.getApplicationMetadata(
                getActivity(), keyguardPackage);
        if (keyguard_metadata != null) {
            canEnableModLockscreen = keyguard_metadata.getBoolean(
                    "com.cyanogenmod.keyguard", false);
        }

        if (mEnableModLock != null && !canEnableModLockscreen) {
            generalCategory.removePreference(mEnableModLock);
            mEnableModLock = null;
        }

        // Remove cLock settings item if not installed
        if (!Utils.isPackageInstalled(getActivity(), "com.cyanogenmod.lockclock")) {
            widgetsCategory.removePreference(findPreference(KEY_LOCK_CLOCK));
        }

        // Remove maximize widgets on tablets
        if (!Utils.isPhone(getActivity())) {
            widgetsCategory.removePreference(
                    mEnableMaximizeWidgets);
        }
        mLockscreenMusicController = (CheckBoxPreference)findPreference(KEY_LOCKSCREEN_MUSIC_CONTROLLER);
        mLockscreenMusicController.setChecked(Settings.System.getInt(getContentResolver(), Settings.System.LOCKSCREEN_MUSIC_SWITCH, 1) == 1);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Update custom widgets and camera
        if (mEnableKeyguardWidgets != null) {
            mEnableKeyguardWidgets.setChecked(mLockUtils.getWidgetsEnabled());
        }

        if (mEnableApplicationWidget != null) {
            mEnableApplicationWidget.setChecked(mLockUtils.getApplicationWidgetEnabled());
        }

        if (mEnableCameraWidget != null) {
            mEnableCameraWidget.setChecked(mLockUtils.getCameraEnabled());
        }

        // Update battery status
        if (mBatteryStatus != null) {
            ContentResolver cr = getActivity().getContentResolver();
            int batteryStatus = Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, 0);
            mBatteryStatus.setValueIndex(batteryStatus);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[batteryStatus]);
        }

        // Update mod lockscreen status
        if (mEnableModLock != null) {
            ContentResolver cr = getActivity().getContentResolver();
            boolean checked = Settings.System.getInt(
                    cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED, 1) == 1;
            mEnableModLock.setChecked(checked);
        }

        // Lockscreen frame
        if (mDisableFrame != null) {
            mDisableFrame.setChecked(Settings.System.getInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED, 0) == 1);
            mDisableFrame.setOnPreferenceChangeListener(this);
        }

        updateAvailableModLockPreferences();
    }

    private void updateAvailableModLockPreferences() {
        if (mEnableModLock == null) {
            return;
        }

        boolean enabled = !mEnableModLock.isChecked();
        if (mEnableKeyguardWidgets != null) {
            // Enable or disable lockscreen widgets based on policy
            if(!checkDisabledByPolicy(mEnableKeyguardWidgets,
                    DevicePolicyManager.KEYGUARD_DISABLE_WIDGETS_ALL)) {
                mEnableKeyguardWidgets.setEnabled(enabled);
            }
        }
        if (mEnableApplicationWidget != null) {
            // Enable or disable application widgets based on policy
            if (!checkDisabledByPolicy(mEnableApplicationWidget,
                    DevicePolicyManager.KEYGUARD_DISABLE_APPLICATION_WIDGET)) {
                mEnableApplicationWidget.setEnabled(enabled);
            }
        }
        if (mEnableMaximizeWidgets != null) {
            mEnableMaximizeWidgets.setEnabled(enabled);
        }
        if (mLockscreenTargets != null) {
            mLockscreenTargets.setEnabled(enabled);
        }
        if (mBatteryStatus != null) {
            mBatteryStatus.setEnabled(enabled);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final String key = preference.getKey();

        if (KEY_ENABLE_WIDGETS.equals(key)) {
            mLockUtils.setWidgetsEnabled(mEnableKeyguardWidgets.isChecked());
            return true;
        } else if (KEY_ENABLE_CAMERA.equals(key)) {
            mLockUtils.setCameraEnabled(mEnableCameraWidget.isChecked());
            return true;
        } else if (KEY_LOCKSCREEN_MUSIC_CONTROLLER.equals(key)) {
	    boolean value = mLockscreenMusicController.isChecked();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_MUSIC_SWITCH, value ? 1 : 0);
            return true;
        } else if (preference == mSeeThrough) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_SEE_THROUGH,
                    mSeeThrough.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mLockBeforeUnlock) {
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.LOCK_BEFORE_UNLOCK,
                    mLockBeforeUnlock.isChecked() ? 1 : 0);
        } else if (KEY_ENABLE_APPLICATION_WIDGET.equals(key)) {
            mLockUtils.setApplicationWidgetEnabled(mEnableApplicationWidget.isChecked());
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver cr = getActivity().getContentResolver();
        if (preference == mBatteryStatus) {
            int value = Integer.valueOf((String) objValue);
            int index = mBatteryStatus.findIndexOfValue((String) objValue);
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_BATTERY_VISIBILITY, value);
            mBatteryStatus.setSummary(mBatteryStatus.getEntries()[index]);
            return true;
        } else if (preference == mBlurRadius) {
                    Settings.System.putInt(getContentResolver(),
            Settings.System.LOCKSCREEN_BLUR_RADIUS, (Integer) objValue);
            return true;
        } else if (preference == mEnableModLock) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(cr, Settings.System.LOCKSCREEN_MODLOCK_ENABLED,
                    value ? 1 : 0);
            // force it so update picks up correct values
            ((CheckBoxPreference) preference).setChecked(value);
            updateAvailableModLockPreferences();
            return true;
        } else if (preference == mDisableFrame) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_WIDGET_FRAME_ENABLED,
                    (Boolean) objValue ? 1 : 0);
            return true;
        } else if (preference == mAllWidgets) {
            final boolean checked = (Boolean) objValue;
            if (checked) {
                showDialogInner(DLG_ALL_WIDGETS);
            } else {
                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0);
            }
            return true;
        } else if (preference == mEnablePowerMenu) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_ENABLE_POWER_MENU,
                    (Boolean) objValue ? 1 : 0);
            return true;
        }

        return false;
    }

    /**
     * Checks if the device has hardware buttons.
     * @return has Buttons
     */
    public boolean hasButtons() {
        return (getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys) > 0);
    }

    /**
     * Checks if a specific policy is disabled by a device administrator, and disables the
     * provided preference if so.
     * @param preference Preference
     * @param feature Feature
     * @return True if disabled.
     */
    private boolean checkDisabledByPolicy(Preference preference, int feature) {
        boolean disabled = featureIsDisabled(feature);

        if (disabled) {
            preference.setSummary(R.string.security_enable_widgets_disabled_summary);
        }

        preference.setEnabled(!disabled);
        return disabled;
    }

    /**
     * Checks if a specific policy is disabled by a device administrator.
     * @param feature Feature
     * @return Is disabled
     */
    private boolean featureIsDisabled(int feature) {
        return (mDPM.getKeyguardDisabledFeatures(null) & feature) != 0;
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        LockscreenInterface getOwner() {
            return (LockscreenInterface) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_ALL_WIDGETS:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lockscreen_allow_all_title)
                    .setMessage(R.string.lockscreen_allow_all_warning)
                    .setNegativeButton(R.string.cancel,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            disableSetting();
                        }
                    })
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.Secure.putInt(getActivity().getContentResolver(),
                                    Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 1);
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_ALL_WIDGETS:
                    disableSetting();
                    break;
                default:
                    // None for now
            }
        }

        private void disableSetting() {
            if (getOwner().mAllWidgets != null) {
                Settings.Secure.putInt(getActivity().getContentResolver(),
                        Settings.Secure.ALLOW_ALL_LOCKSCREEN_WIDGETS, 0);
                getOwner().mAllWidgets.setChecked(false);
            }
        }
    }
}
