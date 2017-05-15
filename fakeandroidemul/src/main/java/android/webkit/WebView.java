package android.webkit;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.Rect;
import android.net.http.SslCertificate;
import android.os.Bundle;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.AbsoluteLayout;
import java.io.File;
import java.util.Map;


/*
 * #%L
 * DukeScript Presenter for Android - a library from the "DukeScript Presenters" project.
 * Visit http://dukescript.com for support and commercial license.
 * %%
 * Copyright (C) 2015 Eppleton IT Consulting
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

public class WebView extends AbsoluteLayout implements ViewTreeObserver.OnGlobalFocusChangeListener, ViewGroup.OnHierarchyChangeListener {

    public static class HitTestResult {

        public static final int UNKNOWN_TYPE = 0;
        @Deprecated
        public static final int ANCHOR_TYPE = 1;
        public static final int PHONE_TYPE = 2;
        public static final int GEO_TYPE = 3;
        public static final int EMAIL_TYPE = 4;
        public static final int IMAGE_TYPE = 5;
        @Deprecated
        public static final int IMAGE_ANCHOR_TYPE = 6;
        public static final int SRC_ANCHOR_TYPE = 7;
        public static final int SRC_IMAGE_ANCHOR_TYPE = 8;
        public static final int EDIT_TEXT_TYPE = 9;

        HitTestResult() {
        }

        public int getType() {
            return 0;
        }

        public String getExtra() {
            return null;
        }
    }

    public static interface FindListener {

        public void onFindResultReceived(int i, int i1, boolean bln);
    }

    public class WebViewTransport {

        public WebViewTransport() {
        }

        public void setWebView(WebView webview) {
        }

        public WebView getWebView() {
            return null;
        }
    }
    public static final String SCHEME_TEL = "tel:";
    public static final String SCHEME_MAILTO = "mailto:";
    public static final String SCHEME_GEO = "geo:0,0?q=";

    public WebView(Context context) {
        super(context);
    }

    public WebView(Context context, AttributeSet attrs) {
        super(context);
    }

    public WebView(Context context, AttributeSet attrs, int defStyle) {
        super(context);
    }

    public WebView(Context context, AttributeSet attrs, int defStyle, boolean privateBrowsing) {
        super(context);
    }

    public void evaluateJavascript(String script, ValueCallback<String> result) {
    }

    public void setHorizontalScrollbarOverlay(boolean overlay) {
    }

    public void setVerticalScrollbarOverlay(boolean overlay) {
    }

    public boolean overlayHorizontalScrollbar() {
        return true;
    }

    public boolean overlayVerticalScrollbar() {
        return true;
    }

    @Deprecated
    public int getVisibleTitleHeight() {
        return 0;
    }

    public SslCertificate getCertificate() {
        return null;
    }

    public void setCertificate(SslCertificate certificate) {
    }

    public void savePassword(String host, String username, String password) {
    }

    public void setHttpAuthUsernamePassword(String host, String realm, String username, String password) {
    }

    public String[] getHttpAuthUsernamePassword(String host, String realm) {
        return null;
    }

    public void destroy() {
    }

    @Deprecated
    public static void enablePlatformNotifications() {
    }

    @Deprecated
    public static void disablePlatformNotifications() {
    }

    public void setNetworkAvailable(boolean networkUp) {
    }

    public WebBackForwardList saveState(Bundle outState) {
        return null;
    }

    @Deprecated
    public boolean savePicture(Bundle b, File dest) {
        return false;
    }

    @Deprecated
    public boolean restorePicture(Bundle b, File src) {
        return false;
    }

    public WebBackForwardList restoreState(Bundle inState) {
        return null;
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
    }

    public void loadUrl(String url) {
    }

    public void postUrl(String url, byte[] postData) {
    }

    public void loadData(String data, String mimeType, String encoding) {
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding, String historyUrl) {
    }

    public void saveWebArchive(String filename) {
    }

    public void saveWebArchive(String basename, boolean autoname, ValueCallback<String> callback) {
    }

    public void stopLoading() {
    }

    public void reload() {
    }

    public boolean canGoBack() {
        return false;
    }

    public void goBack() {
    }

    public boolean canGoForward() {
        return false;
    }

    public void goForward() {
    }

    public boolean canGoBackOrForward(int steps) {
        return false;
    }

    public void goBackOrForward(int steps) {
    }

    public boolean isPrivateBrowsingEnabled() {
        return false;
    }

    public boolean pageUp(boolean top) {
        return false;
    }

    public boolean pageDown(boolean bottom) {
        return false;
    }

    public void clearView() {
    }

    public Picture capturePicture() {
        return null;
    }

    public float getScale() {
        return 0;
    }

    public void setInitialScale(int scaleInPercent) {
    }

    public void invokeZoomPicker() {
    }

    public HitTestResult getHitTestResult() {
        return null;
    }

    public void requestFocusNodeHref(Message hrefMsg) {
    }

    public void requestImageRef(Message msg) {
    }

    public String getUrl() {
        return null;
    }

    public String getOriginalUrl() {
        return null;
    }

    public String getTitle() {
        return null;
    }

    public Bitmap getFavicon() {
        return null;
    }

    public int getProgress() {
        return 0;
    }

    public int getContentHeight() {
        return 0;
    }

    public void pauseTimers() {
    }

    public void resumeTimers() {
    }

    public void onPause() {
    }

    public void onResume() {
    }

    public void freeMemory() {
    }

    public void clearCache(boolean includeDiskFiles) {
    }

    public void clearFormData() {
    }

    public void clearHistory() {
    }

    public void clearSslPreferences() {
    }

    public WebBackForwardList copyBackForwardList() {
        return null;
    }

    public void setFindListener(FindListener listener) {
    }

    public void findNext(boolean forward) {
    }

    @Deprecated
    public int findAll(String find) {
        return 0;
    }

    public void findAllAsync(String find) {
    }

    public boolean showFindDialog(String text, boolean showIme) {
        return false;
    }

    public static String findAddress(String addr) {
        return null;
    }

    public void clearMatches() {
    }

    public void documentHasImages(Message response) {
    }

    public void setWebViewClient(WebViewClient client) {
    }

    public void setDownloadListener(DownloadListener listener) {
    }

    public void setWebChromeClient(WebChromeClient client) {
    }

    public void addJavascriptInterface(Object object, String name) {
    }

    public void removeJavascriptInterface(String name) {
    }

    public WebSettings getSettings() {
        return null;
    }

    @Deprecated
    public void emulateShiftHeld() {
    }

    @Deprecated
    public void onChildViewAdded(View parent, View child) {
    }

    @Deprecated
    public void onChildViewRemoved(View p, View child) {
    }

    @Deprecated
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
    }

    public void setMapTrackballToArrowKeys(boolean setMap) {
    }

    public void flingScroll(int vx, int vy) {
    }

    public boolean canZoomIn() {
        return true;
    }

    public boolean canZoomOut() {
        return true;
    }

    public boolean zoomIn() {
        return true;
    }

    public boolean zoomOut() {
        return true;
    }

    @Deprecated
    public void debugDump() {
    }

    protected void onAttachedToWindow() {
    }

    protected void onDetachedFromWindow() {
    }

    public void setLayoutParams(AbsoluteLayout.LayoutParams params) {
    }

    public void setOverScrollMode(int mode) {
    }

    public void setScrollBarStyle(int style) {
    }

    protected int computeHorizontalScrollRange() {
        return 0;
    }

    protected int computeHorizontalScrollOffset() {
        return 0;
    }

    protected int computeVerticalScrollRange() {
        return 0;
    }

    protected int computeVerticalScrollOffset() {
        return 0;
    }

    protected int computeVerticalScrollExtent() {
        return 0;
    }

    public void computeScroll() {
    }

    public boolean onHoverEvent(MotionEvent event) {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }

    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }

    public boolean onTrackballEvent(MotionEvent event) {
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
        return false;
    }

    @Deprecated
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    }

    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
    }

    public boolean performAccessibilityAction(int action, Bundle arguments) {
        return false;
    }

    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
    }

    protected void onWindowVisibilityChanged(int visibility) {
    }

    protected void onDraw(Canvas canvas) {
    }

    public boolean performLongClick() {
        return false;
    }

    protected void onConfigurationChanged(Configuration newConfig) {
    }

    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return null;
    }

    protected void onVisibilityChanged(View changedView, int visibility) {
    }

    public void onWindowFocusChanged(boolean hasWindowFocus) {
    }

    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
    }

    protected void onSizeChanged(int w, int h, int ow, int oh) {
    }

    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        return false;
    }

    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return false;
    }

    @Deprecated
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rect, boolean immediate) {
        return false;
    }

    public void setBackgroundColor(int color) {
    }

    public void setLayerType(int layerType, Paint paint) {
    }
}
