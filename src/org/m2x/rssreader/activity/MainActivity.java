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

import java.util.ArrayList;

import org.m2x.rssreader.Constants;
import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.fragment.ChannelListFragment;
import org.m2x.rssreader.fragment.RssItemListFragment;
import org.m2x.rssreader.provider.FeedData;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.service.FetcherService;
import org.m2x.rssreader.service.RefreshService;
import org.m2x.rssreader.util.PrefUtils;
import org.m2x.rssreader.util.UiUtils;
import org.m2x.rssreader.R;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Toast;

/**
 * The main activity of the APP.
 * @author teejoe (mtjm2x@gmail.com)
 *
 */
public class MainActivity extends SherlockFragmentActivity implements
		ChannelListFragment.OnChannelSelectedListener,
		ChannelListFragment.OnChannelLongClickListener,
		RssItemListFragment.OnRssItemSelectedListener,
		LoaderManager.LoaderCallbacks<Cursor> {
		
	private static final int LOADER_ID = 0;

	private long mPrevBackTime;			// Last time when back key is pressed.
	
	private String mHintPressBackKey;	// The hint to show when back key is pressed.
	
	private ChannelListFragment mChannelListFragment;	// The fragment to show channel list.
		
	private RssItemListFragment mRssItemListFragment;	// The fragment to show RSS item list.

	private MenuItem mMenuRefresh;		// The refresh menu item, used to change refresh icon to progress bar when refresh service begins.
	
	private ActionMode mActionModeEditChannel;			// The ActionMode to edit RSS channel.
	
	public boolean mIsDualPane;	// The current layout is dual pane or not.
	
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		UiUtils.setPreferenceTheme(this);
		super.onCreate(savedInstanceState);
				
		setContentView(R.layout.main_layout);
		
		mHintPressBackKey = getResources().getString(R.string.hint_press_back_key);
 
        // Determine whether we are in single-pane or dual-pane mode by testing the visibility
        // of the channel list view.
        View rssItemList = findViewById(R.id.rss_item_list);
        mIsDualPane = rssItemList != null && rssItemList.getVisibility() == View.VISIBLE;

        // Create the channel list fragment if it's null.
        mChannelListFragment = (ChannelListFragment)getSupportFragmentManager()
        		.findFragmentByTag(Constants.CHANNEL_LIST_FRAGMENT);
        if (mChannelListFragment == null){
        	mChannelListFragment = new ChannelListFragment();
        	getSupportFragmentManager().beginTransaction().add(
        			R.id.channel_list, mChannelListFragment, Constants.CHANNEL_LIST_FRAGMENT).commit();
        }
        mChannelListFragment.setChannelSelectedListener(this);
        mChannelListFragment.setChannelLongClickListener(this);
    	
    	// Create the RSS item list if it's null.
    	mRssItemListFragment = (RssItemListFragment)getSupportFragmentManager()
    			.findFragmentByTag(Constants.RSS_ITEM_LIST_FRAGMENT);
    	if (mRssItemListFragment == null){
    		mRssItemListFragment = new RssItemListFragment();
    	}
    	mRssItemListFragment.setRssItemSelectedListener(this);
        
    	// Initialize the selected channel position.        
        setChannelSelected(0);		
        
    	// Start the RefreshService to auto refresh.
    	if (PrefUtils.getBoolean(PrefUtils.AUTO_REFRESH_ENABLED, true)) {
            startService(new Intent(this, RefreshService.class));
        } else {
            stopService(new Intent(this, RefreshService.class));
        }
    	
    	// Begin to fetch RSS feeds.
        if (PrefUtils.getBoolean(PrefUtils.REFRESH_ON_OPEN_ENABLED, false)) {
            if (!PrefUtils.getBoolean(PrefUtils.IS_REFRESHING, false)) {
                startService(new Intent(MainActivity.this, FetcherService.class)
                		.setAction(FetcherService.ACTION_REFRESH_FEEDS));
            }
        }
        
        // Register the BroadcastReceiver to receive messages from FetchService.
        LocalBroadcastManager.getInstance(this).registerReceiver(new ResponseReceiver(),  
        		new IntentFilter(Constants.BROADCAST_ACTION_REFRESH_FINISHED));
        LocalBroadcastManager.getInstance(this).registerReceiver(new ResponseReceiver(), 
        		new IntentFilter(Constants.BROADCAST_ACTION_NETWORK_PROBLEM));
        
        // Initialize the Loader.
        getSupportLoaderManager().initLoader(LOADER_ID, null, this);
    }
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main_theme_dark, menu);
		mMenuRefresh = menu.findItem(R.id.refresh);
		
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
	    switch (menuItem.getItemId()) {
	    case android.R.id.home:
	        // Press home button on the ActionBar equals to Back button.
	        onBackPressed();
	        return true;
	    case R.id.add:
	    	Intent intent1 = new Intent(this, AddChannelActivity.class);
	    	startActivity(intent1);
	    	break;
	    case R.id.menu:
	    	Intent intent2 = new Intent(this, PreferenceActivity.class);
	    	startActivity(intent2);
	    	break;
	    case R.id.refresh:
	    	mMenuRefresh.setActionView(R.layout.progress_menu_item);
	    	startService(new Intent(this, FetcherService.class)
	    			.setAction(FetcherService.ACTION_REFRESH_FEEDS));
	    	break;
	    }
	    
	  return (super.onOptionsItemSelected(menuItem));
	}
	
	@Override
	public void onBackPressed() {
		
		// Press the back button twice to exit the APP.
		if (getSupportFragmentManager().getBackStackEntryCount() == 0) { 
			
	        if((System.currentTimeMillis() - mPrevBackTime) > 2000) {  
	            Toast.makeText(getApplicationContext(), mHintPressBackKey, Toast.LENGTH_SHORT).show();                                
	            mPrevBackTime = System.currentTimeMillis();
	        } else {
	            finish();
	            System.exit(0);
	        }
	    }
		else {
			super.onBackPressed();
		}
		
	    if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
	   	    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
	    	getSupportActionBar().setHomeButtonEnabled(false);
	    }
	}
	

	/**
	 * Called when the a RSS channel have been selected.
	 */
	@Override
	public void onChannelSelected(int position) {
		// Build the URI string.
    	Uri uri;
    	if (position == 0) {
    		uri = EntryColumns.ALL_ENTRIES_CONTENT_URI;
    	} else if (position == 1) {
    		uri = EntryColumns.FAVORITES_CONTENT_URI;
    	} else {
    		long id = mChannelListFragment.getItemId(position);
    		if (mChannelListFragment.isItemAGroup(position)) {
    			uri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(id);
    		} else {
    			uri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(id);
    		}
    	}        	

    	// Show the RSS entries.
    	if (mIsDualPane) { // If use dual pane layout, show the RSS entry list in a fragment.
        	if (getSupportFragmentManager().findFragmentByTag(Constants.RSS_ITEM_LIST_FRAGMENT)    
        			== null) {
        		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        		ft.add(R.id.rss_item_list, mRssItemListFragment, Constants.RSS_ITEM_LIST_FRAGMENT);
        		ft.commit();
        	}
        	mRssItemListFragment.beginLoadData(uri);
        } else {	// If use single pane layout, launch the RssItemListActivity.
            Intent intent = new Intent(this, RssItemListActivity.class);
            intent.putExtra(RssItemListActivity.EXTRA_URI, uri.toString());
            startActivity(intent);
        }
	}
	
	/** Select a RSS channel or group add display on the right pane if the current layout
	 * is dual pane. */
	private void setChannelSelected(int position) {
		if (!mIsDualPane)	return;
		
		// Build the URI string.
    	Uri uri;
    	if (position == 0) {
    		uri = EntryColumns.ALL_ENTRIES_CONTENT_URI;
    	} else if (position == 1) {
    		uri = EntryColumns.FAVORITES_CONTENT_URI;
    	} else {
    		long id = mChannelListFragment.getItemId(position);
    		if (mChannelListFragment.isItemAGroup(position)) {
    			uri = EntryColumns.ENTRIES_FOR_GROUP_CONTENT_URI(id);
    		} else {
    			uri = EntryColumns.ENTRIES_FOR_FEED_CONTENT_URI(id);
    		}
    	}        	

        if (getSupportFragmentManager().findFragmentByTag(Constants.RSS_ITEM_LIST_FRAGMENT)    
        		== null) {
        	FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        	ft.add(R.id.rss_item_list, mRssItemListFragment, Constants.RSS_ITEM_LIST_FRAGMENT);
        	ft.commit();
        }
        mRssItemListFragment.beginLoadData(uri);
	}
	
	@Override
	public void onChannelLongClicked(int position) {
		if (position == 0 || position == 1) {	// Press on "all_entries" or "favorite" won't react.
			return;
		}
		mActionModeEditChannel = startActionMode(new ActionModeEditChannel());	
	}

	@Override
	public void onRssItemSelected(int index) {
        // Get extras.
        Intent intent = new Intent(this, RssArticleActivity.class);
        ArrayList<Long> entryIdList = mRssItemListFragment.getRssEntryIds();
        long[] entryIds = new long[entryIdList.size()];
        for (int i = 0; i < entryIdList.size(); i++) {
        	entryIds[i] = entryIdList.get(i);
        }
        intent.putExtra(RssArticleActivity.EXTRA_ENTRY_IDS, entryIds);
        intent.putExtra(RssArticleActivity.EXTRA_INIT_POS, index);
        
		// Set the entry item as read.
		ContentValues value = new ContentValues();
		value.put(EntryColumns.IS_READ, true);
		MainApplication.getContext().getContentResolver().update(
				EntryColumns.CONTENT_URI(entryIds[index]), value, null , null);
		
		// Start the RssArticleActivity.
        startActivity(intent); 
	}

	@Override
	public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
		mChannelListFragment.setCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursorLoader) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
		
		// Load the RSS channel list.
		CursorLoader cursorLoader = new CursorLoader(this, FeedColumns.GROUPED_FEEDS_CONTENT_URI, 
				new String[]{FeedColumns._ID, FeedColumns.URL, FeedColumns.NAME,
	            FeedColumns.IS_GROUP, FeedColumns.GROUP_ID, FeedColumns.ICON, 
	            FeedColumns.LAST_UPDATE, FeedColumns.ERROR, FeedData.FEED_UNREAD_NUMBER},
	            PrefUtils.getBoolean(PrefUtils.SHOW_READ, true) ? "" : FeedData.WHERE_UNREAD_ONLY, 
	            		null, null);
	   
		cursorLoader.setUpdateThrottle(Constants.UPDATE_THROTTLE_DELAY);
	    
	    return cursorLoader;
	}
	
	/** ActionMode for editing RSS channels. */
	class ActionModeEditChannel implements ActionMode.Callback {

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			if (PrefUtils.isLightTheme()) {
				getSupportMenuInflater().inflate(R.menu.action_edit_channel_theme_light, menu);
			} else {
				getSupportMenuInflater().inflate(R.menu.action_edit_channel_theme_dark, menu);
			}
			return true;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		    switch (item.getItemId()) {
		    case R.id.menu_edit:
		    	Intent intent1 = new Intent(MainActivity.this, EditChannelActivity.class);
		    	intent1.putExtra(EditChannelActivity.EXTRA_FEED_ID, 
		    			mChannelListFragment.getCheckedItemId());
		    	startActivity(intent1);
		        return true;
		    case R.id.menu_add:
		    	Intent intent2 = new Intent(MainActivity.this, AddChannelActivity.class);
		    	startActivity(intent2);
		    	break;
		    case R.id.menu_delete:
		    	new AsyncTask<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
				    	mChannelListFragment.deleteCheckedItem();
						return null;
					}
					
					@Override
					protected void onPostExecute(Void result) {
				    	mChannelListFragment.notifyDataSetChanged();
					}
		    		
		    	}.execute();
		    	mActionModeEditChannel.finish();
		    	break;
		    }
			return false;
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {	
		}
	}
	
	/** Broadcast receiver for receiving messages from the FetcherService. */
	private class ResponseReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.BROADCAST_ACTION_REFRESH_FINISHED)) {
				
				if (mMenuRefresh != null)	mMenuRefresh.setActionView(null);
				
			} else if (intent.getAction().equals(Constants.BROADCAST_ACTION_NETWORK_PROBLEM)) {
				Toast.makeText(context, getResources().getString(
						R.string.error_network_access_problem), Toast.LENGTH_LONG).show();
				
				if (mMenuRefresh != null)	mMenuRefresh.setActionView(null);
			}
		}
	}
}


