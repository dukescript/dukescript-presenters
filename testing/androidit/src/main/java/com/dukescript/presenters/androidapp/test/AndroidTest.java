
package com.dukescript.presenters.androidapp.test;

/*
 * #%L
 * Android Integration Tests - a library from the "DukeScript Presenters" project.
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

import android.test.ActivityInstrumentationTestCase2;
import com.dukescript.presenters.androidapp.JUnitTestMethods;
import com.dukescript.presenters.androidapp.TestActivity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import net.java.html.js.tests.GCBodyTest;
import net.java.html.js.tests.JavaScriptBodyTest;
import org.netbeans.html.json.tck.JavaScriptTCK;
import org.netbeans.html.json.tck.KOTest;

@JUnitTestMethods(annotatedBy = KOTest.class,
    tests = { JavaScriptBodyTest.class, GCBodyTest.class },
    name = "AndroidBase",
    superclass = ActivityInstrumentationTestCase2.class,
    generics = "<com.dukescript.presenters.androidapp.TestActivity>"
)
public class AndroidTest extends AndroidBase {
    private static Map<String,Method> toRun;

    public AndroidTest() {
        super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        if (toRun == null) {
            toRun = ScriptTest.assertMethods(AndroidTest.class);
        }
        super.setUp();
    }
    
    @Override
    protected void runTest() throws Throwable {
        final Method m = toRun.get(getName());
        if (m != null) {
            runMethod(this, m);
        } else {
            super.runTest();
        }
    }

    static void runMethod(
        ActivityInstrumentationTestCase2<com.dukescript.presenters.androidapp.TestActivity> activity,
        final Method m
    ) throws InstantiationException, Throwable, IllegalAccessException {
        final Object inst = m.getDeclaringClass().newInstance();
        Executor e = obtainExecutor(activity);
        for (int cnt = 0;; cnt++) {
            if (cnt == 100) {
                throw new InterruptedException("Too many repetitions");
            }
            final CountDownLatch cdl = new CountDownLatch(1);
            final Throwable[] res = { null };
            e.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        m.invoke(inst);
                    } catch (Throwable ex) {
                        res[0] = ex;
                    } finally {
                        cdl.countDown();
                    }
                }
            });
            cdl.countDown();
            if (res[0] != null) {
                if (res[0] instanceof InvocationTargetException) {
                    InvocationTargetException te = (InvocationTargetException) res[0];
                    if (te.getTargetException() instanceof InterruptedException) {
                        Thread.sleep(100);
                        continue;
                    }
                    throw te.getTargetException();
                }
            }
            break;
        }
    }

    private static Executor obtainExecutor(final ActivityInstrumentationTestCase2<com.dukescript.presenters.androidapp.TestActivity> test) {
        final Executor[] e = { null };
        test.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                e[0] = (Executor) test.getActivity().getPresenter();
            }
        });
        test.getInstrumentation().waitForIdleSync();
        return e[0];
    }

    static final class ScriptTest extends JavaScriptTCK {
        static Map<String,Method> assertMethods(Class<?> test) throws Exception {
            Map<String,Method> all = new HashMap<String, Method>();
            StringBuilder errors = new StringBuilder();
            int cnt = 0;
            final Class<?>[] classes = testClasses();
            for (Class<?> c : classes) {
                for (Method method : c.getMethods()) {
                    if (method.getAnnotation(KOTest.class) != null) {
                        cnt++;
                        String name = method.getName();
                        if (!name.startsWith("test")) {
                            name = "test" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                        }
                        try {
                            Method m = test.getMethod(name);
                            all.put(name, method);
                        } catch (NoSuchMethodException ex) {
                            errors.append("\n").append(name);
                        }
                    }
                }
            }
            if (errors.length() > 0) {
                errors.append("\nTesting classes: ").append(Arrays.toString(classes));
                fail("Missing method: " + errors + "\nAdd them as method with empty bodies");
            }
            assert cnt > 0 : "Some methods found";
            return all;
        }
    }
}

