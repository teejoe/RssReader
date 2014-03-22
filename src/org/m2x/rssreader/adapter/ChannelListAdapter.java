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

package org.m2x.rssreader.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.LinkedHashMap;
import java.util.Map;

import org.m2x.rssreader.provider.FeedData;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.util.PrefUtils;
import org.m2x.rssreader.util.StringUtils;
import org.m2x.rssreader.R;

public class ChannelListAdapter extends BaseAdapter {

	public static final long ALL_ID = -3;			// List item id of the "all" entry.
	public static final long FAVOURITE_ID = -2;		// List item id of the "favourite" entry.

    private int mPosId;
    private int mPosUrl;
    private int mPosName;
    private int mPosIsGroup;
    private int mPosIcon;
    private int mPosLastUpdate;
    private int mPosError;
    private int mPosUnread;

    private static final int CACHE_MAX_ENTRIES = 500;
    
    private final Map<Long, String> mFormattedDateCache = new 
    		LinkedHashMap<Long, String>(CACHE_MAX_ENTRIES + 1, .75F, true) {
    	
        private static final long serialVersionUID = -3678524849080041298L;

        @Override
        public boolean removeEldestEntry(Map.Entry<Long, String> eldest) {
            return size() > CACHE_MAX_ENTRIES;
        }
    };

    private final Context mContext;
    private Cursor mFeedsCursor;
    private int mAllUnreadNumber, mFavoritesNumber;

    private static class ViewHolder {
        public ImageView iconView;
        public TextView titleTxt;
        public TextView stateTxt;
        public TextView unreadTxt;
    }

    public ChannelListAdapter(Context context, Cursor feedCursor) {
        mContext = context;
        mFeedsCursor = feedCursor;

        updateNumbers();
    }

    public void setCursor(Cursor feedCursor) {
        mFeedsCursor = feedCursor;

        initCursorIndexes();
        
        updateNumbers();
        notifyDataSetChanged();
    }
    
    public Cursor getCursor() {
    	return mFeedsCursor;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
        		Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.channel_list_item, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.iconView = (ImageView) convertView.findViewById(R.id.icon);
            holder.titleTxt = (TextView) convertView.findViewById(android.R.id.text1);
            holder.stateTxt = (TextView) convertView.findViewById(android.R.id.text2);
            holder.unreadTxt = (TextView) convertView.findViewById(R.id.unread_count);
            convertView.setTag(holder);
        }

        ViewHolder holder = (ViewHolder) convertView.getTag();

        // default init
        holder.iconView.setImageDrawable(null);
        holder.titleTxt.setText("");
        holder.stateTxt.setVisibility(View.GONE);
        holder.unreadTxt.setText("");

        if (position == 0 || position == 1) {
            holder.titleTxt.setText(position == 0 ? R.string.all : R.string.favorites);
            holder.iconView.setImageResource(position == 0 ? R.drawable.ic_menu_rss : 
            		R.drawable.ic_menu_star);

            int unread = position == 0 ? mAllUnreadNumber : mFavoritesNumber;
            if (unread != 0) {
                holder.unreadTxt.setText(String.valueOf(unread));
            }
        } else if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            holder.titleTxt.setText((mFeedsCursor.isNull(mPosName) ? 
            		mFeedsCursor.getString(mPosUrl) : mFeedsCursor.getString(mPosName)));

            if (mFeedsCursor.getInt(mPosIsGroup) == 1) {
            	//holder.iconView.setVisibility(View.GONE);
            } else {
                holder.stateTxt.setVisibility(View.VISIBLE);

                if (mFeedsCursor.isNull(mPosError)) {
                    long timestamp = mFeedsCursor.getLong(mPosLastUpdate);

                    // Date formatting is expensive, look at the cache
                    String formattedDate = mFormattedDateCache.get(timestamp);
                    if (formattedDate == null) {

                        formattedDate = mContext.getString(R.string.update) + ":";

                        if (timestamp == 0) {
                            formattedDate += mContext.getString(R.string.never);
                        } else {
                            formattedDate += StringUtils.getDateTimeString(timestamp);
                        }

                        mFormattedDateCache.put(timestamp, formattedDate);
                    }

                    holder.stateTxt.setText(formattedDate);
                } else {
                    holder.stateTxt.setText(new StringBuilder(mContext.getString(R.string.error))
                    		.append(":").append(mFeedsCursor.getString(mPosError)));
                }

                byte[] iconBytes = mFeedsCursor.getBlob(mPosIcon);
                if (iconBytes != null && iconBytes.length > 0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                    holder.iconView.setImageBitmap(bitmap);
                } else {
                    holder.iconView.setImageResource(R.drawable.ic_launcher);
                }

                int unread = mFeedsCursor.getInt(mPosUnread);
                if (unread != 0) {
                    holder.unreadTxt.setText(String.valueOf(unread));
                }
            }
            
        }
        
        // Set the list selector according to current theme.
        if (PrefUtils.isDefaultTheme()) {
        	convertView.setBackgroundResource(R.drawable.list_selector_theme_default);
        } else {
        	convertView.setBackgroundResource(R.drawable.list_selector_theme_light);
        }

        return convertView;
    }

    @Override
    public int getCount() {
        if (mFeedsCursor != null) {
            return mFeedsCursor.getCount() + 2;
        }
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (position == 0) {
        	return ALL_ID;
        } else if (position == 1) {
        	return FAVOURITE_ID;
        }
    	
    	if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.getLong(mPosId);
        }
        
        return -1;
    }

    public byte[] getItemIcon(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.getBlob(mPosIcon);
        }

        return null;
    }

    public String getItemName(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.isNull(mPosName) ? 
            		mFeedsCursor.getString(mPosUrl) : mFeedsCursor.getString(mPosName);
        }

        return null;
    }

    /** Tell if the item is a group. */
    public boolean isItemAGroup(int position) {
        if (mFeedsCursor != null && mFeedsCursor.moveToPosition(position - 2)) {
            return mFeedsCursor.getInt(mPosIsGroup) == 1;
        }

        return false;
    }

    private void updateNumbers() {
        mAllUnreadNumber = mFavoritesNumber = 0;

        // Gets the numbers of entries (should be in a thread, but it's way easier like 
        // this and it shouldn't be so slow)
        Cursor numbers = mContext.getContentResolver().query(EntryColumns.CONTENT_URI, 
        		new String[]{FeedData.ALL_UNREAD_NUMBER, FeedData.FAVORITES_NUMBER}, 
        		null, null, null);
        if (numbers != null) {
            if (numbers.moveToFirst()) {
                mAllUnreadNumber = numbers.getInt(0);
                mFavoritesNumber = numbers.getInt(1);
            }
            numbers.close();
        }
    }
    
    private void initCursorIndexes() {
    	if (mFeedsCursor == null)	return;
    	
        mPosId = mFeedsCursor.getColumnIndex(FeedColumns._ID);
        mPosUrl = mFeedsCursor.getColumnIndex(FeedColumns.URL);
        mPosName = mFeedsCursor.getColumnIndex(FeedColumns.NAME);
        mPosIsGroup = mFeedsCursor.getColumnIndex(FeedColumns.IS_GROUP);
        mPosIcon = mFeedsCursor.getColumnIndex(FeedColumns.ICON);
        mPosLastUpdate = mFeedsCursor.getColumnIndex(FeedColumns.LAST_UPDATE);
        mPosError = mFeedsCursor.getColumnIndex(FeedColumns.ERROR);
        mPosUnread = 8;	// This columns is created by a select clause so has no column name.
    }
}
