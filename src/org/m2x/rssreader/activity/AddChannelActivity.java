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

import java.io.File;
import java.io.FilenameFilter;

import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.util.OPML;
import org.m2x.rssreader.util.UiUtils;
import org.m2x.rssreader.Constants;
import org.m2x.rssreader.R;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;


/**
 * The Activity to add RSS channels. 
 * @author teejoe(mtjm2x@gmail.com)
 *
 */
public class AddChannelActivity extends SherlockActivity implements
		View.OnClickListener{
	
	private EditText mFeedTitle;
	private EditText mFeedUrl;	
	
	private Button mImportFromOpml;
	private Button mExportToOpml;

	@Override
	public void onCreate(Bundle savedInstanceState){
		UiUtils.setPreferenceTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_channel);
		
		mFeedTitle = (EditText) findViewById(R.id.edit_feed_title);
		mFeedUrl = (EditText) findViewById(R.id.edit_feed_url);
		mImportFromOpml = (Button) findViewById(R.id.import_from_opml);
		mExportToOpml = (Button) findViewById(R.id.export_to_opml);
		
		mImportFromOpml.setOnClickListener(this);
		mExportToOpml.setOnClickListener(this);
		findViewById(R.id.btn_cancel).setOnClickListener(this);
		findViewById(R.id.btn_ok).setOnClickListener(this);

		
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
		case R.id.import_from_opml:
			importFromOpml();
			break;
		case R.id.export_to_opml:
			exportToOpml();
			break;
		case R.id.btn_cancel:
			finish();
			break;
		case R.id.btn_ok:
			addRssChannel();
			finish();
			break;
		}
	}
	
	/** Add a RSS channel by a given URL and title. */
	private void addRssChannel() {
        String url = mFeedUrl.getText().toString();
        if (url.equals(""))	return;

        // Add HTTP scheme if not set.
        if (!url.startsWith(Constants.HTTP_SCHEME) && !url.startsWith(Constants.HTTPS_SCHEME)) {
            url = Constants.HTTP_SCHEME + url;
        }

        // Check if the channel exists, if not exist, insert it into the database.
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(FeedColumns.CONTENT_URI, null, FeedColumns.URL + 
        		Constants.DB_ARG, new String[]{url}, null);
        if (cursor.moveToFirst()) {
            cursor.close();
            Toast.makeText(this, R.string.error_feed_url_exists, Toast.LENGTH_LONG).show();
        } else {
            cursor.close();
            ContentValues values = new ContentValues();

            values.put(FeedColumns.URL, url);
            values.putNull(FeedColumns.ERROR);

            String name = mFeedTitle.getText().toString();

            if (name.trim().length() > 0) {
                values.put(FeedColumns.NAME, name);
            }
            cr.insert(FeedColumns.CONTENT_URI, values);
        }
	}
	
	/** Import RSS channels from a OPML file. */
	private void importFromOpml() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) 
				&& !Environment.getExternalStorageState()
				.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {

			Toast.makeText(this, getString(R.string.storage_can_not_access), Toast.LENGTH_LONG).show();
			return;
		}
		
		// Show a file choose dialog to pick up the OPML file.
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.choose_opml_file);
		
		try {
			final String[] fileNames = Environment.getExternalStorageDirectory().list(
					new FilenameFilter() {
				public boolean accept(File dir, String filename) {
					return new File(dir, filename).isFile();
				}
			});
			
			builder.setItems(fileNames, new DialogInterface.OnClickListener()  {
				public void onClick(DialogInterface dialog, int which) {
					try {
						OPML.importFromFile(new StringBuilder(Environment
								.getExternalStorageDirectory().toString()).append(File.separator)
								.append(fileNames[which]).toString());
					} catch (Exception e) {
						Toast.makeText(AddChannelActivity.this, getString(R.string.error), 
								Toast.LENGTH_LONG).show();
					}
					finish();
				}
			});
			builder.show();
		} catch (Exception e) {
			Toast.makeText(this, getString(R.string.error), Toast.LENGTH_LONG).show();
		}
	}
	
	/** Export the current RSS channels to a default OPML file. */
	private void exportToOpml() {
		if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) 
				||Environment.getExternalStorageState()
				.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			try {
				String filename = new StringBuilder(Environment.getExternalStorageDirectory()
						.toString()).append("/rssreader_backup").append(".opml").toString();
				
				OPML.exportToFile(filename);
				Toast.makeText(this, getString(R.string.opml_export_to) + filename, 
						Toast.LENGTH_LONG).show();
			} catch (Exception e) {
				Toast.makeText(this, getString(R.string.error), Toast.LENGTH_LONG).show();
			}
		} else {
			Toast.makeText(this, getString(R.string.storage_can_not_access), Toast.LENGTH_LONG)
					.show();
		}
	}
}
