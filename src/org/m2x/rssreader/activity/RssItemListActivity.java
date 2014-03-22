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
import org.m2x.rssreader.fragment.RssItemListFragment;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.service.FetcherService;
import org.m2x.rssreader.util.UiUtils;
import org.m2x.rssreader.R;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;


/**
 * The RSS item list Activity. Only work if the layout has single pane, 
 * otherwise will reuse the RssItenListFragment in the MainActivity.
 * @author teejoe (mtjm2x@gmail.com)
 *
 */
public class RssItemListActivity extends SherlockFragmentActivity implements
		RssItemListFragment.OnRssItemSelectedListener {
	
	public static final String EXTRA_URI = "URI";
	
	private RssItemListFragment mFragment; 
    
	private MenuItem mMenuRefresh;
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // If we are in two-pane layout mode, this activity is no longer necessary
        if (getResources().getBoolean(R.bool.has_two_panes)) {
            finish();
            return;
        }

        // Place an RssItemListFragment as our content pane
        mFragment = (RssItemListFragment)getSupportFragmentManager()
        		.findFragmentByTag(Constants.RSS_ITEM_LIST_FRAGMENT);
        if (mFragment == null){
        	mFragment = new RssItemListFragment();
        	getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFragment, 
        			Constants.RSS_ITEM_LIST_FRAGMENT).commit();
        }
        mFragment.setRssItemSelectedListener(this);
        
        // Register the BroadcastReceiver to messages from FetthService.
        LocalBroadcastManager.getInstance(this).registerReceiver(new ResponseReceiver(), 
        		new IntentFilter(Constants.BROADCAST_ACTION_REFRESH_FINISHED));
        LocalBroadcastManager.getInstance(this).registerReceiver(new ResponseReceiver(), 
        		new IntentFilter(Constants.BROADCAST_ACTION_NETWORK_PROBLEM));
    	
    	// Begin to load list data.
    	Uri uri = Uri.parse(getIntent().getStringExtra(EXTRA_URI));
    	mFragment.beginLoadData(uri);
	}

	@Override
	public void onRssItemSelected(int index) {
        // Get extras.
        Intent intent = new Intent(this, RssArticleActivity.class);
        ArrayList<Long> entryIdList = mFragment.getRssEntryIds();
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
    public boolean onCreateOptionsMenu(Menu menu) {
		// Create the options menu by xml resource.
		getSupportMenuInflater().inflate(R.menu.main_theme_dark, menu);
		mMenuRefresh = menu.findItem(R.id.refresh);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
	    switch (menuItem.getItemId()) {
	    case android.R.id.home:
	        // Press home button on the ActionBar equals to Back button.
	        finish();
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
	
	/** Broadcast receiver for receiving messages from the FetcherService. */
	private class ResponseReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.BROADCAST_ACTION_REFRESH_FINISHED)) {
				if (mMenuRefresh != null) {
					mMenuRefresh.setActionView(null);
				}
				// Begin to reload list data.
		    	Uri uri = Uri.parse(getIntent().getStringExtra(EXTRA_URI));
		    	mFragment.beginLoadData(uri);
			} else if (intent.getAction().equals(Constants.BROADCAST_ACTION_NETWORK_PROBLEM)) {
				if (mMenuRefresh != null) {
					mMenuRefresh.setActionView(null);
				}
				
				Toast.makeText(context, getResources().getString(
						R.string.error_network_access_problem), Toast.LENGTH_LONG).show();
			}
			
		}
	}
}
