package com.dukescript.presenters.moe;

/*
 * #%L
 * DukeScript Presenter for Multi OS Engine - a library from the "DukeScript Presenters" project.
 * 
 * Dukehoff GmbH designates this particular file as subject to the "Classpath"
 * exception as provided in the README.md file that accompanies this code.
 * %%
 * Copyright (C) 2015 - 2019 Dukehoff GmbH
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


import apple.foundation.NSBundle;
import apple.foundation.NSError;
import apple.foundation.NSURL;
import apple.foundation.NSURLRequest;
import apple.uikit.UIApplication;
import apple.uikit.UIWebView;
import apple.uikit.protocol.UIWebViewDelegate;
import com.dukescript.presenters.ios.UI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = UI.class)
public final class MoeUI extends UI {
    static final Logger LOG = Logger.getLogger(MoeUI.class.getName());

    public MoeUI() {
        LOG.fine("MoeUI created");
    }

    @Override
    public String identifier() {
        return NSBundle.mainBundle().bundleIdentifier();
    }

    @Override
    public String pathForResouce(String name, String ext, String subdir) {
        if (subdir == null) {
            return NSBundle.mainBundle().pathForResourceOfType(name, ext);
        } else {
            return NSBundle.mainBundle().pathForResourceOfTypeInDirectory(name, ext, subdir);
        }
    }

    @Override
    public String evaluateJavaScript(Object webView, String js) {
        UIWebView v = (UIWebView) webView;
        return v.stringByEvaluatingJavaScriptFromString(js);
    }

    @Override
    public boolean openFileURL(String url) {
        final NSURL openURL = NSURL.URLWithString(url);
        if (!openURL.isFileURL()) {
            UIApplication.sharedApplication().openURL(openURL);
            return false;
        }
        return true;
    }

    @Override
    public void runOnUiThread(Runnable r) {
        MoeApplication.runOnUiThread(r);
    }

    @Override
    public void displayPage(String page, WebViewAdapter webViewDelegate) {
        MoeApplication.displayPage(page, new WebViewDelegate(webViewDelegate));
    }

    @Override
    public void setViewUp(Object view, String page, WebViewAdapter adapter) {
        if (!(view instanceof UIWebView)) {
            throw new ClassCastException("Expecting instance of apple.uikit.UIWebView, but got " + view);
        }
        UIWebView webView = (UIWebView) view;
        webView.setDelegate(new WebViewDelegate(adapter));
        if (page != null) {
            NSURLRequest req = NSURLRequest.requestWithURL(NSURL.URLWithString(page));
            webView.loadRequest(req);
        }
    }

    private static final class WebViewDelegate implements UIWebViewDelegate {
        private final WebViewAdapter delegate;

        WebViewDelegate(WebViewAdapter delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean webViewShouldStartLoadWithRequestNavigationType(UIWebView webView, NSURLRequest request, long navigationType) {
            final NSURL reqURL = request.URL();
            final String url = reqURL == null ? "" : reqURL.toString();
            LOG.log(Level.FINE, "request {0} with url {1}", new Object[] { request, url });
            return delegate.shouldStartLoad(webView, url);
        }

        @Override
        public void webViewDidStartLoad(UIWebView webView) {
            delegate.didStartLoad(webView);
        }

        @Override
        public void webViewDidFailLoadWithError(UIWebView webView, NSError error) {
            delegate.didFailLoad(webView, error.toString());
        }

        @Override
        public void webViewDidFinishLoad(UIWebView webView) {
            delegate.didFinishLoad(webView);
        }
    }
}
