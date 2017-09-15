package com.dukescript.presenters.ios;

import java.util.Iterator;
import java.util.ServiceLoader;


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

public abstract class UI {
    private static final UI INSTANCE;
    static {
        final Iterator<UI> it = ServiceLoader.load(UI.class).iterator();
        if (!it.hasNext()) {
            throw new IllegalStateException("Cannot find implementation of UI class!");
        }
        INSTANCE = it.next();
    }

    public static UI getDefault() {
        return INSTANCE;
    }

    public abstract String identifier();
    public abstract String pathForResouce(String name, String ext, String subdir);
    public abstract String evaluateJavaScript(Object webView, String js);
    public abstract boolean openFileURL(String url);
    public abstract void runOnUiThread(Runnable r);
    public abstract void displayPage(String page, WebViewAdapter webViewDelegate);
    public abstract void setViewUp(Object view, String page, WebViewAdapter webViewDelegate);

    public interface WebViewAdapter {
        public boolean shouldStartLoad(Object webView, String url);
        public void didStartLoad(Object webView);
        public void didFailLoad(Object webView, String error);
        public void didFinishLoad(Object webView);
    }
}
