package com.dukescript.presenters.webkit;

/*
 * #%L
 * WebKit Presenter - a library from the "DukeScript Presenters" project.
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


import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.io.Closeable;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.html.boot.spi.Fn;

public final class GTK implements Callback, WebKitPresenter.Shell {
    private final WebKitPresenter presenter;
    private final boolean headless;
    private final boolean windows;
    private final JSC jsc;
    private final GLib glib;
    private final G g;
    private final Gdk gdk;
    private final WebKit webKit;
    
    private OnDestroy onDestroy;
    private OnLoad onLoad;
    private Pending pending;
    private String page;
    
    GTK(WebKitPresenter p, boolean hl, boolean windows) {
        this.presenter = p;
        this.headless = hl;
        this.windows = windows;

        if (windows) {
            loadDLLs();
        }
        
        this.jsc = loadLibrary(JSC.class, true);
        this.g = loadLibrary(G.class, false);
        this.glib = loadLibrary(GLib.class, false);
        this.gdk = loadLibrary(Gdk.class, false);
        this.webKit = loadLibrary(WebKit.class, false);
    }

    private <T> T loadLibrary(Class<T> type, boolean allowObjects) {
        String libName = System.getProperty("com.dukescript.presenters.webkit." + type.getSimpleName());
        if (libName == null) {
            if (windows) {
                if (type == JSC.class) {
                    libName = "javascriptcoregtk-3.0-0";
                } else if (type == GLib.class) {
                    libName = "glib-2.0-0";
                } else if (type == G.class) {
                    libName = "gobject-2.0-0";
                } else if (type == Gdk.class) {
                    libName = "libgdk-3-0";
                } else if (type == Gtk.class) {
                    libName = "gtk-3-0";
                } else if (type == WebKit.class) {
                    libName = "libwebkitgtk-3.0-0";
                }
            } else {
                if (type == JSC.class) {
                    libName = "javascriptcoregtk-3.0";
                } else if (type == GLib.class) {
                    libName = "glib-2.0";
                } else if (type == G.class) {
                    libName = "gobject-2.0";
                } else if (type == Gdk.class) {
                    libName = "gtk-3";
                } else if (type == Gtk.class) {
                    libName = "gtk-3";
                } else if (type == WebKit.class) {
                    libName = "webkitgtk-3.0";
                }
            }
        }
        
        Object lib = Native.loadLibrary(libName, type, 
            Collections.singletonMap(Library.OPTION_ALLOW_OBJECTS, allowObjects)
        );
        return type.cast(lib);
    }
    
    private void loadDLLs() throws IllegalStateException {
        loadDLL("icudata53.dll");
        loadDLL("libffi-6.dll");
        try {
            loadDLL("libgcc_s_seh-1.dll"); // 64-bits
        } catch (UnsatisfiedLinkError err) {
            loadDLL("libgcc_s_sjlj-1.dll"); // 32-bits
        }
        loadDLL("libdbus-1-3.dll");
        loadDLL("libintl-8.dll");
        loadDLL("libjpeg-8.dll");
        loadDLL("liblzma-5.dll");
        loadDLL("libpixman-1-0.dll");
        loadDLL("libsqlite3-0.dll");
        loadDLL("libstdc++-6.dll");
        loadDLL("libwinpthread-1.dll");
        loadDLL("zlib1.dll");
        loadDLL("icuuc53.dll");
        loadDLL("libfreetype-6.dll");
        loadDLL("libglib-2.0-0.dll");
        loadDLL("libgmodule-2.0-0.dll");
        loadDLL("libgobject-2.0-0.dll");
        loadDLL("libgstfft-0.10-0.dll");
        loadDLL("libgstsdp-0.10-0.dll");
        loadDLL("libharfbuzz-0.dll");
        loadDLL("libhunspell-1.3-0.dll");
        loadDLL("libjasper-1.dll");
        loadDLL("libpango-1.0-0.dll");
        loadDLL("libpangowin32-1.0-0.dll");
        loadDLL("libpng16-16.dll");
        loadDLL("libtiff-5.dll");
        loadDLL("libxml2-2.dll");
        loadDLL("libxslt-1.dll");
        loadDLL("icui18n53.dll");
        loadDLL("icuio53.dll");
        loadDLL("icule53.dll");
        loadDLL("iculx53.dll");
        loadDLL("icutu53.dll");
        loadDLL("libatk-1.0-0.dll");
        loadDLL("libenchant.dll");
        loadDLL("libexslt-0.dll");
        loadDLL("libfontconfig-1.dll");
        loadDLL("libgio-2.0-0.dll");
        loadDLL("libgstreamer-0.10-0.dll");
        loadDLL("libgstrtsp-0.10-0.dll");
        loadDLL("libjavascriptcoregtk-3.0-0.dll");
        loadDLL("libpangoft2-1.0-0.dll");
        loadDLL("libsoup-2.4-1.dll");
        loadDLL("icutest53.dll");
        loadDLL("libcairo-2.dll");
        loadDLL("libcairo-gobject-2.dll");
        loadDLL("libdbus-glib-1-2.dll");
        loadDLL("libgdk_pixbuf-2.0-0.dll");
        loadDLL("libgeoclue-0.dll");
        loadDLL("libgstbase-0.10-0.dll");
        loadDLL("libgstcontroller-0.10-0.dll");
        loadDLL("libgstdataprotocol-0.10-0.dll");
        loadDLL("libgstinterfaces-0.10-0.dll");
        loadDLL("libgstnet-0.10-0.dll");
        loadDLL("libgstnetbuffer-0.10-0.dll");
        loadDLL("libgstpbutils-0.10-0.dll");
        loadDLL("libgstrtp-0.10-0.dll");
        loadDLL("libgsttag-0.10-0.dll");
        loadDLL("libgstvideo-0.10-0.dll");
        loadDLL("libpangocairo-1.0-0.dll");
        loadDLL("libgdk-3-0.dll");
        loadDLL("libgstapp-0.10-0.dll");
        loadDLL("libgstaudio-0.10-0.dll");
        loadDLL("libgstcdda-0.10-0.dll");
        loadDLL("libgstriff-0.10-0.dll");
        loadDLL("libgtk-3-0.dll");
        loadDLL("libgailutil-3-0.dll");
        loadDLL("libwebkitgtk-3.0-0.dll");
    }
    
    private void loadDLL(final String name) throws IllegalStateException {
        File dir;
        try {
            dir = new File(GTK.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
        File mingw = new File(dir, "mingw");
        File bin = new File(mingw, "bin");

        File lib = new File(bin, name);
        UnsatisfiedLinkError err;
        if (lib.exists()) {
            try {
                System.load(lib.getPath());
                return;
            } catch (UnsatisfiedLinkError ue) {
                err = ue;
            }
        } else {
            err = new UnsatisfiedLinkError("No lib found: " + lib);
        }
        throw err;
    }
    
    @Override
    public JSC jsc() {
        return jsc;
    }

    public interface GLib extends Library {
        void g_idle_add(Callback r, Pointer p);
    }
    public interface G extends Library {
        void g_signal_connect_data(Pointer obj, String signal, Callback callback, Pointer data);
    }

    public interface Gtk extends Library {
        void gtk_init(int cnt, String[] args);

        Pointer gtk_window_new(int windowType);

        Pointer gtk_scrolled_window_new(Pointer ignore, Pointer ignore2);

        void gtk_window_set_default_size(Pointer window, int width, int height);

        void gtk_window_set_title(Pointer window, String title);

        void gtk_widget_show_all(Pointer window);

        void gtk_window_set_gravity(Pointer window, int gravity);

        void gtk_window_move(Pointer window, int x, int y);

        void gtk_container_add(Pointer container, Pointer child);

        void gtk_widget_grab_focus(Pointer window);

        void gtk_widget_destroy(Pointer window);

        void gtk_main();

        void gtk_main_quit();
    }

    public interface Gdk extends Library {
        int gdk_screen_get_primary_monitor(Pointer screen);

        Pointer gdk_screen_get_default();

        void gdk_screen_get_monitor_geometry(Pointer screen, int monitor, GRectangle geometry);
    }
    
    public interface WebKit extends Library {
        Pointer webkit_web_view_new();

        void webkit_web_view_load_uri(Pointer webView, String url);

        Pointer webkit_web_page_frame_get_javascript_context_for_script_world(Pointer webFrame, Pointer world);

        int webkit_web_view_get_load_status(Pointer webView);

        Pointer webkit_web_view_get_main_frame(Pointer webView);

        Pointer webkit_web_frame_get_global_context(Pointer webFrame);

        String webkit_web_frame_get_title(Pointer webFrame);
    }


    public static class GRectangle extends Structure {

        public int x, y;
        public int width, height;

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("x", "y", "width", "height");
        }
    }
    private static Gtk INSTANCE;

    private Gtk getInstance(boolean[] initialized) {
        synchronized (GTK.class) {
            if (INSTANCE == null) {
                INSTANCE = loadLibrary(Gtk.class, false);
                initialized[0] = true;
            }
        }
        return INSTANCE;
    }

    @Override
    public void doShow(String url) {
        this.page = url;
        boolean[] justInitialized = {false};
        final Gtk gtk = getInstance(justInitialized);
        if (justInitialized[0]) {
            gtk.gtk_init(0, null);
            callback();
            gtk.gtk_main();
        } else {
            glib.g_idle_add(this, null);
        }
    }

    public void callback() {
        final Gtk gtk = getInstance(null);
        
        
        final Pointer screen = gdk.gdk_screen_get_default();
        int primaryMonitor = gdk.gdk_screen_get_primary_monitor(screen);
        GRectangle size = new GRectangle();
        gdk.gdk_screen_get_monitor_geometry(screen, primaryMonitor, size);
        int height = (int) (size.height * 0.9);
        int width = (int) (size.width * 0.9);
        int x = (int) (size.width * 0.05) + size.x;
        int y = (int) (size.height * 0.05) + size.y;

        final Pointer window = gtk.gtk_window_new(0);
        gtk.gtk_window_set_default_size(window, width, height);
        gtk.gtk_window_set_gravity(window, 5);
        gtk.gtk_window_move(window, x, y);

        Pointer scroll = gtk.gtk_scrolled_window_new(null, null);
        gtk.gtk_container_add(window, scroll);

        final Pointer webView = webKit.webkit_web_view_new();
        gtk.gtk_container_add(scroll, webView);
        Pointer frame = webKit.webkit_web_view_get_main_frame(webView);
        Pointer ctx = webKit.webkit_web_frame_get_global_context(frame);
        presenter.jsContext(ctx);
        onLoad = new OnLoad(webView, gtk, window);
        g.g_signal_connect_data(webView, "notify::load-status", onLoad, null);

        webKit.webkit_web_view_load_uri(webView, page);

        gtk.gtk_widget_grab_focus(webView);

        onDestroy = new OnDestroy();
        g.g_signal_connect_data(window, "destroy", onDestroy, null);
        pending = new Pending();
        if (!headless) {
            gtk.gtk_widget_show_all(window);
        }
    }

    private class OnLoad implements Callback {

        private final Pointer webView;
        private final Gtk gtk;
        private final Pointer window;
        private Title title;

        public OnLoad(Pointer webView, Gtk gtk, Pointer window) {
            this.webView = webView;
            this.gtk = gtk;
            this.window = window;
        }

        public void loadStatus() {
            int status = webKit.webkit_web_view_get_load_status(webView);
            if (status == 2) {
                final Pointer frame = webKit.webkit_web_view_get_main_frame(webView);
                title = new Title(frame);
                title.updateTitle();
                g.g_signal_connect_data(frame, "notify::title", title, null);
                presenter.onPageLoad();
            }
        }

        private class Title implements Callback {

            private final Pointer frame;

            public Title(Pointer frame) {
                this.frame = frame;
            }

            public void updateTitle() {
                String title = webKit.webkit_web_frame_get_title(frame);
                if (title == null) {
                    title = "DukeScript Application";
                }
                gtk.gtk_window_set_title(window, title);
            }
        }
    }

    private static class OnDestroy implements Callback {

        public void signal() {
            System.exit(0);
        }
    }

    @Override
    public void execute(Runnable command) {
        pending.queue.add(command);
        if (Fn.activePresenter() == presenter) {
            try {
                pending.process();
            } catch (Exception ex) {
                Logger.getLogger(WebKitPresenter.class.getName()).log(Level.SEVERE, "Cannot process " + command, ex);
            }
        } else {
            glib.g_idle_add(pending, null);
        }
    }
    
    
    
    private class Pending implements Callback {
        final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();
        
        public void process() throws Exception {
            try (Closeable c = Fn.activate(presenter)) {
                for (;;) {
                    Runnable r = queue.poll();
                    if (r == null) {
                        break;
                    }
                    r.run();
                }
            }
        }
    }
    
    
}
