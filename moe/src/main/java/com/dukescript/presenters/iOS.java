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
import java.net.URL;
import java.net.URLDecoder;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.netbeans.html.boot.spi.Fn;
import org.openide.util.lookup.ServiceProvider;

import apple.foundation.NSBundle;
import apple.foundation.NSError;
import apple.foundation.NSURL;
import apple.foundation.NSURLRequest;
import apple.uikit.UIApplication;
import apple.uikit.UIWebView;
import apple.uikit.protocol.UIWebViewDelegate;
import com.dukescript.presenters.moe.MoeApplication;
import java.io.Flushable;

/** Ultimate <a href="http://dukescript.com">DukeScript</a>
 * <a href="https://github.com/dukescript/dukescript-presenters">Presenter</a>
 * for all iOS devices. Builds upon
 * <a href="http://robovm.org">RoboVM</a> or its
 * <a href="https://dukescript.com/update/2017/03/06/robovm-fork.html">alternatives</a>.
 * To use this presenter specify following dependency:
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.dukescript.presenters&lt;/groupId&gt;
 *   &lt;artifactId&gt;ios&lt;/artifactId&gt;
 *   &lt;version&gt;<a target="blank" href="http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.dukescript.presenters%22%20AND%20a%3A%22ios%22">1.x</a>&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 */
@ServiceProvider(service = Fn.Presenter.class)
public final class iOS extends Generic
        implements Executor, Fn.Presenter, Fn.KeepAlive, Flushable {

    static final Logger LOG = Logger.getLogger(iOS.class.getName());
    private UIWebView webView;
    private Thread dispatchThread;

    public iOS() throws Exception {
        super(true, false, "iOS", NSBundle.mainBundle().bundleIdentifier(), null);
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
            MoeApplication.runOnUiThread(r);
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
                } catch (Error ex) {
                    iOS.this.log(Level.SEVERE, "Error executing " + command.getClass().getName(), ex);
                    throw ex;
                } catch (RuntimeException ex) {
                    iOS.this.log(Level.SEVERE, "Error executing " + command.getClass().getName(), ex);
                    throw ex;
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

    @Override
    void callbackFn(String welcome, OnReady onReady) {
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
        onReady.callbackReady("iOS");
    }

    @Override
    void loadJS(String js) {
        String res = webView.stringByEvaluatingJavaScriptFromString(js);
        LOG.log(Level.FINE, "loadJS done: {0}", res);
    }

    @Override
    public void displayPage(URL page, Runnable onPageLoad) {
        MoeApplication.displayPage(page.toExternalForm(), new WebViewDelegate(onPageLoad));
    }

    private final class WebViewDelegate implements UIWebViewDelegate {

        private final Runnable onPageLoad;

        public WebViewDelegate(Runnable onPageLoad) {
            this.onPageLoad = onPageLoad;
        }

        @Override
        public boolean webViewShouldStartLoadWithRequestNavigationType(UIWebView webView, NSURLRequest request, long navigationType) {
            final String url = request.URL().absoluteString();
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
                        String check = webView.stringByEvaluatingJavaScriptFromString(exec.toString());
                        LOG.log(Level.FINE, "evaluating {0} with return {1}", new Object[]{exec, check});
                    }
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Error processing " + url, ex);
                }
                return false;
            }
            final NSURL openURL = request.URL();
            if (!openURL.isFileURL()) {
                UIApplication.sharedApplication().openURL(openURL);
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
        public void webViewDidStartLoad(UIWebView webView) {
            iOS.this.webView = webView;
        }

        @Override
        public void webViewDidFailLoadWithError(UIWebView webView, NSError error) {
            iOS.this.webView = webView;
        }

        @Override
        public void webViewDidFinishLoad(UIWebView webView) {
            if (dispatchThread == null) {
                execute(onPageLoad);
                dispatchThread = Thread.currentThread();
            }
        }

    }

}
