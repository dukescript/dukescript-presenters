package com.dukescript.presenters.webkit;

/*
 * #%L
 * WebKit Presenter - a library from the "DukeScript Presenters" project.
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


import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.Executor;
import org.netbeans.html.boot.spi.Fn;

/** @deprecated Use {@link org.netbeans.html.presenters.webkit.WebKitPresenter}.
 */
@Deprecated
public final class WebKitPresenter implements Fn.Presenter, Fn.KeepAlive, Executor {
    private final org.netbeans.html.presenters.webkit.WebKitPresenter delegate;
    
    public WebKitPresenter() {
        this(false);
    }
    
    WebKitPresenter(boolean headless) {
        this.delegate = new org.netbeans.html.presenters.webkit.WebKitPresenter(headless);
    }
    
    @Override
    public Fn defineFn(String code, String... names) {
        return delegate.defineFn(code, names);
    }
    
    @Override
    public Fn defineFn(String code, String[] names, boolean[] keepAlive) {
        return delegate.defineFn(code, names, keepAlive);
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
    public void execute(Runnable command) {
        delegate.execute(command);
    }

    @Deprecated
    public final class FnCallback implements Callback {
        private final Object vm;
        private final Method method;

        public FnCallback(Object vm, Method method) {
            this.vm = vm;
            this.method = method;
        }
        
        public Pointer call(
            Pointer jsContextRef, Pointer jsFunction, Pointer thisObject,
            int argumentCount, PointerByReference ref, Pointer exception
        ) throws Exception {
            throw new UnsupportedOperationException();
        }
    }
}
