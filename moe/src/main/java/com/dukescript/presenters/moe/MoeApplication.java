package com.dukescript.presenters.moe;

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

import apple.NSObject;
import apple.coregraphics.struct.CGRect;
import apple.foundation.NSDictionary;
import apple.foundation.NSURL;
import apple.foundation.NSURLRequest;
import apple.uikit.UIApplication;
import apple.uikit.UIColor;
import apple.uikit.UIScreen;
import apple.uikit.UIViewController;
import apple.uikit.UIWebView;
import apple.uikit.UIWindow;
import apple.uikit.c.UIKit;
import apple.uikit.enums.UIViewAutoresizing;
import apple.uikit.protocol.UIApplicationDelegate;
import apple.uikit.protocol.UIWebViewDelegate;
import java.util.concurrent.CountDownLatch;
import org.moe.natj.general.NatJ;
import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;
import org.moe.natj.objc.ann.Selector;

@Runtime(value = ObjCRuntime.class)
@ObjCClassName(value = "iOSApp")
@RegisterOnStartup
public class MoeApplication extends NSObject implements UIApplicationDelegate {

    private static UIWindow window;
    private static UIWebView webView;
    private static UIWebViewDelegate delegate;
    private static String page;
    private static CountDownLatch waitFor;

    protected MoeApplication(Pointer p) {
        super(p);
    }

    @Override
    public boolean applicationDidFinishLaunchingWithOptions(UIApplication application, NSDictionary<?, ?> launchOptions) {
        application.setStatusBarHidden(false);
        final CGRect bounds = UIScreen.mainScreen().bounds();
        webView = UIWebView.alloc().initWithFrame(bounds);
        webView.setDelegate(delegate);
        webView.setAutoresizingMask(UIViewAutoresizing.FlexibleBottomMargin | UIViewAutoresizing.FlexibleHeight | UIViewAutoresizing.FlexibleLeftMargin | UIViewAutoresizing.FlexibleRightMargin | UIViewAutoresizing.FlexibleTopMargin | UIViewAutoresizing.FlexibleWidth);
        CGRect whole = UIScreen.mainScreen().bounds();
        window = UIWindow.alloc().initWithFrame(whole);
        window.setRootViewController(MainController.alloc());
        window.rootViewController().setView(webView);
        window.setBackgroundColor(UIColor.whiteColor());
        window.addSubview(webView);
        window.makeKeyAndVisible();
        NSURLRequest req = NSURLRequest.requestWithURL(NSURL.URLWithString(page));
        webView.loadRequest(req);
        waitFor.countDown();
        return true;
    }

    public static void displayPage(String toExternalForm, UIWebViewDelegate webViewDelegate) {
        mainView(page, delegate);
    }

    private static UIWebView mainView(String p, UIWebViewDelegate d) {
        page = p;
        waitFor = new CountDownLatch(1);
        delegate = d;
        UIKit.UIApplicationMain(0, null, null, "iOSApp");
        try {
            waitFor.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        return webView;
    }

    @Runtime(value = ObjCRuntime.class)
    @RegisterOnStartup
    @ObjCClassName(value = "MainController")
    static final class MainController extends UIViewController {

        static {
            NatJ.register();
        }

        protected MainController(Pointer pntr) {
            super(pntr);
        }

        @Selector(value = "alloc")
        public static native MainController alloc();

        @Override
        public boolean prefersStatusBarHidden() {
            return true;
        }

        @Override
        public boolean shouldAutorotate() {
            return true;
        }

        @Override
        public boolean shouldAutomaticallyForwardRotationMethods() {
            return false;
        }
    }

}
