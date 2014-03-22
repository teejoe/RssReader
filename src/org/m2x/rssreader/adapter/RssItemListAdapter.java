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

package org.m2x.rssreader.adapter;

import java.util.ArrayList;

import org.m2x.rssreader.R;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.util.StringUtils;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

/**
 * The list adapter for the RSS item list.
 * @author teejoe(mtjm2x@gmail.com)
 */
public class RssItemListAdapter extends BaseAdapter {
	
	private final Context mContext;
	private Cursor mCursor;
	LayoutInflater mInflater;
	
	public RssItemListAdapter(Context context, Cursor cursor) {
		mContext = context;
		mCursor = cursor;
		mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	@Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }
    
	@Override
	public int getCount() {
		if (mCursor != null) {
			return mCursor.getCount();
		}
		return 0;
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	static class ViewHolder {
		TextView channelName, updateTime, title;
		CheckBox hasRead;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = new ViewHolder();
		if (convertView == null){
			convertView = mInflater.inflate(R.layout.rss_list_item, parent, false);
			holder = new ViewHolder();
			holder.channelName = (TextView)convertView.findViewById(R.id.channel_name);
			holder.updateTime = (TextView)convertView.findViewById(R.id.update_time);
			holder.hasRead = (CheckBox)convertView.findViewById(R.id.has_read);
			holder.title = (TextView)convertView.findViewById(R.id.title);
			
			convertView.setTag(holder);
		}
		else {
			holder = (ViewHolder)convertView.getTag();
		}
		
		if (mCursor != null && mCursor.moveToPosition(position)) {
			holder.channelName.setText(mCursor.getString(mCursor.getColumnIndex(
					EntryColumns.FEED_ID)));
			holder.updateTime.setText(StringUtils.getDateTimeString(mCursor.getLong(
					mCursor.getColumnIndex(EntryColumns.DATE))));
			holder.title.setText(mCursor.getString(mCursor.getColumnIndex(
					EntryColumns.TITLE)));
			holder.channelName.setText(mCursor.getString(mCursor.getColumnIndex(
					FeedColumns.NAME)));
			holder.hasRead.setChecked(mCursor.getInt(mCursor.getColumnIndex(
					EntryColumns.IS_READ)) == 1);
		}
		
		return convertView;
	}
	
	public void setCursor(Cursor cursor) {
		mCursor = cursor;
		notifyDataSetChanged();
	}
	
	/** Get the RSS entry ids. */
	public ArrayList<Long> getRssEntryIds() {
		if (mCursor.getCount() == 0) {
			return null;
		}
		
		ArrayList<Long> entryIds = new ArrayList<Long>();
		
		mCursor.moveToFirst();
		do {
			Long id = mCursor.getLong(mCursor.getColumnIndex(EntryColumns._ID));
			entryIds.add(id);
		} while (mCursor.moveToNext());
		
		return entryIds;
	}
}
