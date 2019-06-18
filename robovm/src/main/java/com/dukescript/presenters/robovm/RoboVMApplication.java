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


import java.util.concurrent.CountDownLatch;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSOperationQueue;
import org.robovm.apple.foundation.NSURL;
import org.robovm.apple.foundation.NSURLRequest;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationDelegateAdapter;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIViewAutoresizing;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.apple.uikit.UIWebView;
import org.robovm.apple.uikit.UIWebViewDelegate;
import org.robovm.apple.uikit.UIWindow;

public final class RoboVMApplication extends UIApplicationDelegateAdapter {

    private static UIWindow window;
    private static UIWebView webView;
    private static UIWebViewDelegate delegate;
    private static String page;
    private static CountDownLatch waitFor;

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
        application.setStatusBarHidden(false);
        final CGRect bounds = UIScreen.getMainScreen().getBounds();
        webView = new UIWebView(bounds);
        webView.setDelegate(delegate);
        webView.setAutoresizingMask(UIViewAutoresizing.with(UIViewAutoresizing.FlexibleBottomMargin, UIViewAutoresizing.FlexibleHeight, UIViewAutoresizing.FlexibleLeftMargin, UIViewAutoresizing.FlexibleRightMargin, UIViewAutoresizing.FlexibleTopMargin, UIViewAutoresizing.FlexibleWidth));
        CGRect whole = UIScreen.getMainScreen().getBounds();
        window = new UIWindow(whole);
        window.setRootViewController(new UIViewController() {
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

            @Override
            public void didRotate(UIInterfaceOrientation uiio) {
            }
        });
        window.getRootViewController().setView(webView);
        window.setBackgroundColor(UIColor.white());
        window.addSubview(webView);
        window.makeKeyAndVisible();
        NSURLRequest req = new NSURLRequest(new NSURL(page));
        webView.loadRequest(req);
        waitFor.countDown();
        return true;
    }

    public static void runOnUiThread(Runnable w) {
        NSOperationQueue mq = NSOperationQueue.getMainQueue();
        mq.addOperation(w);
    }

    public static void displayPage(String p, UIWebViewDelegate d) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        try {
            RoboVMApplication.mainView(p, d);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        pool.close();

    }

    private static UIWebView mainView(String p, UIWebViewDelegate d) {
        page = p;
        waitFor = new CountDownLatch(1);
        delegate = d;
        UIApplication.main(new String[]{p}, null, RoboVMApplication.class);
        try {
            waitFor.await();
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
        return webView;
    }

}
