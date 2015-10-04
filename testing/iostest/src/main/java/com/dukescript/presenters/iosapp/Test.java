package com.dukescript.presenters.iosapp;

/*
 * #%L
 * iostest - a library from the "DukeScript Presenters" project.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.html.BrwsrCtx;
import net.java.html.boot.BrowserBuilder;
import org.netbeans.html.json.tck.JavaScriptTCK;
import org.netbeans.html.json.tck.KOTest;

public final class Test extends JavaScriptTCK {
    static final Logger LOG = Logger.getLogger(Test.class.getName());
    private static final CountDownLatch CDL = new CountDownLatch(1);
    static BrwsrCtx CTX;
    
    public static void main(final String... args) throws Exception {
        Logger l = Logger.getLogger("com.dukescript");
        l.setLevel(Level.FINE);
        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.FINE);
        l.addHandler(ch);
        l.setUseParentHandlers(false);
        
        Thread t = new Thread("Testing harness") {
            @Override
            public void run() {
                try {
                    processTests();
                } catch (Exception ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            }
        };
        t.start();
        
        BrowserBuilder.newBrowser().loadPage("pages/test.html").
                loadClass(Test.class).invoke("onLoad", args).
                showAndWait();
    }
    
    private static void processTests() throws Exception {
        CDL.await();
        
        int[] cnt = { 0 };
        List<String> failed = new ArrayList<String>();
        for (Class<?> c : Test.testClasses()) {
            LOG.log(Level.INFO, "processing JS tests from {0}", c.getName());
            runTestsIn(c, cnt, failed);
        }
        for (Class<?> c : KnockoutEnv.tsts()) {
            LOG.log(Level.INFO, "processing KO tests from {0}", c.getName());
            runTestsIn(c, cnt, failed);
        }
        if (failed.size() > 0) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Failed tests: ").append(failed.size()).append(" from ").append(cnt[0]).append("\n");
            for (String n : failed) {
                sb.append("  ").append(n).append("\n");
            }
            LOG.log(Level.SEVERE, sb.toString());
            CTX.execute(new Runnable() {
                @Override
                public void run() {
                    KnockoutEnv.textArea(sb.toString());
                    KnockoutEnv.exitButton(1);
                }
            });
            return;
        }
        LOG.log(Level.INFO, "All {0} tests are OK!", cnt[0]);
        System.exit(0);
    }
    
    public static void onLoad(String... args) {
        CTX = BrwsrCtx.findDefault(Test.class);
        CDL.countDown();
    }

    private static void runTestsIn(final Class<?> c, int[] cnt, List<String> failed) {
        for (final Method method : c.getMethods()) {
            if (method.getAnnotation(KOTest.class) != null) {
                cnt[0]++;
                final Method m = method;
                class R implements Runnable {
                    Object obj;
                    Exception ex;
                    int cnt;
                    CountDownLatch cdl;
                    
                    @Override
                    public void run() {
                        try {
                            if (obj == null) {
                                obj = c.newInstance();
                            }
                            m.invoke(obj);
                        } catch (Exception ex) {
                            this.ex = ex;
                        } finally {
                            cdl.countDown();
                        }
                    }
                }
                try {
                    LOG.log(Level.INFO, "Running {0}", method);
                    CTX.execute(new Runnable() {
                        @Override
                        public void run() {
                            KnockoutEnv.textArea(method.toString());
                        }
                    });
                    R run = new R();
                    for (;;) {
                        CountDownLatch cdl = new CountDownLatch(1);
                        run.cdl = cdl;
                        run.ex = null;
                        CTX.execute(run);
                        cdl.await();
                        if (run.ex instanceof InvocationTargetException) {
                            Throwable in = ((InvocationTargetException)run.ex).getTargetException();
                            if (run.cnt++ < 10 && in instanceof InterruptedException) {
                                Thread.sleep(100);
                                continue;
                            }
                            throw in;
                        }
                        if (run.ex != null) {
                            throw run.ex;
                        }
                        break;
                    }
                    LOG.log(Level.INFO, "TEST {0} OK", method);
                } catch (Throwable t) {
                    failed.add(method.getName());
                    LOG.log(Level.SEVERE, "TEST " + method + "FAILED", t);
                }
            }
        }
    }
}
