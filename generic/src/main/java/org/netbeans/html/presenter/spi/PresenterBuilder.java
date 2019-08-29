package org.netbeans.html.presenter.spi;

import java.net.URL;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.netbeans.html.boot.spi.Fn;

/*
 * #%L
 * DukeScript Generic Presenter - a library from the "DukeScript Presenters" project.
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


public final class PresenterBuilder {

    private Consumer<String> loadScript;
    private Executor executor;
    private Consumer<Consumer<String>> onReady;
    private boolean sync;
    private boolean eval;
    private String type;
    private String app;
    private BiConsumer<URL, Runnable> displayer;

    private PresenterBuilder() {
    }

    public static PresenterBuilder newBuilder() {
        return new PresenterBuilder();
    }

    public PresenterBuilder loadJavaScript(Consumer<String> loadScript) {
        this.loadScript = loadScript;
        return this;
    }

    public PresenterBuilder dispatcher(Executor executor) {
        this.executor = executor;
        return this;
    }

    public PresenterBuilder registerCallback(Consumer<Consumer<String>> onReady) {
        this.onReady = onReady;
        return this;
    }

    public PresenterBuilder synchronous(boolean sync) {
        this.sync = sync;
        return this;
    }

    public PresenterBuilder evalJavaScript(boolean eval) {
        this.eval = eval;
        return this;
    }

    public PresenterBuilder type(String type) {
        this.type = type;
        return this;
    }

    public PresenterBuilder app(String app) {
        this.app = app;
        return this;
    }

    public PresenterBuilder displayer(BiConsumer<URL,Runnable> displayer) {
        this.displayer = displayer;
        return this;
    }

    public Fn.Presenter build() {
        return new GenPresenter(this);
    }

    public static interface Callback {
        String callback(String method, String a1, String a2, String a3, String a4) throws Exception;
    }

    private static final class GenPresenter extends Generic implements Callback {
        private final Consumer<String> loadScript;
        private final Executor executor;
        private final Consumer<Consumer<String>> onReady;
        private BiConsumer<URL, Runnable> displayer;

        GenPresenter(PresenterBuilder b) {
            super(b.sync, b.eval, b.type, b.app);
            this.loadScript = b.loadScript;
            this.executor = b.executor;
            this.onReady = b.onReady;
            this.displayer = b.displayer;
        }

        @Override
        void log(Level level, String msg, Object... args) {
        }

        @Override
        void callbackFn(String ignored, OnReady onReady) {
            this.onReady.accept(onReady::callbackReady);
        }

        @Override
        void loadJS(String js) {
            loadScript.accept(js);
        }

        @Override
        void dispatch(Runnable r) {
            executor.execute(r);
        }

        @Override
        public void displayPage(URL url, Runnable r) {
            displayer.accept(url, r);
        }
    }
}
