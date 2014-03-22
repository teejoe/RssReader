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

import java.util.ArrayList;

import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;

import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.R;
import org.m2x.rssreader.adapter.RssItemListAdapter;
import org.m2x.rssreader.provider.FeedData.EntryColumns;

/**
 * The RSS item list fragment.
 * @author teejoe(mtjm2x@gmail.com)
 *
 */
public class RssItemListFragment extends SherlockListFragment{

	public static final String EXTRA_URI = "Uri";
	
	private Uri mUri;
	private RssItemListAdapter mAdapter;
	
	// Listener of the RSS item selections.
	private OnRssItemSelectedListener mOnRssItemSelectedListener;
	
    /**
     * Represents a listener that will be notified of RSS item selections.
     */
    public interface OnRssItemSelectedListener {
        /**
         * Called when a given RSS item is selected.
         * @param index the index of the selected RSS item.
         */
        public void onRssItemSelected(int index);
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
	
		return inflater.inflate(R.layout.fragment_rss_item_list, container, false);
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
				
		mAdapter = new RssItemListAdapter(getActivity(), null);
		this.setListAdapter(mAdapter);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Update the list data.
		if (mUri != null) {
			beginLoadData(mUri);
		}
	}
	
	/**
	 * Set the listener of RSS item selections.
	 * @param listener the listener to set.
	 */
	public void setRssItemSelectedListener(OnRssItemSelectedListener listener){
		mOnRssItemSelectedListener = listener;
	}
	
	@Override
	public void onListItemClick (ListView list, View view, int position, long id){
	
		if (null != mOnRssItemSelectedListener){
			mOnRssItemSelectedListener.onRssItemSelected(position);
		}
	}
	
	/** Begin to load the list items asynchronously. */
	public void beginLoadData(Uri uri){
		mUri = uri;
		new RssItemListLoaderTask().execute(uri);
	}
	
	/** Get the RSS entry IDs of the channel or group. */
	public ArrayList<Long> getRssEntryIds() {
		return mAdapter.getRssEntryIds(); 
	}
	
	/** Set the content uri for RSS entries. */
	public void setUri(Uri uri) {
		mUri = uri;
	}
	
	/** AsyncTask to load the list items. Given a content URI, fetch feed data from
	 *  the FeedDataProvider, then return the Cursor. */
	class RssItemListLoaderTask extends AsyncTask<Uri, Integer, Cursor> {

		@Override
		protected Cursor doInBackground(Uri... uri) {
			Cursor cursor = MainApplication.getContext().getContentResolver().query(
				    uri[0],  
				    null,
				    null,
				    null,
				    EntryColumns.DATE + " DESC");
			
			return cursor;
		}
		
		@Override
		protected void onPostExecute(Cursor cursor){
			mAdapter.setCursor(cursor);
			mAdapter.notifyDataSetChanged();
		}
	}
}
