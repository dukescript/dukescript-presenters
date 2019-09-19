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
 * top of textual protocol transfered between JVM and JavaScript engines.
 */
public final class ProtoPresenter {
    private Evaluator loadScript;
    private Executor executor;
    private Preparator onReady;
    private boolean sync;
    private boolean eval;
    private String type;
    private String app;
    private Displayer displayer;
    private boolean implementExecutor;

    private ProtoPresenter() {
    }

    public static ProtoPresenter newBuilder() {
        return new ProtoPresenter();
    }

    @FunctionalInterface
    public interface Evaluator {
        public void eval(String code);
    }

    public ProtoPresenter loadJavaScript(Evaluator loadScript) {
        this.loadScript = loadScript;
        return this;
    }

    public ProtoPresenter dispatcher(Executor executor, boolean implementExecutor) {
        this.executor = executor;
        this.implementExecutor = implementExecutor;
        return this;
    }

    @FunctionalInterface
    public interface Preparator {
        void prepare(OnPrepare onReady);
    }

    @FunctionalInterface
    public interface OnPrepare {
        void callbackIsPrepared(String callbackFunctionName);
    }

    public ProtoPresenter registerCallback(Preparator onReady) {
        this.onReady = onReady;
        return this;
    }

    public ProtoPresenter synchronous(boolean sync) {
        this.sync = sync;
        return this;
    }

    public ProtoPresenter evalJavaScript(boolean eval) {
        this.eval = eval;
        return this;
    }

    public ProtoPresenter type(String type) {
        this.type = type;
        return this;
    }

    public ProtoPresenter app(String app) {
        this.app = app;
        return this;
    }

    @FunctionalInterface
    public interface Displayer {
        public void displayPage(URL url, Runnable onLoad);
    }

    public ProtoPresenter displayer(Displayer displayer) {
        this.displayer = displayer;
        return this;
    }

    public Fn.Presenter build() {
        if (implementExecutor) {
            return new GenPresenterWithExecutor(this);
        }
        return new GenPresenter(this);
    }

    public static interface Callback extends Fn.Presenter {
        String callback(String method, String a1, String a2, String a3, String a4) throws Exception;
    }

    public static interface Initialize extends Fn.Presenter {
        void initialize();
    }

    private static final class GenPresenterWithExecutor extends GenPresenter implements Executor {
        GenPresenterWithExecutor(ProtoPresenter b) {
            super(b);
        }

        @Override
        public void execute(Runnable command) {
            dispatch(command);
        }
    }

    private static class GenPresenter extends Generic implements Callback, Initialize {
        private final Evaluator loadScript;
        private final Executor executor;
        private final Preparator onReady;
        private final Displayer displayer;

        GenPresenter(ProtoPresenter b) {
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
            this.onReady.prepare(onReady::callbackReady);
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
