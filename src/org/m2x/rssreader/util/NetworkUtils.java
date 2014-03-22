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
 * Some parts of this software are based on "FeedEx" (see below)
 *
 * Copyright (c) 2012-2013 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.m2x.rssreader.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.Html;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.m2x.rssreader.Constants;
import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.provider.FeedData;

public class NetworkUtils {

    public static final File IMAGE_FOLDER_FILE = new File(MainApplication.getContext()
    		.getCacheDir(), "images/");
    
    public static final String IMAGE_FOLDER = IMAGE_FOLDER_FILE.getAbsolutePath() + '/';
    public static final String TEMP_PREFIX = "TEMP__";
    public static final String ID_SEPARATOR = "__";

    private static final String GZIP = "gzip";
    private static final String FILE_FAVICON = "/favicon.ico";
    private static final String PROTOCOL_SEPARATOR = "://";
    private static final String _HTTP = "http";
    private static final String _HTTPS = "https";

    private static class PictureFilenameFilter implements FilenameFilter {
        private static final String REGEX = "__[^\\.]*\\.[A-Za-z]*";

        private Pattern pattern;

        public PictureFilenameFilter() {
        }

        public void setEntryId(String entryId) {
            pattern = Pattern.compile(entryId + REGEX);
        }

        @Override
        public boolean accept(File dir, String filename) {
            return pattern.matcher(filename).find();
        }
    }

    public static String getDownloadedImagePath(long entryId, String imgUrl) {
        return IMAGE_FOLDER + entryId + ID_SEPARATOR + getMd5(imgUrl);
    }

    public static String getTempDownloadedImagePath(long entryId, String imgUrl) {
        return IMAGE_FOLDER + TEMP_PREFIX + entryId + ID_SEPARATOR + getMd5(imgUrl);
    }

    public static void downloadImage(long entryId, String imgUrl) throws IOException {
        String tempImgPath = getTempDownloadedImagePath(entryId, imgUrl);
        String finalImgPath = getDownloadedImagePath(entryId, imgUrl);

        if (!new File(tempImgPath).exists() && !new File(finalImgPath).exists()) {
            HttpURLConnection imgURLConnection = null;
            try {
                IMAGE_FOLDER_FILE.mkdir(); // create images dir

                // Compute the real URL (without "&eacute;", ...)
                String realUrl = Html.fromHtml(imgUrl).toString();
                imgURLConnection = setupConnection(realUrl);

                FileOutputStream fileOutput = new FileOutputStream(tempImgPath);
                InputStream inputStream = imgURLConnection.getInputStream();

                byte[] buffer = new byte[2048];
                int bufferLength;
                while ((bufferLength = inputStream.read(buffer)) > 0) {
                    fileOutput.write(buffer, 0, bufferLength);
                }
                fileOutput.flush();
                fileOutput.close();
                inputStream.close();

                new File(tempImgPath).renameTo(new File(finalImgPath));
            } catch (IOException e) {
                new File(tempImgPath).delete();
                throw e;
            } finally {
                if (imgURLConnection != null) {
                    imgURLConnection.disconnect();
                }
            }
        }
    }

    public static synchronized void deleteFeedImagesCache(Uri entriesUri, String selection) {
        if (IMAGE_FOLDER_FILE.exists()) {
            PictureFilenameFilter filenameFilter = new PictureFilenameFilter();

            Cursor cursor = MainApplication.getContext().getContentResolver().query(
            		entriesUri, FeedData.EntryColumns.PROJECTION_ID, selection, null, null);

            while (cursor.moveToNext()) {
                filenameFilter.setEntryId(cursor.getString(0));

                File[] files = IMAGE_FOLDER_FILE.listFiles(filenameFilter);
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            cursor.close();
        }
    }

    public static String getBaseUrl(String link) {
        String baseUrl = link;
        int index = link.indexOf('/', 8); // this also covers https://
        if (index > -1) {
            baseUrl = link.substring(0, index);
        }

        return baseUrl;
    }

    public static byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];

        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            output.write(buffer, 0, n);
        }

        byte[] result = output.toByteArray();

        output.close();
        inputStream.close();
        return result;
    }

    public static void retrieveFavicon(Context context, URL url, String id) {
        boolean success = false;
        HttpURLConnection iconURLConnection = null;

        try {
            iconURLConnection = setupConnection(new URL(url.getProtocol() + PROTOCOL_SEPARATOR 
            		+ url.getHost() + FILE_FAVICON));

            byte[] iconBytes = getBytes(getConnectionInputStream(iconURLConnection));
            if (iconBytes != null && iconBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(iconBytes, 0, iconBytes.length);
                if (bitmap != null && bitmap.getWidth() != 0 && bitmap.getHeight() != 0) {
                    ContentValues values = new ContentValues();
                    values.put(FeedData.FeedColumns.ICON, iconBytes);
                    context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), 
                    		values, null, null);
                    success = true;
                }
                bitmap.recycle();
            }
        } catch (Throwable ignored) {
        } finally {
            if (iconURLConnection != null) {
                iconURLConnection.disconnect();
            }
        }

        if (!success) {
            // no icon found or error
            ContentValues values = new ContentValues();
            values.putNull(FeedData.FeedColumns.ICON);
            context.getContentResolver().update(FeedData.FeedColumns.CONTENT_URI(id), values, 
            		null, null);
        }
    }

    public static HttpURLConnection setupConnection(String url) throws IOException {
        return setupConnection(new URL(url));
    }

    public static HttpURLConnection setupConnection(URL url) throws IOException {
        return setupConnection(url, 0);
    }

    public static HttpURLConnection setupConnection(URL url, int cycle) throws IOException {
        Proxy proxy = null;
        
        // Try to get the system proxy
        try {
            ProxySelector defaultProxySelector = ProxySelector.getDefault();
            List<Proxy> proxyList = defaultProxySelector.select(url.toURI());
            if (!proxyList.isEmpty()) {
                proxy = proxyList.get(0);
            }
        } catch (Throwable ignored) {
        }
 

        HttpURLConnection connection = proxy == null ? (HttpURLConnection) url.openConnection() 
        		: (HttpURLConnection) url.openConnection(proxy);

        connection.setDoInput(true);
        connection.setDoOutput(false);
        connection.setRequestProperty(
        		"User-agent", "Mozilla/5.0 (compatible) AppleWebKit Chrome Safari"); // some feeds need this to work properly
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setUseCaches(false);

        connection.setRequestProperty("accept", "*/*");
        connection.connect();

        String location = connection.getHeaderField("Location");

        if (location != null
                && (url.getProtocol().equals(_HTTP) && location.startsWith(Constants.HTTPS_SCHEME) 
                || url.getProtocol().equals(_HTTPS) && location.startsWith(Constants.HTTP_SCHEME))) {
            // if location != null, the system-automatic redirect has failed
            // which indicates a protocol change

            connection.disconnect();

            if (cycle < 5) {
                return setupConnection(new URL(location), cycle + 1);
            } else {
                throw new IOException("Too many redirects.");
            }
        }
        return connection;
    }

    /**
     * This is a small wrapper for getting the properly encoded inputstream if is is gzip compressed and not properly recognized.
     */
    public static InputStream getConnectionInputStream(HttpURLConnection connection) 
    		throws IOException {
        InputStream inputStream = connection.getInputStream();

        if (GZIP.equals(connection.getContentEncoding()) && 
        		!(inputStream instanceof GZIPInputStream)) {
            return new GZIPInputStream(inputStream);
        } else {
            return inputStream;
        }
    }
    
    private static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}
