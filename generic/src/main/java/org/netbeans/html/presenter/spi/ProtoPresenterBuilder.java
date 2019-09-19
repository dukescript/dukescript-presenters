package org.netbeans.html.presenter.spi;

import java.net.URL;
import java.util.concurrent.Executor;
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

/** The <em>prototypical</em> presenter builder. Builds a {@link Fn.Presenter} based on
 * top of textual protocol transferred between JVM and JavaScript engines.
 */
public final class ProtoPresenterBuilder {
    private Evaluator loadScript;
    private Executor executor;
    private Preparator onReady;
    private boolean sync;
    private boolean eval;
    private String type;
    private String app;
    private Displayer displayer;
    private boolean implementExecutor;

    private ProtoPresenterBuilder() {
    }

    public static ProtoPresenterBuilder newBuilder() {
        return new ProtoPresenterBuilder();
    }

    @FunctionalInterface
    public interface Evaluator {
        public void eval(String code);
    }

    public ProtoPresenterBuilder loadJavaScript(Evaluator loadScript) {
        this.loadScript = loadScript;
        return this;
    }

    public ProtoPresenterBuilder dispatcher(Executor executor, boolean implementExecutor) {
        this.executor = executor;
        this.implementExecutor = implementExecutor;
        return this;
    }

    /** Prepares the JavaScript environment. Defines a globally visible
     * JavaScript function which takes five string arguments and
     * then calls {@link ProtoPresenter#js2java(java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String) }
     * method of the {@link #build() created} {@link ProtoPresenter}.
     * Once the function is ready, it should notify the system
     * of the name of the function by calling {@link OnPrepare#callbackIsPrepared(java.lang.String)}.
     */
    @FunctionalInterface
    public interface Preparator {
        void prepare(OnPrepare onReady);
    }

    @FunctionalInterface
    public interface OnPrepare {
        /** Called when callback function prepared by {@link Preparator#prepare}
         * is ready.
         *
         * @param callbackFunctionName the name of created JavaScript function
         */
        void callbackIsPrepared(String callbackFunctionName);
    }

    public ProtoPresenterBuilder preparator(Preparator onReady) {
        this.onReady = onReady;
        return this;
    }

    public ProtoPresenterBuilder synchronous(boolean sync) {
        this.sync = sync;
        return this;
    }

    public ProtoPresenterBuilder evalJavaScript(boolean eval) {
        this.eval = eval;
        return this;
    }

    public ProtoPresenterBuilder type(String type) {
        this.type = type;
        return this;
    }

    public ProtoPresenterBuilder app(String app) {
        this.app = app;
        return this;
    }

    @FunctionalInterface
    public interface Displayer {
        public void displayPage(URL url, Runnable onLoad);
    }

    public ProtoPresenterBuilder displayer(Displayer displayer) {
        this.displayer = displayer;
        return this;
    }

    public ProtoPresenter build() {
        if (implementExecutor) {
            return new GenPresenterWithExecutor(this);
        }
        return new GenPresenter(this);
    }

    private static final class GenPresenterWithExecutor extends GenPresenter implements Executor {
        GenPresenterWithExecutor(ProtoPresenterBuilder b) {
            super(b);
        }

        @Override
        public void execute(Runnable command) {
            dispatch(command);
        }
    }

    private static class GenPresenter extends Generic implements ProtoPresenter {
        private final Evaluator loadScript;
        private final Executor executor;
        private final Preparator onReady;
        private final Displayer displayer;

        GenPresenter(ProtoPresenterBuilder b) {
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
        void callbackFn(ProtoPresenterBuilder.OnPrepare onReady) {
            this.onReady.prepare(onReady);
        }

        @Override
        void loadJS(String js) {
            loadScript.eval(js);
        }

        @Override
        void dispatch(Runnable r) {
            executor.execute(r);
        }

        @Override
        public void displayPage(URL url, Runnable r) {
            displayer.displayPage(url, r);
        }

        @Override
        public void initialize() {
            init();
        }
    }
}
