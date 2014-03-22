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
 * Some parts of this software are based on "Sparse rss" under the MIT license (see
 * below). Please refers to the original project to identify which parts are under the
 * MIT license.
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 *     Permission is hereby granted, free of charge, to any person obtaining a copy
 *     of this software and associated documentation files (the "Software"), to deal
 *     in the Software without restriction, including without limitation the rights
 *     to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *     copies of the Software, and to permit persons to whom the Software is
 *     furnished to do so, subject to the following conditions:
 *
 *     The above copyright notice and this permission notice shall be included in
 *     all copies or substantial portions of the Software.
 *
 *     THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *     IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *     FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *     AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *     LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *     OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *     THE SOFTWARE.
 */

package org.m2x.rssreader.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Xml;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.m2x.rssreader.Constants;
import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.activity.MainActivity;
import org.m2x.rssreader.provider.FeedData;
import org.m2x.rssreader.provider.FeedData.EntryColumns;
import org.m2x.rssreader.provider.FeedData.FeedColumns;
import org.m2x.rssreader.provider.FeedData.TaskColumns;
import org.m2x.rssreader.util.NetworkUtils;
import org.m2x.rssreader.util.PrefUtils;
import org.m2x.rssreader.util.RssAtomParser;

import org.m2x.rssreader.R;

public class FetcherService extends IntentService {

    public static final String ACTION_REFRESH_FEEDS = "org.m2x.rssreader.REFRESH";
    public static final String ACTION_DOWNLOAD_IMAGES = "org.m2x.rssreader.DOWNLOAD_IMAGES";

    private static final int THREAD_NUMBER = 3;
    private static final int MAX_TASK_ATTEMPT = 3;

    private static final int FETCHMODE_DIRECT = 1;
    private static final int FETCHMODE_REENCODE = 2;

    private static final String CHARSET = "charset=";
    private static final String COUNT = "COUNT(*)";
    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";
    private static final String HREF = "href=\"";

    private static final String HTML_BODY = "<body";
    private static final String ENCODING = "encoding=\"";
    private static final String SERVICENAME = "RssFetcherService";
    
    /* Allow different positions of the "rel" attribute w.r.t. the "href" attribute */
    private static final Pattern FEED_LINK_PATTERN = Pattern.compile(
            "[.]*<link[^>]* ((rel=alternate|rel=\"alternate\")[^>]* href=\"[^\"]*\"|href=\"[^\"]*\"[^>]* (rel=alternate|rel=\"alternate\"))[^>]*>",
            Pattern.CASE_INSENSITIVE);

    public FetcherService() {
        super(SERVICENAME);
        HttpURLConnection.setFollowRedirects(true);
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(
        		Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        // Connectivity issue, we quit
        if (networkInfo == null || networkInfo.getState() != NetworkInfo.State.CONNECTED) {
        	broadcastNetworkProblem();
            return;
        }

        boolean isFromAutoRefresh = intent.getBooleanExtra(Constants.FROM_AUTO_REFRESH, false);
        boolean skipFetch = isFromAutoRefresh && PrefUtils.getBoolean(
        		PrefUtils.AUTO_REFRESH_ONLY_ON_WIFI, false)
                && networkInfo.getType() != ConnectivityManager.TYPE_WIFI;
        // We need to skip the fetching process, so we quit
        if (skipFetch) {
            return;
        }

        if (ACTION_DOWNLOAD_IMAGES.equals(intent.getAction())) {
            downloadAllImages();
        } else { // == Constants.ACTION_REFRESH_FEEDS
            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, true);

            if (isFromAutoRefresh) {
                PrefUtils.putLong(PrefUtils.LAST_SCHEDULED_REFRESH, SystemClock.elapsedRealtime());
            }

            String feedId = intent.getStringExtra(Constants.FEED_ID);
            int newCount = (feedId == null ? refreshFeeds() : refreshFeed(feedId));

            if (newCount > 0) {
                if (PrefUtils.getBoolean(PrefUtils.PUSH_NOTIFICATION, true)) {
                    Cursor cursor = getContentResolver().query(
                    		EntryColumns.CONTENT_URI, new String[]{COUNT}, 
                    		EntryColumns.WHERE_UNREAD, null, null);

                    cursor.moveToFirst();
                    newCount = cursor.getInt(0); // The number has possibly changed
                    cursor.close();

                    if (newCount > 0) {
                        String text = getResources().getQuantityString(
                        		R.plurals.number_of_new_entries, newCount, newCount);

                        Intent notificationIntent = new Intent(
                        		FetcherService.this, MainActivity.class);
                        PendingIntent contentIntent = PendingIntent.getActivity(
                        		FetcherService.this, 0, notificationIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(
                        		MainApplication.getContext())
                                .setContentIntent(contentIntent)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher)) //
                                .setTicker(text)
                                .setWhen(System.currentTimeMillis())
                                .setAutoCancel(true)
                                .setContentTitle(getString(R.string.rssreader_feeds))
                                .setContentText(text)
                                .setLights(0xffffffff, 300, 1000);

                        if (PrefUtils.getBoolean(PrefUtils.NOTIFICATIONS_VIBRATE, false)) {
                            notifBuilder.setVibrate(new long[]{0, 1000});
                        }

                        String ringtone = PrefUtils.getString(PrefUtils.NOTIFICATIONS_RINGTONE, null);
                        if (ringtone != null && ringtone.length() > 0) {
                            notifBuilder.setSound(Uri.parse(ringtone));
                        }

                        if (Constants.NOTIF_MGR != null) {
                            Constants.NOTIF_MGR.notify(0, notifBuilder.getNotification());
                        }
                    }
                } else if (Constants.NOTIF_MGR != null) {
                    Constants.NOTIF_MGR.cancel(0);
                }
            }

            downloadAllImages();

            PrefUtils.putBoolean(PrefUtils.IS_REFRESHING, false);
            broadcastRefreshFinished();
        }
    }

    public static void addImagesToDownload(String entryId, ArrayList<String> images) {
        if (images != null && !images.isEmpty()) {
            ContentValues[] values = new ContentValues[images.size()];
            for (int i = 0; i < images.size(); i++) {
                values[i] = new ContentValues();
                values[i].put(TaskColumns.ENTRY_ID, entryId);
                values[i].put(TaskColumns.IMG_URL_TO_DL, images.get(i));
            }

            MainApplication.getContext().getContentResolver().bulkInsert(
            		TaskColumns.CONTENT_URI, values);
        }
    }

    private void downloadAllImages() {
        ContentResolver cr = MainApplication.getContext().getContentResolver();
        Cursor cursor = cr.query(TaskColumns.CONTENT_URI,
        		new String[]{TaskColumns._ID, 
        		TaskColumns.ENTRY_ID, 
        		TaskColumns.IMG_URL_TO_DL,
                TaskColumns.NUMBER_ATTEMPT}, 
                TaskColumns.IMG_URL_TO_DL + Constants.DB_IS_NOT_NULL, null, null);

        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

        while (cursor.moveToNext()) {
            long taskId = cursor.getLong(0);
            long entryId = cursor.getLong(1);
            String imgPath = cursor.getString(2);
            int nbAttempt = 0;
            if (!cursor.isNull(3)) {
                nbAttempt = cursor.getInt(3);
            }

            try {
                NetworkUtils.downloadImage(entryId, imgPath);

                // If we are here, everything is OK
                operations.add(ContentProviderOperation.newDelete(TaskColumns.CONTENT_URI(taskId))
                		.build());
                cr.notifyChange(EntryColumns.CONTENT_URI(entryId), null); // To refresh the view
            } catch (Exception e) {
                if (nbAttempt + 1 > MAX_TASK_ATTEMPT) {
                    operations.add(ContentProviderOperation.newDelete(
                    		TaskColumns.CONTENT_URI(taskId)).build());
                } else {
                    ContentValues values = new ContentValues();
                    values.put(TaskColumns.NUMBER_ATTEMPT, nbAttempt + 1);
                    operations.add(ContentProviderOperation.newUpdate(
                    		TaskColumns.CONTENT_URI(taskId)).withValues(values).build());
                }
            }
        }

        cursor.close();

        if (!operations.isEmpty()) {
            try {
                cr.applyBatch(FeedData.AUTHORITY, operations);
            } catch (Throwable ignored) {
            }
        }
    }

    private int refreshFeeds() {
        ContentResolver cr = getContentResolver();
        final Cursor cursor = cr.query(FeedColumns.CONTENT_URI, FeedColumns.PROJECTION_ID, 
        		null, null, null);
        int nbFeed = cursor.getCount();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_NUMBER, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setPriority(Thread.MIN_PRIORITY);
                return t;
            }
        });

        CompletionService<Integer> completionService = new ExecutorCompletionService<Integer>(executor);
        while (cursor.moveToNext()) {
            final String feedId = cursor.getString(0);
            completionService.submit(new Callable<Integer>() {
                @Override
                public Integer call() {
                    int result = 0;
                    try {
                        result = refreshFeed(feedId);
                    } catch (Exception ignored) {
                    }
                    return result;
                }
            });
        }
        cursor.close();

        int globalResult = 0;
        for (int i = 0; i < nbFeed; i++) {
            try {
                Future<Integer> f = completionService.take();
                globalResult += f.get();
            } catch (Exception ignored) {
            }
        }

        executor.shutdownNow(); // To purge all threads

        return globalResult;
    }

    private int refreshFeed(String feedId) {
        RssAtomParser handler = null;

        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(FeedColumns.CONTENT_URI(feedId), null, null, null, null);

        if (!cursor.moveToFirst()) {
        	cursor.close();
        	return 0;
        }
         
    	int urlPosition = cursor.getColumnIndex(FeedColumns.URL);
        int titlePosition = cursor.getColumnIndex(FeedColumns.NAME);
        int fetchmodePosition = cursor.getColumnIndex(FeedColumns.FETCH_MODE);
        int realLastUpdatePosition = cursor.getColumnIndex(FeedColumns.REAL_LAST_UPDATE);
        int iconPosition = cursor.getColumnIndex(FeedColumns.ICON);
        int retrieveFullTextPosition = cursor.getColumnIndex(FeedColumns.RETRIEVE_FULLTEXT);

        HttpURLConnection connection = null;
        try {
            String feedUrl = cursor.getString(urlPosition);
            connection = NetworkUtils.setupConnection(feedUrl);
            String contentType = connection.getContentType();
            int fetchMode = cursor.getInt(fetchmodePosition);

            handler = new RssAtomParser(new Date(cursor.getLong(realLastUpdatePosition)), 
            				feedId, cursor.getString(titlePosition), feedUrl,
            				cursor.getInt(retrieveFullTextPosition) == 1);
            handler.setFetchImages(PrefUtils.getBoolean(PrefUtils.FETCH_PICTURES, true));

            if (fetchMode == 0) {
                if (contentType != null && contentType.startsWith(CONTENT_TYPE_TEXT_HTML)) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                    		NetworkUtils.getConnectionInputStream(connection)));

                    String line;
                    int posStart = -1;

                    while ((line = reader.readLine()) != null) {
                        if (line.contains(HTML_BODY)) {
                            break;
                        } else {
                            Matcher matcher = FEED_LINK_PATTERN.matcher(line);

                            if (matcher.find()) { // not "while" as only one link is needed
                                line = matcher.group();
                                posStart = line.indexOf(HREF);

                                if (posStart > -1) {
                                    String url = line.substring(posStart + 6, line.indexOf('"', 
                                    		posStart + 10)).replace("&amp", "&");

                                    ContentValues values = new ContentValues();

                                    if (url.startsWith("/")) {
                                        int index = feedUrl.indexOf('/', 8);

                                        if (index > -1) {
                                            url = feedUrl.substring(0, index) + url;
                                        } else {
                                            url = feedUrl + url;
                                        }
                                    } else if (!url.startsWith(Constants.HTTP_SCHEME)
                                    			&& !url.startsWith(Constants.HTTPS_SCHEME)) {
                                        url = feedUrl + '/' + url;
                                    }
                                    values.put(FeedColumns.URL, url);
                                    cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);
                                    connection.disconnect();
                                    connection = NetworkUtils.setupConnection(url);
                                    contentType = connection.getContentType();
                                    break;
                                }
                            }
                        }
                    }
                    // this indicates a badly configured feed
                    if (posStart == -1) {
                        connection.disconnect();
                        connection = NetworkUtils.setupConnection(feedUrl);
                        contentType = connection.getContentType();
                    }
                }

                if (contentType != null) {
                    int index = contentType.indexOf(CHARSET);

                    if (index > -1) {
                        int index2 = contentType.indexOf(';', index);

                        try {
                            Xml.findEncodingByName(index2 > -1 ? contentType.substring(
                            		index + 8, index2) : contentType.substring(index + 8));
                            fetchMode = FETCHMODE_DIRECT;
                        } catch (UnsupportedEncodingException usee) {
                            fetchMode = FETCHMODE_REENCODE;
                        }
                    } else {
                        fetchMode = FETCHMODE_REENCODE;
                    }

                } else {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                    		NetworkUtils.getConnectionInputStream(connection)));

                    char[] chars = new char[20];

                    int length = bufferedReader.read(chars);

                    String xmlDescription = new String(chars, 0, length);

                    connection.disconnect();
                    connection = NetworkUtils.setupConnection(connection.getURL());

                    int start = xmlDescription != null ? xmlDescription.indexOf(ENCODING) : -1;

                    if (start > -1) {
                        try {
                            Xml.findEncodingByName(xmlDescription.substring(start + 10, 
                            		xmlDescription.indexOf('"', start + 11)));
                            fetchMode = FETCHMODE_DIRECT;
                        } catch (UnsupportedEncodingException usee) {
                            fetchMode = FETCHMODE_REENCODE;
                        }
                    } else {
                        // absolutely no encoding information found
                        fetchMode = FETCHMODE_DIRECT;
                    }
                }

                ContentValues values = new ContentValues();
                values.put(FeedColumns.FETCH_MODE, fetchMode);
                cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);
            }

            switch (fetchMode) {
                default:
                case FETCHMODE_DIRECT: {
                    if (contentType != null) {
                        int index = contentType.indexOf(CHARSET);

                        int index2 = contentType.indexOf(';', index);

                        InputStream inputStream = NetworkUtils.getConnectionInputStream(connection);
                        Xml.parse(inputStream,
                                Xml.findEncodingByName(index2 > -1 ? contentType.substring(
                                index + 8, index2) : contentType.substring(index + 8)), handler);
                    } else {
                        InputStreamReader reader = new InputStreamReader(
                        		NetworkUtils.getConnectionInputStream(connection));
                        Xml.parse(reader, handler);
                    }
                    break;
                }
                case FETCHMODE_REENCODE: {
                    ByteArrayOutputStream ouputStream = new ByteArrayOutputStream();
                    InputStream inputStream = NetworkUtils.getConnectionInputStream(connection);

                    byte[] byteBuffer = new byte[4096];

                    int n;
                    while ((n = inputStream.read(byteBuffer)) > 0) {
                        ouputStream.write(byteBuffer, 0, n);
                    }

                    String xmlText = ouputStream.toString();

                    int start = xmlText != null ? xmlText.indexOf(ENCODING) : -1;

                    if (start > -1) {
                        Xml.parse(new StringReader(new String(ouputStream.toByteArray(),
                                xmlText.substring(start + 10, xmlText.indexOf('"', 
                                start + 11)))), handler);
                    } else {
                        // use content type
                        if (contentType != null) {
                            int index = contentType.indexOf(CHARSET);

                            if (index > -1) {
                                int index2 = contentType.indexOf(';', index);

                                try {
                                    StringReader reader = new StringReader(new String(
                                    		ouputStream.toByteArray(), index2 > -1 ? 
                                    		contentType.substring(index + 8, index2) 
                                    		: contentType.substring(index + 8)));
                                    Xml.parse(reader, handler);
                                } catch (Exception ignored) {
                                }
                            } else {
                                StringReader reader = new StringReader(new String(
                                		ouputStream.toByteArray()));
                                Xml.parse(reader, handler);
                            }
                        }
                    }
                    break;
                }
            }

            connection.disconnect();
        } catch (FileNotFoundException e) {
            if (handler == null || (handler != null && !handler.isDone() && !handler.isCancelled())) {
                ContentValues values = new ContentValues();

                // resets the fetchmode to determine it again later
                values.put(FeedColumns.FETCH_MODE, 0);

                values.put(FeedColumns.ERROR, getString(R.string.error_feed_error));
                cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);
            }
        } catch (Throwable e) {
            if (handler == null || (handler != null && !handler.isDone() && !handler.isCancelled())) {
                ContentValues values = new ContentValues();

                // resets the fetchmode to determine it again later
                values.put(FeedColumns.FETCH_MODE, 0);

                values.put(FeedColumns.ERROR, e.getMessage() != null ? e.getMessage() 
                		: getString(R.string.error_feed_process));
                cr.update(FeedColumns.CONTENT_URI(feedId), values, null, null);
            }
        } finally {

			/* check and optionally find favicon */
            try {
                if (handler != null && cursor.getBlob(iconPosition) == null) {
                    String feedLink = handler.getFeedLink();
                    if (feedLink != null) {
                        NetworkUtils.retrieveFavicon(this, new URL(feedLink), feedId);
                    } else {
                        NetworkUtils.retrieveFavicon(this, connection.getURL(), feedId);
                    }
                }
            } catch (Throwable ignored) {
            }

            if (connection != null) {
                connection.disconnect();
            }
        }
        
        cursor.close();

        return handler != null ? handler.getNewCount() : 0;
    }
    
    /** Broadcast refresh finished message. */
    public void broadcastRefreshFinished() {
    	Intent localIntent = new Intent(Constants.BROADCAST_ACTION_REFRESH_FINISHED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
    
    /** Broadcast network problem. */
    public void broadcastNetworkProblem() {
    	Intent localIntent = new Intent(Constants.BROADCAST_ACTION_NETWORK_PROBLEM);
    	LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }
}
