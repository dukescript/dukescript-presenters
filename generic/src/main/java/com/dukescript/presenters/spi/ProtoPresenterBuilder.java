package com.dukescript.presenters.spi;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
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

/** @deprecated Use {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
 */
@Deprecated
public final class ProtoPresenterBuilder {
    private final org.netbeans.html.presenters.spi.ProtoPresenterBuilder delegate;

    private ProtoPresenterBuilder() {
        delegate = org.netbeans.html.presenters.spi.ProtoPresenterBuilder.newBuilder();
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public static ProtoPresenterBuilder newBuilder() {
        return new ProtoPresenterBuilder();
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    @FunctionalInterface
    public interface Evaluator {
        /** Evaluates (potentially asynchronously) given code in the JavaScript
         * engine.
         * @param code the code to evaluate
         */
        public void eval(String code);
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder loadJavaScript(Evaluator loadScript, boolean synchronous) {
        delegate.loadJavaScript(loadScript::eval, synchronous);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder dispatcher(Executor executor, boolean implementExecutor) {
        delegate.dispatcher(executor, implementExecutor);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    @FunctionalInterface
    public interface Preparator {
        void prepare(OnPrepared onReady);
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public static abstract class OnPrepared {
        OnPrepared() {
        }

        /** Called when callback function prepared by {@link Preparator#prepare}
         * is ready.
         *
         * @param callbackFunctionName the name of created JavaScript function
         */
        public abstract void callbackIsPrepared(String callbackFunctionName);
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder preparator(Preparator onReady, boolean evalJavaScript) {
        delegate.preparator((when) -> {
            onReady.prepare(new OnPrepared() {
                @Override
                public void callbackIsPrepared(String callbackFunctionName) {
                    when.callbackIsPrepared(callbackFunctionName);
                }
            });
        }, evalJavaScript);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder type(String type) {
        delegate.type(type);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder app(String app) {
        delegate.app(app);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    @FunctionalInterface
    public interface Displayer {
        /** Display given URL and callback when the page is ready.
         *
         * @param url the page to display
         * @param onLoad callback to make when the page is ready
         */
        public void displayPage(URL url, Runnable onLoad);
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder displayer(Displayer displayer) {
        delegate.displayer(displayer::displayPage);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder register(Object data) {
        delegate.register(data);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    @FunctionalInterface
    public interface Logger {
        /** Log a message. Uses levels and message formats suitable for
         * java.util.logging package.
         *
         * @param level value from 500-1000
         * @param msg message with optional curly braces parameters
         * @param args the parameter values
         */
        public void log(int level, String msg, Object... args);
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenterBuilder logger(Logger logger) {
        delegate.logger(logger::log);
        return this;
    }

    /**
     * @deprecated Use
     * {@link org.netbeans.html.presenters.spi.ProtoPresenterBuilder}.
     */
    @Deprecated
    public ProtoPresenter build() {
        org.netbeans.html.presenters.spi.ProtoPresenter p = delegate.build();
        if (p instanceof Executor) {
            return new GenPresenterWithExecutor(p);
        }
        return new GenPresenter(p);
    }
    
    private class GenPresenter implements ProtoPresenter {
        final org.netbeans.html.presenters.spi.ProtoPresenter delegate;

        GenPresenter(org.netbeans.html.presenters.spi.ProtoPresenter p) {
            this.delegate = p;
        }

        @Override
        public String js2java(String method, String a1, String a2, String a3, String a4) throws Exception {
            return delegate.js2java(method, a1, a2, a3, a4);
        }

        @Override
        public <T> T lookup(Class<T> type) {
            return delegate.lookup(type);
        }

        @Override
        public Fn defineFn(String code, String... names) {
            return delegate.defineFn(code, names);
        }

        @Override
        public void displayPage(URL page, Runnable onPageLoad) {
            delegate.displayPage(page, onPageLoad);
        }

        @Override
        public void loadScript(Reader code) throws Exception {
            delegate.loadScript(code);
        }

        @Override
        public Fn defineFn(String code, String[] names, boolean[] keepAlive) {
            return delegate.defineFn(code, names, keepAlive);
        }

        @Override
        public void flush() throws IOException {
            delegate.flush();
        }
    }
    
    private final class GenPresenterWithExecutor extends GenPresenter implements Executor {
        public GenPresenterWithExecutor(org.netbeans.html.presenters.spi.ProtoPresenter p) {
            super(p);
        }

        @Override
        public void execute(Runnable command) {
            ((Executor)delegate).execute(command);
        }
    }
}
