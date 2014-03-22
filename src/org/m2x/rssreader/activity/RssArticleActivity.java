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
import org.m2x.rssreader.adapter.RssArticlePagerAdapter;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.util.UiUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

/**
 * The RSS article Activity. Shows the RSS articles.
 * @author teejoe (mtjm2x@gmail.com)
 *
 */
public class RssArticleActivity extends SherlockFragmentActivity 
		implements ViewPager.OnPageChangeListener {
	
	private static final String TAG = "RssArticleActivity";

	public static final String EXTRA_ENTRY_IDS = "entry_ids";
	public static final String EXTRA_INIT_POS = "initial_pos";
	
	private long[] mEntryIds;	// The list of RSS entry IDs.
		
	private ViewPager mPager;	// The ViewPager to show RSS articles.
	
	private RssArticlePagerAdapter mAdapter;	// Adapter for the ViewPager.
	
	private MenuItem mMenuStar;		// The star menu, used to change the icon of action star.
	
	private int mInitPos;	// The initial position of the ViewPager.

	@Override
    protected void onCreate(Bundle savedInstanceState) {
		UiUtils.setPreferenceTheme(this);
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_rss_article);
        
        setSupportProgressBarIndeterminateVisibility(true);
        
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        // Get the RSS entry ids.
        mPager = (ViewPager) findViewById(R.id.pager);
        mEntryIds = getIntent().getLongArrayExtra(EXTRA_ENTRY_IDS);
        if (mEntryIds == null) {
        	Log.e(TAG, "Entry id extra is null!");
        }
        
        // Set the adapter.
        mAdapter = new RssArticlePagerAdapter(getSupportFragmentManager(), mEntryIds);
        mPager.setAdapter(mAdapter);
        mPager.setOnPageChangeListener(this);

        // Get the initial position of pager.
        mInitPos = getIntent().getIntExtra(EXTRA_INIT_POS, -1);
        if (mInitPos != -1) {
        	mPager.setCurrentItem(mInitPos);
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.article_activity_theme_dark, menu);
		
		// Set the star icon of the action bar.
		mMenuStar = menu.findItem(R.id.star);
		if (isEntryStared(mEntryIds[mInitPos])) {
			mMenuStar.setIcon(R.drawable.ic_action_important);
		} else {
			mMenuStar.setIcon(R.drawable.ic_action_not_important);
		}
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
	    switch (menuItem.getItemId()) {
	    case android.R.id.home:
	        finish();
	        break;
	    case R.id.star:
	    	int position = mPager.getCurrentItem();
	    	boolean stared = isEntryStared(mEntryIds[position]);
	    	ContentValues values = new ContentValues();
	    	values.put(EntryColumns.IS_FAVORITE, !stared);
	    	MainApplication.getContext().getContentResolver().update(
	    			EntryColumns.CONTENT_URI(mEntryIds[position]), values, null, null);
	    	mMenuStar.setIcon(stared? R.drawable.ic_action_not_important: 
	    			R.drawable.ic_action_important);
	    	break;
	    case R.id.menu:
	    	Intent intent = new Intent(this, PreferenceActivity.class);
	    	startActivity(intent);
	    	break;
	    default:		
	        return true;
	    }
	    
	    return (super.onOptionsItemSelected(menuItem));
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		return;
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
		return;
	}

	@Override
	public void onPageSelected(int position) {	
		ContentResolver cr = MainApplication.getContext().getContentResolver();
		
		// Set the RSS entry as read.
		ContentValues value = new ContentValues();
		value.put(EntryColumns.IS_READ, true);
		cr.update(EntryColumns.CONTENT_URI(mEntryIds[position]), value, null , null);
		
		// Update the star icon.
		if (mMenuStar != null) {	// mMenuStar could be null when OptionsMenu has not created yet.
			if (isEntryStared(mEntryIds[position])) {
				mMenuStar.setIcon(R.drawable.ic_action_important);
			} else {
				mMenuStar.setIcon(R.drawable.ic_action_not_important);
			}
		}

	}
	
	/** Tell if the entry is stared by its ID. */
	private boolean isEntryStared(long entryId) {
		ContentResolver cr = MainApplication.getContext().getContentResolver();
		Cursor cursor = cr.query(EntryColumns.CONTENT_URI(entryId), 
				new String[]{EntryColumns.IS_FAVORITE}, null, null, null);
		
		if (cursor.moveToFirst() && 
				cursor.getInt(cursor.getColumnIndex(EntryColumns.IS_FAVORITE)) == 1) {
			cursor.close();
			return true;
		}
			
		return false;
	}
}
