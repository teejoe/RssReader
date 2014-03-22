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

package org.m2x.rssreader.fragment;

import org.m2x.rssreader.adapter.ChannelListAdapter;
import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.R;


import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import com.actionbarsherlock.app.SherlockListFragment;


/**
 * The channel list fragment, including a ListView that shows all
 * the RSS channel titles that user have subscribed and corresponding refresh count.
 * If the user clicks the channel list item, it will navigate to the RSS item list 
 * for that channel.
 * @author teejoe(mtjm2x@gmail.com)
 *
 */
public class ChannelListFragment extends SherlockListFragment {
	

	// The channel list adapter.
	private ChannelListAdapter mAdapter;
	
    // The listener to notify when a channel is selected
    private OnChannelSelectedListener mChannelSelectedListener = null;
    
    // The listener to notify when a channel is long clicked. 
    private OnChannelLongClickListener mChannelLongClickListener = null;

    /** Represents a listener that will be notified of channel selections. */
    public interface OnChannelSelectedListener {
        /**
         * Called when a given channel is selected.
         * @param position the position of the selected channel.
         */
        public void onChannelSelected(int position);
    }
    
    /** Represents a listener that be notified of channel long clicks. */
    public interface OnChannelLongClickListener {
    	/**
    	 * Called when a given channel is long clicked.
    	 * @param position the position of the long clicked channel.
    	 */
    	public void onChannelLongClicked(int position);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState){
	
		return inflater.inflate(R.layout.fragment_channel_list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		
		// Set the ListView's long click listener.
		getListView().setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> listView, View view,
					int position, long id) {
				if (mChannelLongClickListener != null) {
					getListView().setItemChecked(position, true);
					mChannelLongClickListener.onChannelLongClicked(position);
					return true;
				}
				return false;
			}
		});
	}
	
    /** Set the listener that should be notified of channel selection events. */
    public void setChannelSelectedListener(OnChannelSelectedListener listener) {
        mChannelSelectedListener = listener;
    }
    
    /** Set the listener that should be notified of channel long click events. */
    public void setChannelLongClickListener (OnChannelLongClickListener listener) {
    	mChannelLongClickListener = listener;
    }
    
	@Override
	public void onListItemClick (ListView list, View view, int position, long id){
        if (null != mChannelSelectedListener) {
        	mChannelSelectedListener.onChannelSelected(position);
        }
        getListView().setItemChecked(position, true);
	}
	
	/** Get the position of checked item. */
	public int getCheckedItemPosition() {
		return getListView().getCheckedItemPosition();
	}
	
	/** Get the ID of checked item. */
	public long getCheckedItemId() {
		return getItemId(getCheckedItemPosition());
	}
	
	
	/** Set the cursor of ChannelListApdater. */
	public void setCursor(Cursor feedCursor){
		if (mAdapter == null){
			mAdapter = new ChannelListAdapter(getActivity(), feedCursor);
			mAdapter.setCursor(feedCursor);
			setListAdapter(mAdapter);
		}
		else {
			mAdapter.setCursor(feedCursor);
		}
	}
	
	public long getItemId(int position) {
		return mAdapter.getItemId(position);
	}
	
	public boolean isItemAGroup(int position) {
		return mAdapter.isItemAGroup(position);
	}
	
	/** Delete a checked item by remove the feed or feed group in the ContentProvider. Note that
	 * "all_feeds" and "favorite" item will not be removed. */
	public boolean deleteCheckedItem() {
		int position = getListView().getCheckedItemPosition();
		if (position == 0 || position == 1 || position == ListView.INVALID_POSITION) {
			return false;
		}
		//getListView().setItemChecked(position, false);
		
		Cursor cursor = mAdapter.getCursor();
		cursor.moveToPosition(position - 2);
		
		int id = cursor.getInt(cursor.getColumnIndex(FeedColumns._ID));
		boolean isGroup = cursor.getInt(cursor.getColumnIndex(FeedColumns.IS_GROUP)) == 1;
		if (isGroup) {
			MainApplication.getContext().getContentResolver().delete(FeedColumns
					.GROUPS_CONTENT_URI(id), null, null);
		} else {
			MainApplication.getContext().getContentResolver().delete(FeedColumns
					.CONTENT_URI(id), null, null);
		}
		
		return true;
	}
	
	public void notifyDataSetChanged() {
		mAdapter.notifyDataSetChanged();
	}
}
