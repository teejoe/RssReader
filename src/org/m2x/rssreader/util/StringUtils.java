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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

import org.m2x.rssreader.MainApplication;

public class StringUtils {

    public static DateFormat DATE_SHORT_FORMAT = null;

    static {
        DATE_SHORT_FORMAT = android.text.format.DateFormat.getDateFormat(MainApplication.getContext());
        
    }

    public static final DateFormat TIME_FORMAT = 
    		android.text.format.DateFormat.getTimeFormat(MainApplication.getContext());
    
    public static final int SIX_HOURS = 21600000; // six hours in milliseconds

    static public String getDateTimeString(long timestamp) {
        String outString;

        Date date = new Date(timestamp);
        Calendar calTimestamp = Calendar.getInstance();
        calTimestamp.setTimeInMillis(timestamp);
        Calendar calCurrent = Calendar.getInstance();

        if (calCurrent.getTimeInMillis() - timestamp < SIX_HOURS 
        		|| calCurrent.get(Calendar.DAY_OF_MONTH) 
        		== calTimestamp.get(Calendar.DAY_OF_MONTH)) {
            outString = TIME_FORMAT.format(date);
        } else {
            outString = DATE_SHORT_FORMAT.format(date) + ' ' + TIME_FORMAT.format(date);
        }

        return outString;
    }

    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
