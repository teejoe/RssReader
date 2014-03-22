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

package org.m2x.rssreader.view;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.Date;

import org.m2x.rssreader.MainApplication;
import org.m2x.rssreader.R;
import org.m2x.rssreader.util.HtmlUtils;
import org.m2x.rssreader.util.PrefUtils;

public class RssArticleView extends WebView {

    public interface OnActionListener {
    	
        public void onClickComment();

    }

    private static final String STATE_SCROLL_PERCENTAGE = "STATE_SCROLL_PERCENTAGE";

    private static final String TEXT_HTML = "text/html";
    //private static final String HTML_IMG_REGEX = "(?i)<[/]?[ ]?img(.|\n)*?>";

    private static final String THEME = PrefUtils.getString(PrefUtils.THEME, 
    		PrefUtils.DEFAULT_THEME);
    private static String sBackgroundColor;
    private static String sTextColor;
    private static String sButtonColor;
    private static String sSubtitleColor;

    static {
    	if (THEME.equals(PrefUtils.DEFAULT_THEME)) {
    		sBackgroundColor = MainApplication.getContext().getResources().getString(
    				R.string.background_theme_default);
    		sTextColor = MainApplication.getContext().getResources().getString(
    				R.string.text_color_theme_default);
    		sButtonColor = MainApplication.getContext().getResources().getString(
    				R.string.button_color_theme_default);
    		sSubtitleColor = MainApplication.getContext().getResources().getString(
    				R.string.subtitle_color_theme_default);
    	} else if (THEME.equals(PrefUtils.LIGHT_THEME)) {
    		sBackgroundColor = MainApplication.getContext().getResources().getString(
    				R.string.background_theme_light);
    		sTextColor = MainApplication.getContext().getResources().getString(
    				R.string.text_color_theme_light);
    		sButtonColor = MainApplication.getContext().getResources().getString(
    				R.string.button_color_theme_light);
    		sSubtitleColor = MainApplication.getContext().getResources().getString(
    				R.string.subtitle_color_theme_light);
    	} else if (THEME.equals(PrefUtils.DARK_THEME)) {
    		sBackgroundColor = MainApplication.getContext().getResources().getString(
    				R.string.background_theme_dark);
    		sTextColor = MainApplication.getContext().getResources().getString(
    				R.string.text_color_theme_dark);
    		sButtonColor = MainApplication.getContext().getResources().getString(
    				R.string.button_color_theme_dark);
    		sSubtitleColor = MainApplication.getContext().getResources().getString(
    				R.string.subtitle_color_theme_dark);
    	}
    }
    
    private static final String CSS = "<head><style type='text/css'> "
            + "body {max-width: 100%; margin: 1.2em 0.3cm 0.3cm 0.2cm; font-family: sans-serif-light; color: " + sTextColor + "; background-color:" + sBackgroundColor + "; line-height: 140%} "
            + "* {max-width: 100%; word-break: break-word}"
            + "h1, h2 {font-weight: normal; line-height: 130%} "
            + "h1 {font-size: 170%; margin-bottom: 0.1em} "
            + "h2 {font-size: 140%} "
            + "a {color: #0099CC}"
            + "h1 a {color: inherit; text-decoration: none}"
            + "img {height: auto} "
            + "pre {white-space: pre-wrap;} "
            + "blockquote {margin: 0.8em 0 0.8em 1.2em; padding: 0} "
            + "p {margin: 0.8em 0 0.8em 0} "
            + "p.subtitle {color: " + sSubtitleColor + "} "
            + "ul, ol {margin: 0 0 0.8em 0.6em; padding: 0 0 0 1em} "
            + "ul li, ol li {margin: 0 0 0.8em 0; padding: 0} "
            + "div.button-section {padding: 0.4cm 0; margin: 0; text-align: center} "
            + ".button-section p {margin: 0.1cm 0 0.2cm 0}"
            + ".button-section p.marginfix {margin: 0.5cm 0 0.5cm 0}"
            + ".button-section input, .button-section a {font-family: sans-serif-light; font-size: 100%; background-color: " + sButtonColor + "; color: " + sTextColor + "; text-decoration: none; border: none; border-radius:0.2cm; padding: 0.3cm} "
            + "</style><meta name='viewport' content='width=device-width'/></head>";

    private static final String BODY_START = "<body>";
    private static final String BODY_END = "</body>";
    private static final String TITLE_START = "<h1><a href='";
    private static final String TITLE_MIDDLE = "'>";
    private static final String TITLE_END = "</a></h1>";
    private static final String SUBTITLE_START = "<p class='subtitle'>";
    private static final String SUBTITLE_END = "</p>";
    private static final String BUTTON_SECTION_START = "<div class='button-section'>";
    private static final String BUTTON_SECTION_END = "</div>";
    private static final String BUTTON_START = "<p><input type='button' value='";
    private static final String BUTTON_MIDDLE = "' onclick='";
    private static final String BUTTON_END = "'/></p>";
    // the separate 'marginfix' selector in the following is only needed because the CSS box model treats <input> and <a> elements differently
    private static final String LINK_BUTTON_START = "<p class='marginfix'><a href='";
    private static final String LINK_BUTTON_MIDDLE = "'>";
    private static final String LINK_BUTTON_END = "</a></p>";
    //private static final String IMAGE_ENCLOSURE = "[@]image/";

    private float mScrollPercentage = 0;

    private OnActionListener mListener;

    private final JavaScriptObject mInjectedJSObject = new JavaScriptObject();

    public RssArticleView(Context context) {
        super(context);

        setupWebview();
    }

    public RssArticleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setupWebview();
    }

    public RssArticleView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        setupWebview();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superInstanceState", super.onSaveInstanceState());
        bundle.putFloat(STATE_SCROLL_PERCENTAGE, getScrollPercentage());

        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle bundle = (Bundle) state;
        mScrollPercentage = bundle.getFloat(STATE_SCROLL_PERCENTAGE);
        super.onRestoreInstanceState(bundle.getParcelable("superInstanceState"));
    }

    public float getScrollPercentage() {
        float positionTopView = getTop();
        float contentHeight = getContentHeight();
        float currentScrollPosition = getScrollY();

        return (currentScrollPosition - positionTopView) / contentHeight;
    }

    public void setScrollPercentage(float scrollPercentage) {
        mScrollPercentage = scrollPercentage;
    }

    public void setListener(OnActionListener listener) {
        mListener = listener;
    }

    public void setHtml(long entryId, String title, String link, String contentText, 
    		String author, long timestamp, boolean preferFullText) {
    	
        if (PrefUtils.getBoolean(PrefUtils.FETCH_PICTURES, true)) {
            contentText = HtmlUtils.replaceImageURLs(contentText, entryId);
        }

        if (getSettings().getBlockNetworkImage()) {
            // setBlockNetwortImage(false) calls postSync, which takes time, so we clean up the html first and change the value afterwards
            loadData("", TEXT_HTML, "UTF-8");
                getSettings().setBlockNetworkImage(false);
        }
   
        // do not put 'null' to the base url...
        loadDataWithBaseURL("", generateHtmlContent(title, link, contentText, 
        		author, timestamp, preferFullText), TEXT_HTML, "UTF-8", null);
    }

    private String generateHtmlContent(String title, String link, String contentText, String author, long timestamp, boolean preferFullText) {
        StringBuilder content = new StringBuilder(CSS).append(BODY_START);

        if (link == null) {
            link = "";
        }
        content.append(TITLE_START).append(link).append(TITLE_MIDDLE).append(title).append(TITLE_END).append(SUBTITLE_START);
        Date date = new Date(timestamp);
        Context context = getContext();
        StringBuilder dateStringBuilder = new StringBuilder(DateFormat.getLongDateFormat(context).format(date)).append(' ').append(
                DateFormat.getTimeFormat(context).format(date));

        if (author != null && !author.equals("")) {
            dateStringBuilder.append(" &mdash; ").append(author);
        }

        content.append(dateStringBuilder).append(SUBTITLE_END).append(contentText)
        		.append(BUTTON_SECTION_START).append(BUTTON_START)
        		.append(context.getString(R.string.open_comment)).append(BUTTON_MIDDLE)
        		.append("injectedJSObject.onClickComment();").append(BUTTON_END);

        if (link.length() > 0) {
            content.append(LINK_BUTTON_START).append(link).append(LINK_BUTTON_MIDDLE)
            		.append(context.getString(R.string.open_link)).append(LINK_BUTTON_END);
        }

        content.append(BUTTON_SECTION_END).append(BODY_END);

        return content.toString();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebview() {
        // For scrolling
        setHorizontalScrollBarEnabled(false);
        getSettings().setUseWideViewPort(false);

        // For color
        setBackgroundColor(Color.parseColor(sBackgroundColor));

        // Text zoom level from preferences
        int fontSize = Integer.parseInt(PrefUtils.getString(PrefUtils.FONT_SIZE, "10"));
        if (fontSize != 0) {
            getSettings().setDefaultFontSize(fontSize);
        }
        
        // For javascript
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(mInjectedJSObject, mInjectedJSObject.toString());

        // For HTML5 video
        setWebChromeClient(new WebChromeClient());

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (mScrollPercentage != 0) {
                    view.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            float webviewSize = getContentHeight() - getTop();
                            float positionInWV = webviewSize * mScrollPercentage;
                            int positionY = Math.round(getTop() + positionInWV);
                            scrollTo(0, positionY);
                        }
                        // Delay the scrollTo to make it work
                    }, 150);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Context context = getContext();
                try {
                    // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(context, R.string.open_link_failed, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private class JavaScriptObject {
        @Override
        @JavascriptInterface
        public String toString() {
            return "injectedJSObject";
        }

        @JavascriptInterface
        public void onClickComment() {
            mListener.onClickComment();
        }
    }
}
