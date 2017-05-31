package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Generic Presenter - a library from the "DukeScript Presenters" project.
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

import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

class Testing extends Generic {
    static final Logger LOG = Logger.getLogger(Testing.class.getName());
    final Executor QUEUE = Executors.newSingleThreadExecutor();
    final ScriptEngine eng;

    public Testing() {
        this(false);
    }
    
    protected Testing(boolean sync) {
        super(sync, true, "Testing", "test", "GPLv3");
        ScriptEngineManager sem = new ScriptEngineManager();
        eng = sem.getEngineByMimeType("text/javascript");
        try {
            eng.eval("function alert(m) { Packages.java.lang.System.out.println(m); };");
        } catch (ScriptException ex) {
            throw new IllegalStateException(ex);
        }        
    }

    @Override
    protected void log(Level level, String msg, Object... args) {
        if (args.length == 1 && args[0] instanceof Throwable) {
            LOG.log(level, msg, (Throwable)args[0]);
        } else {
            LOG.log(level, msg, args);
        }
    }

    public final class Clbk {
        private Clbk() {
        }
        
        public String pass(String method, String a1, String a2, String a3, String a4) throws Exception {
            return callback(method, a1, a2, a3, a4);
        }
    }
    private final Clbk clbk = new Clbk();
    
    @Override
    protected void callbackFn(String welcome, OnReady ready) {
        eng.getBindings(ScriptContext.ENGINE_SCOPE).put("jvm", clbk);
        try {
            eng.eval("(function(global) {\n"
                + "  var jvm = global.jvm;\n"
                + "  global.testingCB = function(m,a1,a2,a3,a4) {\n"
                + "    return jvm.pass(m,a1,a2,a3,a4);\n"
                + "  }\n"
                + "})(this);\n"
            );
        } catch (ScriptException ex) {
            throw new IllegalStateException(ex);
        }
        eng.getBindings(ScriptContext.ENGINE_SCOPE).put("jvm", "");
        ready.callbackReady("testingCB");
    }

    @Override
    protected void loadJS(final String js) {
        QUEUE.execute(new Runnable() {
            public void run() {
                try {
                    Object res = eng.eval(js);
                    LOG.log(Level.FINE, "Result: {0}", res);
                } catch (Throwable ex) {
                    LOG.log(Level.SEVERE, "Can't process " + js, ex);
                }
            }
        });
    }

    public void displayPage(URL url, Runnable r) {
        r.run();
    }

    @Override
    public void dispatch(Runnable r) {
        QUEUE.execute(r);
    }

    void beforeTest(Class<?> declaringClass) throws Exception {
    }
    
    static class Synchronized extends Testing {
        public Synchronized() {
            super(true);
        }

        @Override
        protected void loadJS(final String js) {
            try {
                Object res = eng.eval(js);
                LOG.log(Level.FINE, "Result: {0}", res);
            } catch (Throwable ex) {
                LOG.log(Level.SEVERE, "Can't process " + js, ex);
            }
        }

        @Override
        public void displayPage(URL url, Runnable r) {
            r.run();
        }

        @Override
        public void dispatch(Runnable r) {
            r.run();
        }

        @Override
        void beforeTest(Class<?> declaringClass) throws Exception {
        }
    } // end of Synchronized
}
