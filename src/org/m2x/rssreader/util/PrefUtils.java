/**
 * RssReader
 *
 * Copyright (c) 2013-2014 teejoe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Some parts of this software are based on "FeedEx" (see below)
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.m2x.rssreader.util;

import org.m2x.rssreader.MainApplication;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;

/**
 * The preference utility class, which can set add get SharedPreference values conveniently.
 * @author teejoe(teejoe@163.com)
 *
 */
public class PrefUtils {
	
	// Preference keys. (must be corresponding to keys in preference.xml)
	public static final String THEME = "pref_theme";
    
	public static final String AUTO_REFRESH_ENABLED = "pref_auto_refresh_enabled";
	public static final String AUTO_REFRESH_ONLY_ON_WIFI = "pref_auto_refresh_only_on_wifi";
    public static final String REFRESH_ON_OPEN_ENABLED = "pref_refresh_on_open";
    public static final String AUTO_REFRESH_INTERVAL = "pref_auto_refresh_interval";
    
	public static final String PUSH_NOTIFICATION = "pref_push_notification";
    public static final String NOTIFICATIONS_RINGTONE = "pref_notifications_ringtone";
    public static final String NOTIFICATIONS_VIBRATE = "pref_notifications_vibrate";
    
    public static final String KEEP_TIME = "pref_keeptime";
    
    public static final String FONT_SIZE = "pref_font_size";
    
    public static final String FETCH_PICTURES = "pref_fetch_pictures";
    
    public static final String LAST_SCHEDULED_REFRESH = "pref_last_scheduled_refresh";
    
    public static final String IS_REFRESHING = "pref_is_refreshing";
    
    public static final String SHOW_READ = "pref_show_read";

	
	// Preference values. (must be corresponding to values in preference.xml)
	public static final String DEFAULT_THEME = "default";
	public static final String LIGHT_THEME = "light";
	public static final String DARK_THEME = "dark";
		
	/** Tell if the current theme is Light. */
	public static boolean isLightTheme() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext());
		if (settings.getString(THEME, DEFAULT_THEME).equals(DARK_THEME)) {
			return false;
		}
		
		return true;
	}
	
	/** Tell if the current theme is default. */
	public static boolean isDefaultTheme() {
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext());
		if (settings.getString(THEME, DEFAULT_THEME).equals(DEFAULT_THEME)) {
			return true;
		}
		
		return false;		
	}
	
	/** Get boolean value of the SharePreferences. */
    public static boolean getBoolean(String key, boolean defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext());
        return settings.getBoolean(key, defValue);
    }

    /** Put boolean value to the SharePreferences. */
    public static void putBoolean(String key, boolean value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext()).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    /** Get string value of the SharePreferences. */
    public static String getString(String key, String defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext());
        return settings.getString(key, defValue);
    }

    /** Put string value to the SharePreferences. */
    public static void putString(String key, String value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext()).edit();
        editor.putString(key, value);
        editor.commit();
    }
    
    /** Get long value of the SharedPreferences. */
    public static long getLong(String key, long defValue) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext());
        return settings.getLong(key, defValue);
    }

    /** Put long value to the SharedPreferences. */
    public static void putLong(String key, long value) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(
        		MainApplication.getContext()).edit();
        editor.putLong(key, value);
        editor.commit();
    }
    
    public static void registerOnPrefChangeListener(OnSharedPreferenceChangeListener listener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
            		.registerOnSharedPreferenceChangeListener(listener);
        } catch (Exception ignored) { // Seems to be possible to have a NPE here... Why??
        }
    }

    public static void unregisterOnPrefChangeListener(OnSharedPreferenceChangeListener listener) {
        try {
            PreferenceManager.getDefaultSharedPreferences(MainApplication.getContext())
            		.unregisterOnSharedPreferenceChangeListener(listener);
        } catch (Exception ignored) { // Seems to be possible to have a NPE here... Why??
        }
    }
}
