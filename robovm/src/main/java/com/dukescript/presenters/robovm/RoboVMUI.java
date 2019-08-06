package com.dukescript.presenters.robovm;

/*
 * #%L
 * DukeScript Presenter for RoboVM - a library from the "DukeScript Presenters" project.
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

import com.dukescript.presenters.ios.UI;
import org.openide.util.lookup.ServiceProvider;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSObject;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.webkit.WKNavigation;
import org.robovm.apple.webkit.WKNavigationAction;
import org.robovm.apple.webkit.WKNavigationActionPolicy;
import org.robovm.apple.webkit.WKNavigationDelegateAdapter;
import org.robovm.apple.webkit.WKWebView;
import org.robovm.objc.block.VoidBlock1;
import org.robovm.objc.block.VoidBlock2;



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
        WKWebView v = (WKWebView) webView;
        v.evaluateJavaScript(js, new VoidBlock2<NSObject, NSError>() {
            @Override
            public void invoke(NSObject a, NSError b) {
            }
        });
        return null;
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
        if (!(view instanceof WKWebView)) {
            throw new ClassCastException("Expecting instance of org.robovm.apple.uikit.UIWebView, but got " + view);
        }
        WKWebView webView = (WKWebView) view;
        webView.setNavigationDelegate(new WebViewDelegate(adapter));
        if (page != null) {
            NSURLRequest req = new NSURLRequest(new NSURL(page));
            webView.loadRequest(req);
        }
    }

    private final class WebViewDelegate extends WKNavigationDelegateAdapter {
        private final WebViewAdapter delegate;

        WebViewDelegate(WebViewAdapter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void decidePolicyForNavigationAction(WKWebView webView, WKNavigationAction action, VoidBlock1<WKNavigationActionPolicy> handler) {
            final String url = action.getRequest().getURL().getAbsoluteString();
            if (delegate.shouldStartLoad(webView, url)) {
                handler.invoke(WKNavigationActionPolicy.Allow);
            } else {
                handler.invoke(WKNavigationActionPolicy.Cancel);
            }
        }

        @Override
        public void didStartProvisionalNavigation(WKWebView webView, WKNavigation navigation) {
            delegate.didStartLoad(webView);
        }

        @Override
        public void didFinishNavigation(WKWebView webView, WKNavigation navigation) {
            delegate.didFinishLoad(webView);
        }

        @Override
        public void didFailNavigation(WKWebView webView, WKNavigation navigation, NSError error) {
            delegate.didFailLoad(webView, error.getLocalizedFailureReason());
        }

        @Override
        public void didFailProvisionalNavigation(WKWebView webView, WKNavigation navigation, NSError error) {
            delegate.didFailLoad(webView, error.getLocalizedFailureReason());
        }
    }
}
