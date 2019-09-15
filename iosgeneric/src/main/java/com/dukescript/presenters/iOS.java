package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Presenter for Any iOS - a library from the "DukeScript Presenters" project.
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
import java.io.Closeable;
import java.io.File;
import java.io.Flushable;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.html.BrwsrCtx;
import net.java.html.js.JavaScriptBody;
import net.java.html.boot.BrowserBuilder;
import org.netbeans.html.boot.spi.Fn;
import org.openide.util.lookup.ServiceProvider;

/** Versatile <a target="_blank" href="http://dukescript.com">DukeScript</a>
 * <a target="_blank" href="https://github.com/dukescript/dukescript-presenters">Presenter</a>
 * for all <b>iOS</b> devices. This presenter supports two <b>Java</b> virtual machines:
 * <ul>
 *   <li><a target="_blank" href="https://github.com/MobiVM/robovm">RoboVM</a> by com.mobidevelop guys</li>
 *   <li><a target="_blank" href="https://multi-os-engine.org/">Multi OS Engine</a> by Intel</li>
 * </ul>
 * <p>
 * To use this presenter in your <a href="https://github.com/MobiVM/robovm">RoboVM</a> based project,
 * please specify following dependency:
 * </p>
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.dukescript.presenters&lt;/groupId&gt;
 *   &lt;artifactId&gt;<b>ios</b>&lt;/artifactId&gt; &lt;!-- gives you <em>RoboVM</em> version --&gt;
 *   &lt;version&gt;<a target="_blank" href="http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.dukescript.presenters%22%20AND%20a%3A%22ios%22">1.x</a>&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 * <p>
 * If you are using <a target="_blank" href="https://multi-os-engine.org/">Multi OS Engine</a>,
 * just change the dependency to refer to <b>moe</b> artifact:
 * </p>
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.dukescript.presenters&lt;/groupId&gt;
 *   &lt;artifactId&gt;<b>moe</b>&lt;/artifactId&gt; &lt;!-- gives you <em>Multi OS Engine</em> version --&gt;
 *   &lt;version&gt;<a target="_blank" href="http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.dukescript.presenters%22%20AND%20a%3A%22moe%22">1.x</a>&lt;/version&gt; &lt;-- at least 1.4 --&gt;
 * &lt;/dependency&gt;
 * </pre>
 * both of these versions use the same API and thus they are covered by the same
 * Javadoc of this {@code iOS} class.
 *
 * <p>
 * <b>Embedding</b>
 * <p>
 * Read more about embedding DukeScript technology into existing
 * Multi OS Engine or RoboVM <b>iOS</b> application. Start by reading javadoc
 * of the
 * {@link #configure(java.lang.String, java.lang.Object, java.lang.String) configure}
 * method.
 *
 * <p>
 * <b>Cross-Platform</b>
 * <p>
 * Learn how to build fully portable (iOS, browser, desktop) Java applications
 * in <a target="_blank" href="https://dukescript.com/getting_started.html">DukeScript getting
 * started</a> tutorial. The great value of the tutorial is that it
 * helps you set your project up correctly - getting the classpath right may be
 * tough for the first time.
 * Launching the presenter
 * is a matter of a few {@link BrowserBuilder#newBrowser(java.lang.Object...) BrowserBuilder}
 * calls and then running your initialization code when
 * {@linkplain BrowserBuilder#loadFinished(java.lang.Runnable)
 * page load is finished}.
 *
 */
@ServiceProvider(service = Fn.Presenter.class)
public final class iOS extends Generic
        implements Executor, Fn.Presenter, Fn.KeepAlive, Flushable {

    static final Logger LOG = Logger.getLogger(iOS.class.getName());
    private Object webView;
    private Executor dispatchExecutor;
    private Thread dispatchThread;
    private List<Runnable> pending;

    /** Default constructor. Used by {@link ServiceLoader#load(java.lang.Class)}
     * to lookup instance of presenter registered on the classpath of the
     * application.
     */
    public iOS() {
        super(false, true, "iOS", UI.getDefault().identifier());
    }

    /**
     * <p>
     * Configures an instance of {@code WebView} to give you easy access to
     * HTML features from Java. Use this method to embed HTML components
     * into your existing application. By configuring your {@code apple.uikit.UIWebView}
     * (in case of <em>Multi OS Engine</em>) or {@code org.robovm.apple.uikit.UIWebView}
     * (in case of <em>RoboVM</em> version of this library)
     * you can use all the <a href="http://dukescript.com">DukeScript</a>
     * goodies like:
     * </p>
     * <ul>
     * <li><a target="_blank" href="https://dukescript.com/javadoc/charts/">charts</a></li>
     * <li><a target="_blank" href="https://dukescript.com/javadoc/leaflet4j/">maps</a></li>
     * <li><a target="_blank" href="https://dukescript.com/javadoc/canvas/">canvas</a></li>
     * <li><a target="_blank" href="http://bits.netbeans.org/html+java/">UI bindings</a></li>
     * </ul>
     * <p>
     * and still code in the same language that you use for the rest
     * of your application - e.g. in <b>Java</b> or <a href="https://kotlinlang.org/">Kotlin</a>.
     * </p>
     *
     * <p>
     * <b>Accessing the HTML Content</b>
     * <p>
     *
     * There can be multiple {@code WebView} components in your application
     * and it is thus essential to always identify the one you are working with.
     * As such the HTML content of your {@code WebView} needs to be accessed from
     * a dedicated {@link Executor} provided to you as a return value of the
     * {@link #configure(java.lang.String, java.lang.Object, java.lang.String) configure}
     * method. Use {@link Executor#execute(java.lang.Runnable)} to switch
     * into the context of the {@code WebView} and perform an action.
     * <p>
     * Here is an example to display an alert in your {@link WebView view} and
     * its associated {@link Executor executor} created as a result of
     *
     * <pre>
     * {@link Executor} executor = {@link #configure(java.lang.String, java.lang.Object, java.lang.String)  iOS.configure}(null, view, null);
     * {@link Executor executor}.execute(<b>new</b> {@link Runnable} {
     *     <b>public void</b> run() {
     *        alert("Hello World!");
     *     }
     *     {@link JavaScriptBody @JavaScriptBody}(args = { "msg" }, body = "alert(msg);")
     *     <b>native void</b> alert(String msg);
     * });
     * </pre>
     * The example is using {@link JavaScriptBody} annotation (you can read
     * more about this building block of DukeScript system {@link net.java.html.js here}),
     * but in most cases you are likely to use one of the predefined Java wrappers. For example there
     * are <a target="_blank" href="https://dukescript.com/javadoc/libs/net/java/html/lib/jquery/JQuery.html">
     * JQuery bindings</a> which you may consider to use instead.
     *
     * <p>
     * <b>Threading</b>
     * <p>
     *
     * The HTML content of your {@code WebView} needs to be accessed from a single
     * UI thread. The same UI thread is {@link net.java.html.js also making callbacks}
     * to your Java code. To post a task into such thread use the returned
     * {@link Executor}'s {@link iOS#execute(java.lang.Runnable)} method. When
     * inside of the executor you can access the HTML as well as native components
     * of your <b>iOS</b> application.
     * <p>
     * This presenter is licensed under <em>GPLv3</em> license - visit
     * <a target="top" href="https://dukescript.com/index.html#pricing">DukeScript website</a>
     * for commercial licenses and support.
     *
     * @param <WebView> either {@code apple.uikit.UIWebView}
     *   or {@code org.robovm.apple.uikit.UIWebView} depending on
     *   {@linkplain iOS the version of the JVM} you are using
     * @param licenseKey ignored
     * @param view the {@code WebView} to configure and make ready for access from Java code
     * @param page the initial page to load - usually read from assets - e.g. the URL looks like: <code>file:///android_asset/mypage.html</code>
     * @return the executor to use to safely execute code inside of the HTML content of your view
     * @since 1.4
     */
    public static <WebView> Executor configure(String licenseKey, WebView view, String page) {
        iOS presenter = new iOS();
        String foundPage;
        if (page != null) {
            final File pageFile = new File(page);
            if (pageFile.exists()) {
                foundPage = page;
            } else {
                int lastSlash = page.lastIndexOf('/');
                String subdir = lastSlash < 0 ? null : page.substring(0, lastSlash);
                String rest = page.substring(lastSlash + 1);
                int lastDot = rest.lastIndexOf('.');
                String name = lastDot < 0 ? rest : rest.substring(0, lastDot);
                String ext = lastDot < 0 ? "" : rest.substring(lastDot + 1);
                foundPage = UI.getDefault().pathForResouce(name, ext, subdir);
                File foundFile = foundPage == null ? null : new File(foundPage);
                if (foundFile == null || !foundFile.exists()) {
                    throw new IllegalStateException("Cannot find page " + page + " neither as file " + pageFile + " or file " + foundPage);
                }
            }
        } else {
            foundPage = null;
        }
        UI.getDefault().setViewUp(view, foundPage, presenter.new WebViewDelegate(null));
        return presenter;
    }

    @Override
    void log(Level level, String msg, Object... args) {
        if (args.length == 1 && args[0] instanceof Throwable) {
            LOG.log(level, msg, (Throwable) args[0]);
        } else {
            LOG.log(level, msg, args);
        }
    }

    final boolean assignThread(Thread t) {
        boolean assigned = false;
        List<Runnable> tmp = null;
        synchronized (this) {
            if (dispatchExecutor == null) {
                dispatchExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        dispatchThread = new Thread(r, "DukeScript Worker");
                        return dispatchThread;
                    }
                });
                assigned = true;
                tmp = pending;
                pending = null;
            }
        }
        if (tmp != null) {
            for (Runnable r : tmp) {
                dispatchExecutor.execute(r);
            }
        }
        return assigned;
    }

    @Override
    final void dispatch(final Runnable r) {
        synchronized (this) {
            if (dispatchExecutor == null) {
                if (pending == null) {
                    pending = new LinkedList<Runnable>();
                }
                pending.add(r);
                return;
            }
        }

        if (dispatchThread == Thread.currentThread()) {
            r.run();
        } else {
            dispatchExecutor.execute(r);
        }
    }

    /** Executes the command in the {@link BrwsrCtx context} of this presenter.
     * The behavior is specified by {@link BrwsrCtx#execute(java.lang.Runnable)}
     * method - e.g. if we are on the right thread, let's execute the
     * {@code command} synchronously, otherwise pass the command to the queue
     * in the right UI thread and execute it asynchronously later.
     *
     * @param command the runnable to execute
     */
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
                + "  console.log('prompt calling for : ' + url);\n"
                + "  var r = window.prompt(url);\n"
                + "  console.log('prompt got in: ' + url + ' x: ' + r);\n"
                + "  return r;\n"
                + "}\n"
        );
        loadJS(welcome);
        onReady.callbackReady("iOS");
    }

    @Override
    void loadJS(final String js) {
        UI.getDefault().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String res = UI.getDefault().evaluateJavaScript(webView, js);
                LOG.log(Level.FINE, "loadJS done: {0}", res);
            }
        });
    }

    @Override
    void drainQueue() {
        UI.getDefault().drainQueue();
    }

    @Override
    public void displayPage(URL page, Runnable onPageLoad) {
        UI.getDefault().displayPage(page.toExternalForm(), new WebViewDelegate(onPageLoad));
    }

    private final class WebViewDelegate implements UI.WebViewAdapter {

        private final Runnable onPageLoad;

        public WebViewDelegate(Runnable onPageLoad) {
            this.onPageLoad = onPageLoad;
        }
        
        @Override
        public String processInvoke(Object webView, String url) {
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
                    System.err.println("returning " + ret);
                    return ret;
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, "Error processing " + url, ex);
                } 
            }
            return null;   
        }
        
        @Override
        public boolean shouldStartLoad(Object webView, String url) {
            final String pref = "presenter://";
            if (url.startsWith(pref)) {
                String ret = processInvoke(webView, url);
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
                            } else if (ch == '\\') {
                                exec.append("\\\\");
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
                    String check = UI.getDefault().evaluateJavaScript(webView, exec.toString());
                    UI.getDefault().drainQueue();
                    LOG.log(Level.FINE, "evaluating {0} with return {1}", new Object[]{exec, check});
                }
                return false;
            }
            return UI.getDefault().openFileURL(url);
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
        public void didStartLoad(Object webView) {
            iOS.this.webView = webView;
        }

        @Override
        public void didFailLoad(Object webView, String error) {
            iOS.this.webView = webView;
        }

        @Override
        public void didFinishLoad(Object webView) {
            if (assignThread(Thread.currentThread()) && onPageLoad != null) {
                execute(onPageLoad);
            }
        }

    }

}
