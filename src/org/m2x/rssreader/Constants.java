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

package org.m2x.rssreader;

import android.app.NotificationManager;
import android.content.Context;

public class Constants{
	
    public static final NotificationManager NOTIF_MGR = (NotificationManager) 
    		MainApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
	
	/** Fragment TAGs */
	public static final String CHANNEL_LIST_FRAGMENT = "ChannelListFragment";
	public static final String RSS_ITEM_LIST_FRAGMENT = "RssItemListFragment";
	public static final String RSS_ARTICLE_FRAGMENT = "RssArticleFragment";
	
	/** Database constants */
    public static final String DB_IS_TRUE = "=1";
    public static final String DB_IS_FALSE = "=0";
    public static final String DB_IS_NULL = " IS NULL";
    public static final String DB_IS_NOT_NULL = " IS NOT NULL";
    public static final String DB_DESC = " DESC";
    public static final String DB_ASC = " ASC";
    public static final String DB_ARG = "=?";
    public static final String DB_AND = " AND ";
    public static final String DB_OR = " OR ";
    
    /** Other constants. */
    public static final String FROM_AUTO_REFRESH = "from_auto_refresh";
    public static final String FEED_ID = "feed_id";
    
    public static final String HTTP_SCHEME = "http://";
    public static final String HTTPS_SCHEME = "https://";
    public static final String FILE_SCHEME = "file://";

    public static final String ENCLOSURE_SEPARATOR = "[@]"; // exactly three characters!

    public static final String HTML_QUOT = "&quot;";
    public static final String QUOT = "\"";
    public static final String HTML_APOSTROPHE = "&#39;";
    public static final String APOSTROPHE = "'";
    public static final String AMP = "&";
    public static final String AMP_SG = "&amp;";
    public static final String SLASH = "/";
    public static final String COMMA_SPACE = ", ";
    
    public static final String HTML_LT = "&lt;";
    public static final String HTML_GT = "&gt;";
    public static final String LT = "<";
    public static final String GT = ">";
    
    public static final int UPDATE_THROTTLE_DELAY = 500;
    
    public static final String BROADCAST_ACTION_REFRESH_FINISHED =
    		"org.m2x.rssreader.BROADCAST_REFRESH_FINISHED";
    
    public static final String BROADCAST_ACTION_NETWORK_PROBLEM =
    		"org.m2x.rssreader.BROADCAST_NETWORK_PROBLEM";
}