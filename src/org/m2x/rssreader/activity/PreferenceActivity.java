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
 */

package org.m2x.rssreader.activity;

import org.m2x.rssreader.util.PrefUtils;
import org.m2x.rssreader.util.UiUtils;
import org.m2x.rssreader.R;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * The preference Activity.
 * @author teejoe(mtjm2x@gmail.com)
 *
 */
public class PreferenceActivity extends SherlockPreferenceActivity {
		
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
    	UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        addPreferencesFromResource(R.xml.preferences);
                
        /** Set the listener of the push notification preference. **/
        Preference preference = findPreference(PrefUtils.PUSH_NOTIFICATION);
        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (Boolean.TRUE.equals(newValue)) {
                	PrefUtils.putBoolean(PrefUtils.PUSH_NOTIFICATION, false);
                }
                else {
                	PrefUtils.putBoolean(PrefUtils.PUSH_NOTIFICATION, true);
                }
                return true;
            }
        });

        /** Set the listener of the theme preference. **/
        preference = findPreference(PrefUtils.THEME);
        preference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PrefUtils.putString(PrefUtils.THEME, (String)newValue);
            	android.os.Process.killProcess(android.os.Process.myPid());

                // this return statement will never be reached
                return true;
            }
        });
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
	    switch (menuItem.getItemId()) {
	    case android.R.id.home:
	        finish();
	        return true;
	    }
	    
	    return (super.onOptionsItemSelected(menuItem));
	}
    
}
