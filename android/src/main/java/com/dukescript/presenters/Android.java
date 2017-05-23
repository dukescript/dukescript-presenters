package com.dukescript.presenters;

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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.html.BrwsrCtx;
import org.netbeans.html.boot.spi.Fn;
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.sound.spi.AudioEnvironment;

public final class Android extends Activity {
    private static final Logger LOG = Logger.getLogger(Android.class.getName());
    
    public Android() {
    }

    public static Fn.Presenter create(WebView view, String page) {
        String aPkg = view.getContext().getApplicationInfo().packageName;
        final Presenter p = new Presenter(view, aPkg, page, null, null);
        p.dispatch(new Runnable() {
            @Override
            public void run() {
                p.init();
            }
        }, false);
        return p;
    }
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WebView webView = new WebView(this);
        setContentView(webView);
        String aPkg = getPackageName();

        ApplicationInfo ai;
        try {
            ai = getPackageManager().getApplicationInfo(aPkg, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
            ai = null;
        }

        String loadPage = null;
        Class<?> loadClass = null;
        String invoke = null;
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
                    LOG.log(Level.SEVERE, null, ex);
                }
                invoke = bundle.getString("invoke");
            }
        }
        if (loadPage == null || loadClass == null || invoke == null) {
            throw new IllegalStateException("Cannot find meta-data ('loadPage', 'loadClass', 'invoke') in " + ai);
        }
        Presenter presenter = new Presenter(webView, aPkg, loadPage, loadClass, invoke);
        presenter.execute(presenter);
    }
    private static final class Presenter extends Generic implements Executor, Runnable {
        final WebView view;
        final Chrome chrome;
        final JVM jvm;
        final String page;
        Class<?> loadClass;
        String invoke;
        BrwsrCtx ctx;

        Presenter(WebView view, String app, String page, Class<?> loadClass, String invoke) {
            super(false, true, "Android", app);
            this.view = view;
            this.page = page;
            this.loadClass = loadClass;
            this.invoke = invoke;
            this.chrome = new Chrome();
            this.jvm = new JVM(this);
            allowFileAccessFromFiles(view);
            allowUnversalAccessFromFiles(view);
            view.getSettings().setJavaScriptEnabled(true);
            view.getSettings().setDomStorageEnabled(true);
            view.getSettings().setGeolocationEnabled(true);
            view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            view.setWebChromeClient(chrome);
            view.addJavascriptInterface(jvm, "jvm");
        }

        @Override
        protected void loadJS(String js) {
            loadScript("javascript:" + js);
        }

        @Override
        public void displayPage(URL page, final Runnable onPageLoad) {
            view.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    onPageLoad.run();
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                }
            });
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

        @Override
        protected void log(Level level, String msg, Object... args) {
            if (args.length == 1 && args[0] instanceof Throwable) {
                LOG.log(level, msg, (Throwable)args[0]);
            } else {
                LOG.log(level, msg, args);
            }
        }

        @Override
        protected final void dispatch(Runnable r) {
            if (jvm.dispatch(r)) {
                Activity a = (Activity) view.getContext();
                dispatch(jvm, false);
            }
        }

        @Override
        public final void execute(final Runnable command) {
            class CtxRun implements Runnable {
                @Override
                public void run() {
                    Closeable c = Fn.activate(Presenter.this);
                    try {
                        command.run();
                    } finally {
                        try {
                            c.close();
                        } catch (IOException ex) {
                            Presenter.this.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            dispatch(new CtxRun());
        }

        @Override
        void callbackFn(final String welcome, final OnReady onReady) {
            class Ready implements Runnable {
                boolean subsequent;
                @Override
                public void run() {
                    if (!subsequent) {
                        subsequent = true;
                        if (page != null) {
                            view.loadUrl(page);
                        } else {
                            view.loadDataWithBaseURL("file:///", "<html><body><script></script></body></html>", "text/html", null, null);
                        }
                    }

                    if (!jvm.ready) {
                        loadScript("javascript:try {\n"
                                + "  jvm.ready();\n"
                                + "} catch (e) {\n"
                                + "  alert('jvm' + Object.getOwnPropertyNames(jvm) + ' ready: ' + jvm.ready);\n"
                                + "}");
                        view.postDelayed(this, 10);
                    } else {
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
                                + "})(this);\n" + welcome
                        );
                        dispatch(new Runnable() {
                            @Override
                            public void run() {
                                onReady.callbackReady("androidCB");
                                jvm.run();
                            }
                        }, true);
                    }
                }
            }
            Activity a = (Activity) view.getContext();
            a.runOnUiThread(new Ready());
        }

        @Override
        public void run() {
            if (loadClass == null) {
                return;
            }
            if (ctx == null) {
                Contexts.Builder cb = Contexts.newBuilder();
                Contexts.fillInByProviders(loadClass, cb);
                cb.register(Fn.Presenter.class, this, 100);
                cb.register(Executor.class, (Executor) this, 100);
                cb.register(AudioEnvironment.class, new Audio(view.getContext(), page), 100);
                ctx = cb.build();
                ctx.execute(this);
                return;
            }
            try {
                invokeOnPageLoad(this, view.getContext(), loadClass, invoke);
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }

        private static Executor DISPATCH;

        final void dispatch(Runnable runnable, boolean delayed) {
            if (Build.VERSION.SDK_INT < 24) {
                if (DISPATCH == null) {
                    DISPATCH = Executors.newSingleThreadExecutor(jvm);
                }
                if (!delayed && Thread.currentThread() == DISPATCH) {
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
            LOG.log(Level.WARNING, resource, ex);
        } catch (URISyntaxException ex) {
            LOG.log(Level.WARNING, resource, ex);
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
                LOG.log(Level.FINE, null, ex);
                return false;
            }
        } else if (str.startsWith(assetPrefix2)) {
            try {
                InputStream fd = ctx.getAssets().open(str.substring(assetPrefix2.length()));
                fd.close();
                return true;
            } catch (IOException ex) {
                LOG.log(Level.FINE, null, ex);
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
            LOG.log(Level.FINE, "found real url: {0}", u);
            return true;
        } catch (IOException ignore) {
            LOG.log(Level.FINE, "Cannot open " + u, ignore);
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
            if (Fn.activePresenter() == null) {
                throw new IllegalStateException("Initialize providers in an active presenter!");
            }
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
                LOG.log(Level.WARNING, "Cannot initialize " + url, ex);
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
        volatile boolean ready;
        private final Presenter presenter;
        private final LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

        JVM(Presenter p) {
            this.presenter = p;
        }

        boolean dispatch(Runnable r) {
            queue.add(r);
            return ready;
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
            ready = true;
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
            return presenter.callback(method, a1, a2, a3, a4);
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Browser Dispatch Thread");
        }
    }
}

