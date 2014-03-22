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

import java.lang.ref.WeakReference;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.R;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.view.RssArticleView;

/**
 * The fragment to show a RSS article.
 * @author teejoe (mtjm2x@gmail.com)
 *
 */
public class RssArticleFragment extends SherlockFragment implements
		RssArticleView.OnActionListener {
	
	private static final String EXTRA_ENTRY_ID = "extra_entry_id";
	
	private static final int MESSAGE_LOAD_BEGIN = 0x0001;
	private static final int MESSAGE_LOAD_FINISH = 0x0002;
	
	private long mEntryId;
	
	private Cursor mCursor;
	
	private RssArticleView mRssArticleView;
			
	/** Handler to update progress bar state. */
	static class ProgressBarHandler extends Handler {
		WeakReference<SherlockFragmentActivity> mActivityRef = null;
		
		ProgressBarHandler(SherlockFragmentActivity activity) {
			mActivityRef = new WeakReference<SherlockFragmentActivity>(activity);
		}
		
		public void handlerMessage(Message msg) {
    		if (msg.what == MESSAGE_LOAD_BEGIN) {
    			SherlockFragmentActivity activity = mActivityRef.get();
    			if (activity != null) {
    				activity.setSupportProgressBarIndeterminateVisibility(true);
    			}
    		} else if (msg.what == MESSAGE_LOAD_FINISH) {
    			SherlockFragmentActivity activity = mActivityRef.get();
    			if (activity != null) {
    				activity.setSupportProgressBarIndeterminateVisibility(false);
    			}
    		}
		}
	};
	
	public static RssArticleFragment newInstance(long feedId) {
	    final RssArticleFragment fragment = new RssArticleFragment();
		
	    final Bundle args = new Bundle();
	    args.putLong(EXTRA_ENTRY_ID, feedId);
	    fragment.setArguments(args);
	
	    return fragment;
	}
	
	public RssArticleFragment() {}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEntryId = getArguments() != null ? getArguments().getLong(EXTRA_ENTRY_ID) : null;
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, 
			Bundle savedInstanceState){
		View rootView = inflater.inflate(R.layout.fragment_rss_article, container, false);
				
		mRssArticleView = (RssArticleView) rootView.findViewById(R.id.article_view);
		mRssArticleView.setListener(this);
		
		return rootView;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
	
		beginLoadData();
	}

	private void beginLoadData() {
		new LoadArticleTask().execute(mEntryId);
	}
	
	/** Load the RSS feed information by a given feed ID. */
	class LoadArticleTask extends AsyncTask<Long, Integer, Cursor> {
		
		@Override
		protected Cursor doInBackground(Long... params) {
			mCursor = MainApplication.getContext().getContentResolver().query(
				    EntryColumns.CONTENT_URI(params[0]), null, null, null, null);
			
			return mCursor;
		}		
		
		@Override
		protected void onPostExecute(Cursor cursor){
			if (cursor.moveToFirst()) {
				String title = cursor.getString(cursor.getColumnIndex(EntryColumns.TITLE));
				String link = cursor.getString(cursor.getColumnIndex(EntryColumns.LINK));
				long timestamp = cursor.getLong(cursor.getColumnIndex(EntryColumns.DATE));
				String author = cursor.getString(cursor.getColumnIndex(EntryColumns.AUTHOR));
				String content = cursor.getString(cursor.getColumnIndex(EntryColumns.ABSTRACT));
			
				mRssArticleView.setHtml(mEntryId, title, link, content, author, 
						timestamp, false);	
				
				setProgressBarVisibility(false);
			}
		}
	}
	
	/** Set the visibility of ProgressBar on the ActionBar. */
	private void setProgressBarVisibility(boolean visible) {
		SherlockFragmentActivity activity = (SherlockFragmentActivity)getActivity();
		if (activity == null) {
			return;
		} else {
			activity.setSupportProgressBarIndeterminateVisibility(visible);
		}
	}

	@Override
	public void onClickComment() {
		if (mCursor == null || !mCursor.moveToFirst()) {
			return;
		}
		
		String commentsLink = mCursor.getString(mCursor.getColumnIndex(EntryColumns.COMMENTS));
		if (commentsLink == null || commentsLink.equals("")) {
			Toast.makeText(MainApplication.getContext(), MainApplication.getContext()
					.getResources().getString(R.string.no_comments_link), Toast.LENGTH_SHORT)
					.show();
		} else {
			try {
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(commentsLink));
				startActivity(browserIntent);	
			} catch (ActivityNotFoundException e) {
				Toast.makeText(MainApplication.getContext(), MainApplication.getContext()
						.getResources().getString(R.string.open_comments_fail), Toast.LENGTH_SHORT)
						.show();
				e.printStackTrace();
			}
		};
	}
}
