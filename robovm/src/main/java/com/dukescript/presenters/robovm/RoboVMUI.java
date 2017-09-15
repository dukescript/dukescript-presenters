package com.dukescript.presenters.robovm;

import com.dukescript.presenters.ios.UI;
import org.openide.util.lookup.ServiceProvider;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIWebView;
import org.robovm.apple.uikit.UIWebViewDelegateAdapter;
import org.robovm.apple.uikit.UIWebViewNavigationType;


/*
 * #%L
 * DukeScript Presenter for iOS - a library from the "DukeScript Presenters" project.
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

@ServiceProvider(service = UI.class)
public final class RoboVMUI extends UI {
    @Override
    public String identifier() {
        return NSBundle.getMainBundle().getBundleIdentifier();
    }

    @Override
    public String pathForResouce(String name, String ext, String subdir) {
        if (subdir == null) {
            return NSBundle.getMainBundle().findResourcePath(name, ext);
        } else {
            return NSBundle.getMainBundle().findResourcePath(name, ext, subdir);
        }
    }

    @Override
    public String evaluateJavaScript(Object webView, String js) {
        UIWebView v = (UIWebView) webView;
        return v.evaluateJavaScript(js);
    }

    @Override
    public boolean openFileURL(String url) {
        final NSURL openURL = new NSURL(url);
        if (!openURL.isFileURL()) {
            UIApplication.getSharedApplication().openURL(openURL);
            return false;
        }
        return true;
    }

    @Override
    public void runOnUiThread(Runnable r) {
        RoboVMApplication.runOnUiThread(r);
    }

    @Override
    public void displayPage(String page, WebViewAdapter webViewDelegate) {
        RoboVMApplication.displayPage(page, new WebViewDelegate(webViewDelegate));
    }

    @Override
    public void setViewUp(Object view, String page, WebViewAdapter adapter) {
        if (!(view instanceof UIWebView)) {
            throw new ClassCastException("Expecting instance of org.robovm.apple.uikit.UIWebView, but got " + view);
        }
        UIWebView webView = (UIWebView) view;
        webView.setDelegate(new WebViewDelegate(adapter));
        NSURLRequest req = new NSURLRequest(new NSURL(page));
        webView.loadRequest(req);
    }

    private final class WebViewDelegate extends UIWebViewDelegateAdapter {
        private final WebViewAdapter delegate;

        WebViewDelegate(WebViewAdapter delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean shouldStartLoad(UIWebView webView, NSURLRequest request, UIWebViewNavigationType navigationType) {
            final String url = request.getURL().getAbsoluteString();
            return delegate.shouldStartLoad(webView, url);
        }

        @Override
        public void didStartLoad(UIWebView webView) {
            delegate.didStartLoad(webView);
        }

        @Override
        public void didFailLoad(UIWebView webView, NSError error) {
            delegate.didFailLoad(webView, error.getLocalizedFailureReason());
        }

        @Override
        public void didFinishLoad(UIWebView webView) {
            delegate.didFinishLoad(webView);
        }
    }
}
