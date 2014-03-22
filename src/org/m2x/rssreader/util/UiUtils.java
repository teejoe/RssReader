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

import org.m2x.rssreader.R;

import android.app.Activity;

/**
 * The UI utility class, used to set the APP theme.
 * @author teejoe(mtjm2x@gmail.com)
 *
 */
public class UiUtils {

	// Set the theme of a Activity according to the Preference.
    static public void setPreferenceTheme(Activity a) {
    	String theme = PrefUtils.getString(PrefUtils.THEME, PrefUtils.DEFAULT_THEME);
        
    	if (theme.equals(PrefUtils.LIGHT_THEME)){
    		a.setTheme(R.style.AppTheme_Light);
    	}
    	else if (theme.equals(PrefUtils.DEFAULT_THEME)){
    		a.setTheme(R.style.AppTheme_Default);
    	}
    	else if (theme.equals(PrefUtils.DARK_THEME)){
    		a.setTheme(R.style.AppTheme_Dark);
    	}
    }
}