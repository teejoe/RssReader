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

import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.R;
import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.util.UiUtils;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

/**
 * The Activity to edit a RSS channel.
 * @author teejoe(mtjm2x@gmail.com)
 *
 */
public class EditChannelActivity extends SherlockActivity implements
		View.OnClickListener {
	
	public static final String EXTRA_FEED_ID = "feed_id";
	
	private static long INVALID_FEED_ID = -1;
	
	private long mFeedId;
	
	private EditText mFeedName;
	private EditText mFeedUrl;
	
	private Cursor mCursor;
	
	private boolean mHasInit = false;	// Is the content of EditText has been initialized. 
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		UiUtils.setPreferenceTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_channel);
		
		mFeedId = getIntent().getLongExtra(EXTRA_FEED_ID, INVALID_FEED_ID);
		
		mFeedName = (EditText) findViewById(R.id.edit_feed_title);
		mFeedUrl = (EditText) findViewById(R.id.edit_feed_url);
		
		findViewById(R.id.btn_ok).setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
		
		initTitleAndUrl();
		
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_ok:
			updateFeedData();
			finish();
			break;
		case R.id.btn_cancel:
			finish();
			break;
		default:
			break;
		}
	}
	
	/** Initialize the feed title and URL and apply to the EditText. */
	private void initTitleAndUrl() {
		if (mFeedId == INVALID_FEED_ID)	return;
		
		new AsyncTask<Void, Void, Void> () {

			@Override
			protected Void doInBackground(Void... params) {
				mCursor = MainApplication.getContext().getContentResolver().query(
						FeedColumns.CONTENT_URI(mFeedId), null, null, null, null);
				return null;
			}
			
			@Override
			protected void onPostExecute(Void result) {
				if (mCursor != null && mCursor.moveToFirst()) {
					mFeedName.setText(mCursor.getString(mCursor.getColumnIndex(FeedColumns.NAME)));
					mFeedUrl.setText(mCursor.getString(mCursor.getColumnIndex(FeedColumns.URL)));
					mHasInit = true;
				}
			}
		}.execute();
	}
	
	/** Update the feed data. */
	private void updateFeedData() {
		if (mFeedId == INVALID_FEED_ID || !mHasInit) return;
		
		new Thread() {
			@Override
			public void run() {
				ContentValues values = new ContentValues();
				values.put(FeedColumns.NAME, mFeedName.getText().toString());
				values.put(FeedColumns.URL, mFeedUrl.getText().toString());
				
				MainApplication.getContext().getContentResolver().update(
						FeedColumns.CONTENT_URI(mFeedId), values, null, null);
			}
			
		}.start();
	}
}
