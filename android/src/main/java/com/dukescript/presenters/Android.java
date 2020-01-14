package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Presenter for Android - a library from the "DukeScript Presenters" project.
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


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import net.java.html.BrwsrCtx;
import net.java.html.js.JavaScriptBody;
import org.netbeans.html.boot.spi.Fn;
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.presenters.spi.ProtoPresenter;
import org.netbeans.html.presenters.spi.ProtoPresenterBuilder;
import org.netbeans.html.sound.spi.AudioEnvironment;

/** Ultimate <a target="_blank" href="http://dukescript.com">DukeScript</a>
 * <a target="_blank" href="https://github.com/dukescript/dukescript-presenters">Presenter</a>
 * for all Android devices.
 * <p>
 * Use as a {@linkplain #onCreate(android.os.Bundle) launching activity}
 * of your
 * <a target="_blank" href="https://dukescript.com/getting_started.html">portable DukeScript application</a>
 * or configure any {@link WebView} in your (existing) Android application to
 * give you easy access to HTML features (like
 * <a target="_blank" href="https://dukescript.com/javadoc/charts/">charts</a>,
 * <a target="_blank" href="https://dukescript.com/javadoc/leaflet4j/">maps</a>,
 * <a target="_blank" href="https://dukescript.com/javadoc/canvas/">canvas</a> or
 * <a target="_blank" href="https://dukescript.com/documentation.html">more</a>)
 * from Java by using the
 * {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean)}
 * method.
 * <p>
 * To use this presenter specify following dependency:
 * <pre>
 * &lt;dependency&gt;
 *   &lt;groupId&gt;com.dukescript.presenters&lt;/groupId&gt;
 *   &lt;artifactId&gt;android&lt;/artifactId&gt;
 *   &lt;version&gt;<a target="blank" href="http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.dukescript.presenters%22%20AND%20a%3A%22android%22">1.3</a>&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 * <p>
 * <b>Embedding</b>
 * <p>
 * Read more about embedding DukeScript technology into existing
 * Android application. Start at the
 * {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean) configure}
 * method.
 *
 * <p>
 * <b>Cross-Platform</b>
 * <p>
 * Learn how to build fully portable (iOS, browser, desktop) Java applications
 * in <a target="_blank" href="https://dukescript.com/getting_started.html">DukeScript getting
 * started</a> tutorial. Details about configuring Android manifest file
 * can be found in the {@link #onCreate(android.os.Bundle) activity definition} section.
 */
public final class Android extends Activity {
    public Android() {
    }

    /**
     * Configures an instance of {@link WebView} to give you easy access to
     * HTML features from Java. Use this method to embed HTML components
     * into your existing application. By configuring your {@link WebView}
     * you can use all the <a href="http://dukescript.com">DukeScript</a>
     * goodies like:
     * <ul>
     * <li><a target="_blank" href="https://dukescript.com/javadoc/charts/">charts</a></li>
     * <li><a target="_blank" href="https://dukescript.com/javadoc/leaflet4j/">maps</a></li>
     * <li><a target="_blank" href="https://dukescript.com/javadoc/canvas/">canvas</a></li>
     * <li><a target="_blank" href="http://bits.netbeans.org/html+java/">UI bindings</a></li>
     * </ul>
     * and still code in the same language that you use for the rest
     * of your application - e.g. in <b>Java</b> or <a href="https://kotlinlang.org/">Kotlin</a>.
     *
     * <p>
     * <b>Accessing the HTML Content</b>
     * <p>
     *
     * There can be multiple {@link WebView} components in your application
     * and it is thus essential to always identify the one you are working with.
     * As such the HTML content of your {@link WebView} needs to be accessed from
     * a dedicated {@link Executor} provided to you as a return value of the
     * {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean) configure}
     * method. Use {@link Executor#execute(java.lang.Runnable)} to switch
     * into the context of the {@link WebView} and perform an action.
     * <p>
     * Here is an example to display an alert in your {@link WebView view} and
     * its associated {@link Executor executor} created as a result of
     * 
     * <pre>
     * {@link Executor} executor = {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean) Android.configure}(null, view, null, null);
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
     * The HTML content of your {@link WebView} needs to be accessed from a single
     * thread. The same thread is {@link net.java.html.js also making callbacks}
     * to your Java code. To post a task into such thread use the returned
     * {@link Executor}'s {@link Executor#execute(java.lang.Runnable)} method.
     * Those are the basics.
     * <p>
     * Now the tougher part: the thread for accessing the HTML content can be
     * the same as {@linkplain Activity#runOnUiThread(java.lang.Runnable) Android UI thread},
     * but due to
     * <a target="_blank" href="https://chromium.googlesource.com/chromium/src/+/a4b0d4cc0b9780aecd3e23a94293b2e3002baf7e">bug 438255</a>
     * this reliably works only on Android SDK 24 and newer - otherwise it may
     * cause deadlocks.
     * <p>
     * To control whether the {@linkplain Activity#runOnUiThread(java.lang.Runnable) Android UI thread}
     * is the one to make calls into your {@link WebView} or whether one shall use
     * a background thread and access it via returned {@linkplain Executor#execute(java.lang.Runnable) executor's execute}
     * method, one can use the <code>runOnUiThread</code> parameter of the
     * {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean)} method.
     * <p>
     * It is usually more comfortable to set the <code>runOnUiThread</code> parameter to <code>true</code>
     * as one can make direct calls between {@link WebView} HTML content and other Android widgets. Whether
     * that is safe depends on the minimal SDK you are targeting (24 and newer is safe). For now it is probably
     * more useful to stick with verbose:
     * <pre>
     * {@link Executor} executor = {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean) Android.configure}(null, view, null, <b>false</b>);
     * {@link Executor executor}.execute(<b>new</b> {@link Runnable} {
     *     <b>public void</b> run() {
     *        // access the HTML Content of your view
     *        {@link Activity#runOnUiThread(java.lang.Runnable) runOnUiThread}(<b>new</b> {@link Runnable} {
     *          // access native UI of your application
     *        });
     *     }
     * });
     * </pre>
     * <p>
     * This presenter is licensed under <em>GPLv3</em> license - visit
     * <a target="top" href="https://dukescript.com/index.html#pricing">DukeScript website</a>
     * for commercial licenses and support.
     *
     * @param licenseKey ignored
     * @param view the {@link WebView} to configure and make ready for access from Java code
     * @param page the initial page to load - usually read from assets - e.g. the URL looks like: <code>file:///android_asset/mypage.html</code>
     * @param runOnUiThread <code>true</code> to run all the communication with the
     *    HTML content of your view in {@linkplain Activity#runOnUiThread(java.lang.Runnable) Android UI thread},
     *    <code>false</code> to use a background thread (safer on SDK 23 and older), <code>null</code> to use
     *    the default which is the {@linkplain Activity#runOnUiThread(java.lang.Runnable) Android UI thread}
     *    on SDK 24 and newer and background thread otherwise
     * @return the executor to use to safely execute code inside of the HTML content of your view
     * @since 1.3
     */
    public static Executor configure(String licenseKey, final WebView view, String page, Boolean runOnUiThread) {
        String aPkg = view.getContext().getApplicationInfo().packageName;
        final Presenter p = new Presenter(view, aPkg, page, null, null, runOnUiThread);
        androidLog(Level.FINE, "Creating presenter for {0}", view);
        doInit(p, view);
        return p;
    }

    private static void doInit(final Presenter p, final WebView view) {
        p.dispatch(new Runnable() {
            @Override
            public void run() {
                androidLog(Level.FINE, "Initializing presenter for {0}", view);
                p.presenter.displayPage(null, null);
                androidLog(Level.FINE, "Init done for {0}", view);
            }
        }, false);
    }

    /**
     * @param view the view to configure
     * @param page the page to load into the view or <code>null</code>
     * @return presenter which also implements {@link Executor} interface
     * @deprecated Consider using {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean)}
     *    which offers more control over behavior of Android DukeScript presenter.
     * @since 1.0
     */
    @Deprecated
    public static Fn.Presenter create(WebView view, String page) {
        return (Fn.Presenter) configure(null, view, page, null);
    }

    /**
     * Initializes the {@link Activity} by creating a {@link WebView} and
     * setting it up to allow execution of DukeScript application. The
     * configuration is performed in <code>AndroidManifest.xml</code> file.
     * Sample definition looks like:
     * <pre>
&lt;application
    android:allowBackup="true"
    android:icon="@drawable/ic_launcher"
    android:label="dstest"
    android:theme="@android:style/Theme.Black.NoTitleBar.Fullscreen"&gt;
    &lt;activity android:name="<b>com.dukescript.presenters.Android</b>"
              android:configChanges="orientation|screenSize"&gt;
        &lt;intent-filter&gt;
            &lt;action android:name="android.intent.action.MAIN" /&gt;
            &lt;category android:name="android.intent.category.LAUNCHER" /&gt;
        &lt;/intent-filter&gt;
    &lt;/activity&gt;

    &lt;meta-data android:name="<b>loadPage</b>" android:value="file:///android_asset/pages/index.html" /&gt;
    &lt;meta-data android:name="<b>loadClass</b>" android:value="org.apidesign.bck2brwsr.dstest.AndroidMain" /&gt;
    &lt;meta-data android:name="<b>invoke</b>" android:value="main" /&gt;
&lt;/application&gt;
     * </pre>
     * The file specifies fully qualified name of this activity
     * and additional configuration parameters. Configuration section can define:
     * <ul>
     *   <li><b>loadPage</b> - the HTML page to load on start</li>
     *   <li><b>loadClass</b> - the fully qualified name of a class that contains the main initialization method</li>
     *   <li><b>invoke</b> - name of static initialization method in the given class that
     *      will be invoked when the page is loaded and can access the objects
     *      in the specified HTML page</li>
     *   <li><b>runOnUiThread</b> - optional parameter that can be set to <code>true</code> or
     *      <code>false</code> to specify whether to execute the code on standard
     *      UI thread (only possible on SDK 24 and newer) or on a background thread -
     *      {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean) more info}.
     *   </li>
     * </ul>
     * <p>
     * To get more control over the {@link WebView} one can build the scene itself
     * and use
     * {@link #configure(java.lang.String, android.webkit.WebView, java.lang.String, java.lang.Boolean) }
     * factory method to initialize the view(s).
     * <p>
     * This presenter is licensed under <em>GPLv3</em> license - visit
     * <a target="top" href="https://dukescript.com/index.html#pricing">DukeScript website</a>
     * for commercial licenses and support.
     *
     * @param savedInstanceState the state of the bundle, unused so far
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        setContentView(webView);
        String aPkg = getPackageName();

        ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo(aPkg, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException ex) {
            androidLog(Level.SEVERE, null, ex);
            ai = null;
        }

        String loadPage = null;
        Class<?> loadClass = null;
        String invoke = null;
        Boolean runOnUiThread = null;
        if (ai != null) {
            Bundle bundle = ai.metaData;
            if (bundle != null) {
                loadPage = bundle.getString("loadPage");
                try {
                    String cn = bundle.getString("loadClass");
                    if (cn != null) {
                        loadClass = Class.forName(cn);
                    }
                } catch (ClassNotFoundException ex) {
                    androidLog(Level.SEVERE, null, ex);
                }
                invoke = bundle.getString("invoke");
                if ("true".equals(bundle.getString("runOnUiThread"))) {
                    runOnUiThread = true;
                }
                if ("false".equals(bundle.getString("runOnUiThread"))) {
                    runOnUiThread = false;
                }
            }
        }
        if (loadPage == null || loadClass == null || invoke == null) {
            throw new IllegalStateException("Cannot find meta-data ('loadPage', 'loadClass', 'invoke') in " + ai);
        }
        Presenter presenter = new Presenter(webView, aPkg, loadPage, loadClass, invoke, runOnUiThread);
        presenter.execute(presenter);
        doInit(presenter, webView);
    }

    static void androidLog(Level severity, String msg, Object... args) {
        int priority = Log.WARN;
        if (severity == Level.FINEST) {
            priority = Log.DEBUG;
        }
        if (severity == Level.FINE || severity == Level.FINER) {
            priority = Log.VERBOSE;
        }
        if (severity == Level.INFO) {
            priority = Log.INFO;
        }
        if (severity == Level.SEVERE) {
            priority = Log.ERROR;
        }
        if (!Log.isLoggable("Presenter", priority)) {
            return;
        }
        if (args.length == 1 && args[0] instanceof Throwable) {
            Log.w("Presenter", msg, (Throwable)args[0]);
        } else {
            for (int i = 0; i < args.length; i++) {
                msg = msg.replace("{" + i + "}", Objects.toString(args[i]));
            }
            Log.println(priority, "Presenter", msg);
        }
    }

    private static final class Presenter extends Object
    implements Executor, Runnable, ProtoPresenterBuilder.Displayer,
    ProtoPresenterBuilder.Evaluator, ProtoPresenterBuilder.Preparator, ProtoPresenterBuilder.Logger {
        final WebView view;
        final Chrome chrome;
        final JVM jvm;
        final String page;
        final boolean runOnUiThread;
        final BrwsrCtx ctx;
        final Class<?> loadClass;
        String invoke;
        final ProtoPresenter presenter;

        Presenter(final WebView view, String app, String page, Class<?> loadClass, String invoke, Boolean runOnUiThread) {
            this.view = view;
            this.page = page;
            this.loadClass = loadClass;
            this.invoke = invoke;
            this.runOnUiThread = runOnUiThread == null ? Build.VERSION.SDK_INT >= 24 : runOnUiThread;
            this.chrome = new Chrome();
            this.jvm = new JVM(this);
            final Class<? extends Object> ctxClass = loadClass != null ? loadClass : Android.class;
            final Presenter p = Presenter.this;
            final Executor e = new Executor() {
                @Override
                public void execute(final Runnable command) {
                    class CtxRun implements Runnable {

                        @Override
                        public void run() {
                            Closeable c = Fn.activate(Presenter.this.presenter);
                            try {
                                command.run();
                            } finally {
                                try {
                                    c.close();
                                } catch (IOException ex) {
                                    androidLog(Level.SEVERE, null, ex);
                                }
                            }
                        }

                        @Override
                        public String toString() {
                            return "CtxRun:" + command.toString();
                        }
                    }
                    p.dispatch(new CtxRun());
                }
            };

            this.presenter = ProtoPresenterBuilder.newBuilder().
                type("Android").
                app(app).
                dispatcher(e, true).
                displayer(this).
                preparator(this, true).
                loadJavaScript(this, false).
                logger(this).
                build();

            final Audio audio = new Audio(view.getContext(), page);

            Contexts.Builder cb = Contexts.newBuilder();
            Contexts.fillInByProviders(ctxClass, cb);
            cb.register(Context.class, view.getContext(), 100);
            cb.register(Fn.Presenter.class, this.presenter, 100);
            cb.register(Executor.class, e, 100);
            cb.register(AudioEnvironment.class, audio, 100);
            this.ctx = cb.build();
            allowFileAccessFromFiles(view);
            allowUnversalAccessFromFiles(view);
            view.getSettings().setDomStorageEnabled(true);
            view.getSettings().setGeolocationEnabled(true);
            view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            view.setWebChromeClient(chrome);
            view.getSettings().setJavaScriptEnabled(true);
            view.addJavascriptInterface(jvm, "jvm");
        }

        @Override
        public void eval(String js) {
            loadScript("javascript:" + js);
        }

        @Override
        public void displayPage(URL page, final Runnable onPageLoad) {
            view.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    onPageLoad.run();
                    androidLog(Level.FINE, "onPageFinished {0}", url);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    androidLog(Level.FINE, "onPageStarted {0}", url);
                }

                @Override
                public void onLoadResource(WebView view, String url) {
                    androidLog(Level.FINE, "onLoadResource {0}", url);
                }
            });
            androidLog(Level.FINE, "displayPage {0}", page.toExternalForm());
            view.loadUrl(page.toExternalForm());
        }

        private void loadScript(final String js) {
            final String pref = "javascript:";
            assert js.startsWith(pref);
            Activity a = (Activity) view.getContext();
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < 19 /* KITKAT */) {
                        view.loadUrl(js);
                    } else {
                        view.evaluateJavascript(js.substring(pref.length()), null);
                    }
                }
            });
        }

        private static Level findLevel(int priority) {
            if (priority >= Level.SEVERE.intValue()) {
                return Level.SEVERE;
            }
            if (priority >= Level.WARNING.intValue()) {
                return Level.WARNING;
            }
            if (priority >= Level.INFO.intValue()) {
                return Level.INFO;
            }
            return Level.FINE;
        }

        @Override
        public void log(int level, String msg, Object... args) {
            androidLog(findLevel(level), msg, args);
        }

        protected final void dispatch(Runnable r) {
            if (jvm.dispatch(r)) {
                Activity a = (Activity) view.getContext();
                dispatch(jvm, false);
            }
        }

        @Override
        public final void execute(final Runnable command) {
            ctx.execute(command);
        }

        @Override
        public void prepare(final ProtoPresenterBuilder.OnPrepared onReady) {
            class LoadPage extends WebViewClient implements Runnable {
                private final Runnable initialize;

                LoadPage(Runnable initialize) {
                    this.initialize = initialize;
                }

                @Override
                public void run() {
                    view.setWebViewClient(this);
                    if (page != null) {
                        androidLog(Level.FINE, "loading page {0}", page);
                        view.loadUrl(page);
                    } else {
                        androidLog(Level.FINE, "loading empty page");
                        view.loadDataWithBaseURL("file:///", "<html><body><script></script></body></html>", "text/html", null, null);
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    dispatch(initialize, false);
                }
            }
            class InitializeJSBridge implements Runnable {
                private final Runnable initialize;

                InitializeJSBridge(Runnable initialize) {
                    this.initialize = initialize;
                }

                @Override
                public void run() {
                    androidLog(Level.FINE, "calling jvm.ready()");
                    loadScript("javascript:jvm.ready();\n");
                    Thread thread = new Thread(initialize, "Initialize JS Bridge");
                    thread.start();
                }
            }
            class Ready implements Runnable {
                private boolean done;

                @Override
                public void run() {
                    if (done) {
                        return;
                    }
                    androidLog(Level.FINE, "awaiting jvm.ready()");
                    try {
                        jvm.ready.await();
                    } catch (InterruptedException ex) {
                        throw new IllegalStateException(ex);
                    }
                    loadScript("javascript:(function(global) {\n"
                            + "  var jvm = global.jvm;\n"
                            + "  global.androidCB = function(m,a1,a2,a3,a4) {\n"
                            + "    return jvm.invoke(m,a1,a2,a3,a4);\n"
                            + "  }\n"
                            + "  global.alert = function(msg) { jvm.invoke('alert', msg, null, null, null); };"
                            + "  global.confirm = function(msg) {\n"
                            + "    var ret = jvm.invoke('confirm', msg, null, null, null);\n"
                            //                + "    alert('val: ' + ret + ' typeof: ' + typeof ret);\n"
                            + "    return 'true' == ret;"
                            + "  };"
                            + "  global.prompt = function(msg, val) {\n"
                            + "    var ret = jvm.invoke('prompt', msg, val, null, null);\n"
                            //                + "    alert('val: ' + ret + ' typeof: ' + typeof ret);\n"
                            + "    return ret;"
                            + "  };"
                            + "})(this);\n"
                    );
                    done = true;
                    onReady.callbackIsPrepared("androidCB");
                    dispatch(jvm, true);
                }
            }
            Activity a = (Activity) view.getContext();
            a.runOnUiThread(new LoadPage(new InitializeJSBridge(new Ready())));
        }

        @Override
        public void run() {
            if (loadClass == null) {
                return;
            }
            ctx.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        invokeOnPageLoad(Presenter.this.presenter, view.getContext(), loadClass, invoke);
                    } catch (Exception ex) {
                        androidLog(Level.SEVERE, null, ex);
                    }
                }
            });
        }

        private static Executor DISPATCH;

        final void dispatch(Runnable runnable, boolean delayed) {
            if (!runOnUiThread) {
                if (DISPATCH == null) {
                    DISPATCH = Executors.newSingleThreadExecutor(jvm);
                }
                if (!delayed && jvm.isBrowserDispatchThread()) {
                    runnable.run();
                } else {
                    DISPATCH.execute(runnable);
                }
            } else {
                if (delayed) {
                    view.post(runnable);
                } else {
                    Activity a = (Activity) view.getContext();
                    a.runOnUiThread(runnable);
                }
            }
        }
    }

    static void invokeOnPageLoad(Fn.Presenter presenter, Context context, Class<?> clazz, final String method) throws Exception {
        Closeable c = Fn.activate(presenter);
        try {
            for (Method m : clazz.getMethods()) {
                if (m.getName().equals(method)) {
                    final Class<?>[] params = m.getParameterTypes();
                    if (params.length == 0) {
                        m.invoke(null);
                        return;
                    }
                    if (params.length == 1) {
                        if (params[0] == String[].class) {
                            m.invoke(null, (Object) new String[0]);
                            return;
                        }
                        if (Context.class.isAssignableFrom(params[0]) || params[0].isInstance(context)) {
                            m.invoke(null, context);
                            return;
                        }
                    }
                    throw new IllegalStateException("Cannot call " + m + " wrong arguments");
                }
            }
        } finally {
            c.close();
        }
    }


    private static void allowUnversalAccessFromFiles(WebView v) {
        try {
            v.getSettings().setAllowUniversalAccessFromFileURLs(true);
        } catch (LinkageError err) {
            // ignore, probably older version then API 16
        }
    }

    private static void allowFileAccessFromFiles(WebView v) {
        try {
            v.getSettings().setAllowFileAccessFromFileURLs(true);
        } catch (LinkageError err) {
            // ignore, probably older version then API 16
        }
    }

    private static URI findResourceURL(Context ctx, URI uri, String suffix) {
        if (uri == null) {
            return uri;
        }
        String resource = uri.toString();
        if (suffix != null) {
            int lastDot = resource.lastIndexOf('.');
            if (lastDot != -1) {
                resource = resource.substring(0, lastDot) + suffix + resource.substring(lastDot);
            } else {
                resource = resource + suffix;
            }
        }

        try {
            URL u = new URL(resource);
            if (suffix == null || isReal(ctx, u)) {
                return u.toURI();
            }
        } catch (MalformedURLException ex) {
            androidLog(Level.WARNING, resource, ex);
        } catch (URISyntaxException ex) {
            androidLog(Level.WARNING, resource, ex);
        }
        return null;
    }

    static URI findLocalizedResourceURL(Context ctx, URI uri, Locale l) {
        URI url = null;
        if (l != null) {
            url = findResourceURL(ctx, uri, "_" + l.getLanguage() + "_" + l.getCountry());
            if (url != null) {
                return url;
            }
            url = findResourceURL(ctx, uri, "_" + l.getLanguage());
        }
        if (url != null) {
            return url;
        }
        return findResourceURL(ctx, uri, null);
    }

    private static boolean isReal(Context ctx, URL u) {
        String str = u.toExternalForm();
        final String assetPrefix = "file:///android_asset/"; // NOI18N
        final String assetPrefix2 = "file:/android_asset/"; // NOI18N
        if (str.startsWith(assetPrefix)) {
            try {
                InputStream fd = ctx.getAssets().open(str.substring(assetPrefix.length()));
                fd.close();
                return true;
            } catch (IOException ex) {
                androidLog(Level.FINE, null, ex);
                return false;
            }
        } else if (str.startsWith(assetPrefix2)) {
            try {
                InputStream fd = ctx.getAssets().open(str.substring(assetPrefix2.length()));
                fd.close();
                return true;
            } catch (IOException ex) {
                androidLog(Level.FINE, null, ex);
                return false;
            }
        }
        try {
            URLConnection conn = u.openConnection();
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection hc = (HttpURLConnection) conn;
                hc.setReadTimeout(5000);
                if (hc.getResponseCode() >= 300) {
                    throw new IOException("Wrong code: " + hc.getResponseCode());
                }
            }
            InputStream is = conn.getInputStream();
            is.close();
            androidLog(Level.FINE, "found real url: {0}", u);
            return true;
        } catch (IOException ignore) {
            androidLog(Level.FINE, "Cannot open " + u, ignore);
            return false;
        }
    }

    private static final class AlertHandler extends Handler
            implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener {

        private final boolean[] clicked;

        public AlertHandler(boolean[] clicked) {
            this.clicked = clicked;
        }

        @Override
        public void handleMessage(Message msg) {
            throw new IllegalStateException();
        }

        @Override
        public void onClick(DialogInterface di, int i) {
            if (clicked != null) {
                clicked[0] = true;
            }
            sendMessage(obtainMessage());
        }

        @Override
        public void onCancel(DialogInterface di) {
            sendMessage(obtainMessage());
        }
    }

    static final class Audio implements AudioEnvironment<MediaPlayer> {
        private final String baseUrl;
        private final Context ctx;

        public Audio(Context ctx, String baseUrl) {
            this.ctx = ctx;
            this.baseUrl = baseUrl;
        }

        @Override
        public MediaPlayer create(String url) {
            MediaPlayer mp;
            try {
                mp = new MediaPlayer();
            } catch (Exception ex) {
                return null;
            }
            try {
                URI base = new URI(baseUrl);
                URI full = base.resolve(url);
                final String str = full.toString();
                final String assetPrefix = "file:///android_asset/"; // NOI18N
                final String assetPrefix2 = "file:/android_asset/"; // NOI18N
                if (str.startsWith(assetPrefix)) {
                    AssetFileDescriptor fd = ctx.getAssets().openFd(str.substring(assetPrefix.length()));
                    mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                } else if (str.startsWith(assetPrefix2)) {
                    AssetFileDescriptor fd = ctx.getAssets().openFd(str.substring(assetPrefix2.length()));
                    mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                } else {
                    mp.setDataSource(ctx, Uri.parse(str));
                }
                mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                return mp;
            } catch (Exception ex) {
                androidLog(Level.WARNING, "Cannot initialize " + url, ex);
                return null;
            }
        }

        @Override
        public void play(MediaPlayer mp) {
            mp.prepareAsync();
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
        }

        @Override
        public void pause(MediaPlayer mp) {
            mp.pause();
        }

        @Override
        public void setVolume(MediaPlayer mp, double d) {
            mp.setVolume((float) d, (float) d);
        }

        @Override
        public boolean isSupported(MediaPlayer mp) {
            return true;
        }
    }

    private static final class Chrome extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    private static final class JVM implements Runnable, ThreadFactory {
        private final CountDownLatch ready;
        private final Presenter presenter;
        private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
        private Thread thread;

        JVM(Presenter p) {
            this.presenter = p;
            this.ready = new CountDownLatch(1);
        }

        boolean dispatch(Runnable r) {
            queue.add(r);
            if (ready.getCount() > 0) {
                androidLog(Level.FINE, "dispatch queued {0}", r);
                return false;
            }
            return true;
        }

        @Override
        public void run() {
            for (;;) {
                Runnable r = queue.poll();
                if (r == null) {
                    return;
                }
                r.run();
            }
        }

        @JavascriptInterface
        public void ready() {
            ready.countDown();
            androidLog(Level.FINE, "ready set to done. Queue: {0}", queue);
        }

        @JavascriptInterface
        public String invoke(String method, String a1, String a2, String a3, String a4) throws Exception {
            if ("alert".equals(method)) { // NOI18N
                AlertHandler h = new AlertHandler(null);
                AlertDialog show = new AlertDialog.Builder(presenter.view.getContext())
                        .setMessage(a1)
                        .setNeutralButton(android.R.string.ok, h)
                        .setOnCancelListener(h)
                        .show();
                try {
                    Looper.loop();
                } catch (RuntimeException ex) {
                }
                return null;
            }
            if ("confirm".equals(method)) { // NOI18N
                final boolean[] res = {false};
                AlertHandler ok = new AlertHandler(res);
                AlertHandler h = new AlertHandler(null);
                AlertDialog show = new AlertDialog.Builder(presenter.view.getContext())
                        .setMessage(a1)
                        .setPositiveButton(android.R.string.ok, ok)
                        .setNegativeButton(android.R.string.cancel, h)
                        .setOnCancelListener(h)
                        .show();
                try {
                    Looper.loop();
                } catch (RuntimeException ex) {
                }
                return res[0] ? "true" : "false"; // NOI18N
            }
            if ("prompt".equals(method)) { // NOI18N
                final boolean[] res = {false};
                final EditText line = new EditText(presenter.view.getContext());
                if (a2 != null) {
                    line.setText(a2);
                } else {
                    line.setText("");
                }
                AlertHandler ok = new AlertHandler(res);
                AlertHandler h = new AlertHandler(null);
                AlertDialog show = new AlertDialog.Builder(presenter.view.getContext())
                        .setMessage(a1)
                        .setView(line)
                        .setPositiveButton(android.R.string.ok, ok)
                        .setNegativeButton(android.R.string.cancel, h)
                        .setOnCancelListener(h)
                        .show();
                try {
                    Looper.loop();
                } catch (RuntimeException ex) {
                }
                return res[0] ? line.getText().toString() : null;
            }
            ProtoPresenter cb = presenter.presenter;
            return cb.js2java(method, a1, a2, a3, a4);
        }

        @Override
        public Thread newThread(Runnable r) {
            assert thread == null;
            thread = new Thread(r, "Browser Dispatch Thread");
            return thread;
        }

        boolean isBrowserDispatchThread() {
            return Thread.currentThread() == thread;
        }
    }
}

