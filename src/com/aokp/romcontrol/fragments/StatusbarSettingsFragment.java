/*
* Copyright (C) 2015 The Android Open Kang Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.aokp.romcontrol.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.DateFormat;
import android.view.View;

import com.android.internal.logging.MetricsLogger;
import cyanogenmod.providers.CMSettings;

import static com.android.internal.util.cm.PowerMenuConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aokp.romcontrol.R;

public class StatusbarSettingsFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActivity().getFragmentManager().beginTransaction()
                .replace(R.id.container, new SettingsPreferenceFragment())
                .commit();
    }

    public static class SettingsPreferenceFragment extends PreferenceFragment
            implements OnPreferenceChangeListener {
        public SettingsPreferenceFragment() {
        }

        private static final String TAG = "StatusBar";

        private ContentResolver mContentResolver;

        private static final String STATUS_BAR_CLOCK_STYLE = "status_bar_clock";
        private static final String STATUS_BAR_AM_PM = "status_bar_am_pm";
        private static final String STATUS_BAR_BATTERY_STYLE = "status_bar_battery_style";
        private static final String STATUS_BAR_SHOW_BATTERY_PERCENT = "status_bar_show_battery_percent";

        private static final int STATUS_BAR_BATTERY_STYLE_HIDDEN = 4;
        private static final int STATUS_BAR_BATTERY_STYLE_TEXT = 6;

        private ListPreference mStatusBarClock;
        private ListPreference mStatusBarAmPm;
        private ListPreference mStatusBarBattery;
        private ListPreference mStatusBarBatteryShowPercent;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.fragment_statusbar_settings);

            ContentResolver resolver = getActivity().getContentResolver();

            mStatusBarClock = (ListPreference) findPreference(STATUS_BAR_CLOCK_STYLE);
            mStatusBarAmPm = (ListPreference) findPreference(STATUS_BAR_AM_PM);
            mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY_STYLE);
            mStatusBarBatteryShowPercent =
                    (ListPreference) findPreference(STATUS_BAR_SHOW_BATTERY_PERCENT);

            int clockStyle = CMSettings.System.getInt(resolver,
                    CMSettings.System.STATUS_BAR_CLOCK, 1);
            mStatusBarClock.setValue(String.valueOf(clockStyle));
            mStatusBarClock.setSummary(mStatusBarClock.getEntry());
            mStatusBarClock.setOnPreferenceChangeListener(this);

            if (DateFormat.is24HourFormat(getActivity())) {
                mStatusBarAmPm.setEnabled(false);
                mStatusBarAmPm.setSummary(R.string.status_bar_am_pm_info);
            } else {
                int statusBarAmPm = CMSettings.System.getInt(resolver,
                        CMSettings.System.STATUS_BAR_AM_PM, 2);
                mStatusBarAmPm.setValue(String.valueOf(statusBarAmPm));
                mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntry());
                mStatusBarAmPm.setOnPreferenceChangeListener(this);
            }

            int batteryStyle = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_BATTERY_STYLE, 0);
            mStatusBarBattery.setValue(String.valueOf(batteryStyle));
                mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
            mStatusBarBattery.setOnPreferenceChangeListener(this);

            int batteryShowPercent = CMSettings.System.getInt(resolver,
                    CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0);
            mStatusBarBatteryShowPercent.setValue(String.valueOf(batteryShowPercent));
            mStatusBarBatteryShowPercent.setSummary(mStatusBarBatteryShowPercent.getEntry());
            enableStatusBarBatteryDependents(batteryStyle);
            mStatusBarBatteryShowPercent.setOnPreferenceChangeListener(this);
        }

        protected ContentResolver getContentResolver() {
            Context context = getActivity();
            if (context != null) {
                mContentResolver = context.getContentResolver();
            }
            return mContentResolver;
        }

        protected int getMetricsCategory() {
            // todo add a constant in MetricsLogger.java
            return MetricsLogger.MAIN_SETTINGS;
        }

        @Override
        public void onResume() {
            super.onResume();
            // Adjust clock position for RTL if necessary
            Configuration config = getResources().getConfiguration();
            if (config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
                    mStatusBarClock.setEntries(getActivity().getResources().getStringArray(
                            R.array.status_bar_clock_style_entries_rtl));
                    mStatusBarClock.setSummary(mStatusBarClock.getEntry());
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            ContentResolver resolver = getActivity().getContentResolver();
            if (preference == mStatusBarClock) {
                int clockStyle = Integer.parseInt((String) newValue);
                int index = mStatusBarClock.findIndexOfValue((String) newValue);
                CMSettings.System.putInt(
                        resolver, CMSettings.System.STATUS_BAR_CLOCK, clockStyle);
                mStatusBarClock.setSummary(mStatusBarClock.getEntries()[index]);
                return true;
            } else if (preference == mStatusBarAmPm) {
                int statusBarAmPm = Integer.valueOf((String) newValue);
                int index = mStatusBarAmPm.findIndexOfValue((String) newValue);
                CMSettings.System.putInt(
                        resolver, CMSettings.System.STATUS_BAR_AM_PM, statusBarAmPm);
                mStatusBarAmPm.setSummary(mStatusBarAmPm.getEntries()[index]);
                return true;
            } else if (preference == mStatusBarBattery) {
                int batteryStyle = Integer.valueOf((String) newValue);
                int index = mStatusBarBattery.findIndexOfValue((String) newValue);
                CMSettings.System.putInt(
                        resolver, CMSettings.System.STATUS_BAR_BATTERY_STYLE, batteryStyle);
                mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
                enableStatusBarBatteryDependents(batteryStyle);
                return true;
            } else if (preference == mStatusBarBatteryShowPercent) {
                int batteryShowPercent = Integer.valueOf((String) newValue);
                int index = mStatusBarBatteryShowPercent.findIndexOfValue((String) newValue);
                CMSettings.System.putInt(
                        resolver, CMSettings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, batteryShowPercent);
                mStatusBarBatteryShowPercent.setSummary(
                        mStatusBarBatteryShowPercent.getEntries()[index]);
                return true;
            }
            return false;
        }

        private void enableStatusBarBatteryDependents(int batteryIconStyle) {
            if (batteryIconStyle == STATUS_BAR_BATTERY_STYLE_HIDDEN ||
                    batteryIconStyle == STATUS_BAR_BATTERY_STYLE_TEXT) {
                mStatusBarBatteryShowPercent.setEnabled(false);
            } else {
                mStatusBarBatteryShowPercent.setEnabled(true);
            }
        }

    }
}