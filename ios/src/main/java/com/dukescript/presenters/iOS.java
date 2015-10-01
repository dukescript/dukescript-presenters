package com.dukescript.presenters;

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
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.html.boot.spi.Fn;
import org.openide.util.lookup.ServiceProvider;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSBundle;
import org.robovm.apple.foundation.NSError;
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
import org.robovm.apple.uikit.UIWebViewDelegateAdapter;
import org.robovm.apple.uikit.UIWebViewNavigationType;
import org.robovm.apple.uikit.UIWindow;

@ServiceProvider(service = Fn.Presenter.class)
public final class iOS extends Generic
        implements Executor {

    static final Logger LOG = Logger.getLogger(iOS.class.getName());
    private UIWebView webView;
    private Thread dispatchThread;

    public iOS() throws Exception {
        super(true, false, "iOS", NSBundle.getMainBundle().getBundleIdentifier());
    }

    @Override
    void log(Level level, String msg, Object... args) {
        if (args.length == 1 && args[0] instanceof Throwable) {
            LOG.log(level, msg, (Throwable) args[0]);
        } else {
            LOG.log(level, msg, args);
        }
    }

    @Override
    final void dispatch(final Runnable r) {
        if (dispatchThread == Thread.currentThread()) {
            r.run();
        } else {
            runOnUiThread(r);
        }
    }

    @Override
    public final void execute(final Runnable command) {
        class CtxRun implements Runnable {

            @Override
            public void run() {
                Closeable c = Fn.activate(iOS.this);
                try {
                    command.run();
                } finally {
                    try {
                        c.close();
                    } catch (IOException ex) {
                        iOS.this.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        dispatch(new CtxRun());
    }

    static void runOnUiThread(Runnable w) {
        NSOperationQueue mq = NSOperationQueue.getMainQueue();
        mq.addOperation(w);
    }

    @Override
    String callbackFn(String welcome) {
        loadJS(
                "function iOS(method, a1, a2, a3, a4) {\n"
                + "  window.iOSVal = null;\n"
                + "  var url = 'presenter://' + method;\n"
                + "  url += '?p0=' + encodeURIComponent(a1);\n"
                + "  url += '&p1=' + encodeURIComponent(a2);\n"
                + "  url += '&p2=' + encodeURIComponent(a3);\n"
                + "  url += '&p3=' + encodeURIComponent(a4);\n"
                + "  var iframe = document.createElement('iframe');\n"
                + "  iframe.setAttribute('width','1');\n"
                + "  iframe.setAttribute('height','1');\n"
                + "  iframe.setAttribute('frameborder',0);\n"
                + "  iframe.setAttribute('style','display:none');\n"
                + "  iframe.setAttribute('src', url);\n"
                + "  document.documentElement.appendChild(iframe);\n"
                + "  iframe.parentNode.removeChild(iframe);\n"
                + "  iframe = null;\n"
                + "  var r = window.iOSVal;\n"
                + "  delete window.iOSVal;\n"
                + "  return r;\n"
                + "}\n"
        );
        loadJS(welcome);
        return "iOS";
    }

    @Override
    void loadJS(String js) {
        String res = webView.evaluateJavaScript(js);
        LOG.log(Level.FINE, "loadJS done: {0}", res);
    }

    @Override
    public void displayPage(URL page, Runnable onPageLoad) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        try {
            App.mainView(page.toExternalForm(), new WebViewDelegate(onPageLoad));
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        pool.close();
    }

    @Override
    public void loadScript(Reader code) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (;;) {
            int ch = code.read();
            if (ch == -1) {
                break;
            }
            sb.append((char) ch);
        }
        webView.evaluateJavaScript(sb.toString());
    }

    static final class App extends UIApplicationDelegateAdapter {

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
            webView.setAutoresizingMask(UIViewAutoresizing.with(
                    UIViewAutoresizing.FlexibleBottomMargin,
                    UIViewAutoresizing.FlexibleHeight,
                    UIViewAutoresizing.FlexibleLeftMargin,
                    UIViewAutoresizing.FlexibleRightMargin,
                    UIViewAutoresizing.FlexibleTopMargin,
                    UIViewAutoresizing.FlexibleWidth
            ));
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

        static UIWebView mainView(String p, UIWebViewDelegate d) throws IOException {
            page = p;
            waitFor = new CountDownLatch(1);
            delegate = d;
            UIApplication.main(new String[]{p}, null, App.class);
            try {
                waitFor.await();
            } catch (InterruptedException ex) {
                throw new InterruptedIOException();
            }
            return webView;
        }
    }

    private final class WebViewDelegate extends UIWebViewDelegateAdapter {

        private final Runnable onPageLoad;

        public WebViewDelegate(Runnable onPageLoad) {
            this.onPageLoad = onPageLoad;
        }

        @Override
        public boolean shouldStartLoad(UIWebView webView, NSURLRequest request, UIWebViewNavigationType navigationType) {
            final String url = request.getURL().getAbsoluteString();
            final String pref = "presenter://";
            if (url.startsWith(pref)) {
                int[] q = {url.indexOf('?')};
                String method = url.substring(pref.length(), q[0]);
                try {
                    String p0 = nextParam(url, q);
                    String p1 = nextParam(url, q);
                    String p2 = nextParam(url, q);
                    String p3 = nextParam(url, q);
                    String ret = callback(method, p0, p1, p2, p3);
                    if (ret != null) {
                        StringBuilder exec = new StringBuilder();
                        exec.append("window.iOSVal = ");
                        if (ret.startsWith("javascript:")) {
                            exec.append("\"");
                            for (int i = 0; i < ret.length(); i++) {
                                char ch = ret.charAt(i);
                                if (ch == '\n') {
                                    exec.append("\\n");
                                } else if (ch == '\"') {
                                    exec.append("\\\"");
                                } else if (ch < 16) {
                                    exec.append("\\u000").append(Integer.toHexString(ch));
                                } else if (ch < 32) {
                                    exec.append("\\u00").append(Integer.toHexString(ch));
                                } else {
                                    exec.append(ch);
                                }
                            }
                            exec.append("\"");
                        } else {
                            exec.append(ret);
                        }
                        String check = webView.evaluateJavaScript(exec.toString());
                        LOG.log(Level.FINE, "evaluating {0} with return {1}", new Object[]{exec, check});
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Error processing " + url, ex);
                }
                return false;
            }
            if (navigationType == UIWebViewNavigationType.LinkClicked) {
                UIApplication.getSharedApplication().openURL(request.getURL());
                return false;
            }
            return true;
        }

        private String nextParam(String text, int[] from) throws Exception {
            int eq = text.indexOf('=', from[0]);
            int amp = text.indexOf('&', eq);
            if (amp == -1) {
                amp = text.length();
            }
            from[0] = amp;
            return URLDecoder.decode(text.substring(eq + 1, amp), "UTF-8");
        }

        @Override
        public void didStartLoad(UIWebView webView) {
            iOS.this.webView = webView;
        }

        @Override
        public void didFailLoad(UIWebView webView, NSError error) {
            iOS.this.webView = webView;
        }

        @Override
        public void didFinishLoad(UIWebView webView) {
            if (dispatchThread == null) {
                execute(onPageLoad);
                dispatchThread = Thread.currentThread();
            }
        }

    }

}
