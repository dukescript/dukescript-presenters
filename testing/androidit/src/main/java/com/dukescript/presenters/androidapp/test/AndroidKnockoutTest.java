
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
import android.util.Log;
import com.dukescript.presenters.androidapp.JUnitTestMethods;
import com.dukescript.presenters.androidapp.TestActivity;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import org.netbeans.html.json.tck.JavaScriptTCK;
import org.netbeans.html.json.tck.KOTest;

@JUnitTestMethods(annotatedBy = KOTest.class,
    tests = {
        net.java.html.js.tests.JavaScriptBodyTest.class,
        net.java.html.js.tests.GCBodyTest.class,
        net.java.html.json.tests.ConvertTypesTest.class,
        net.java.html.json.tests.JSONTest.class,
        net.java.html.json.tests.KnockoutTest.class,
        net.java.html.json.tests.MinesTest.class,
        net.java.html.json.tests.OperationsTest.class,
        net.java.html.json.tests.WebSocketTest.class,
        net.java.html.json.tests.GCKnockoutTest.class

    },
    name = "AndroidKnockoutBase",
    superclass = ActivityInstrumentationTestCase2.class,
    generics = "<com.dukescript.presenters.androidapp.TestActivity>"
)
public class AndroidKnockoutTest extends AndroidKnockoutBase {
    private static Map<String,Method> toRun;
    private static Knockout knockout;

    public AndroidKnockoutTest() {
        super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        if (toRun == null) {
            toRun = ScriptTest.assertMethods(AndroidKnockoutTest.class);
            knockout = new Knockout();
        }
        super.setUp();
    }

    @Override
    protected void runMethod(String name) throws Throwable {
        final Method m = toRun.get(name);
        if (m != null) {
            runMethod(this, m);
        } else {
            throw new IllegalStateException("Cannot find method " + name);
        }
    }

    static void runMethod(
        final ActivityInstrumentationTestCase2<com.dukescript.presenters.androidapp.TestActivity> activity,
        final Method m
    ) throws InstantiationException, Throwable, IllegalAccessException {
        final String logName = activity.getClass().getSimpleName();
        final Object inst = m.getDeclaringClass().newInstance();
        Executor e = obtainExecutor(activity);
        final boolean[] calledRepeatedly = { false };
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
                        if (!calledRepeatedly[0]) {
                            Log.v(logName, "Running " + m.getDeclaringClass().getName() + "::" + m.getName());
                            activity.getActivity().changeName(m.getName());
                            calledRepeatedly[0] = true;
                        } else {
                            Log.v(logName, "Re-run: " + m.getDeclaringClass().getName() + "::" + m.getName());
                        }
                        m.invoke(inst);
                    } catch (Throwable ex) {
                        res[0] = ex;
                    } finally {
                        cdl.countDown();
                    }
                }
            });
            cdl.await();
            if (res[0] != null) {
                if (res[0] instanceof InvocationTargetException) {
                    InvocationTargetException te = (InvocationTargetException) res[0];
                    if (te.getTargetException() instanceof InterruptedException) {
                        Thread.sleep(100);
                        continue;
                    }
                    Log.e(logName, "Error", te.getTargetException());
                    activity.getActivity().error(m.getName() + te.getTargetException());
                    throw te.getTargetException();
                }
            }
            break;
        }
        Log.v(logName, "Success " + m.getDeclaringClass().getName() + "::" + m.getName());
    }

    private static Executor obtainExecutor(final ActivityInstrumentationTestCase2<com.dukescript.presenters.androidapp.TestActivity> test) throws InterruptedException {
        final Executor[] e = { null };
        final CountDownLatch assigned = new CountDownLatch(1);
        test.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                e[0] = (Executor) test.getActivity().getPresenter();
                assigned.countDown();
            }
        });
        assigned.await();
        return e[0];
    }

    static final class ScriptTest extends JavaScriptTCK {
        static Map<String,Method> assertMethods(Class<?> test) throws Exception {
            Map<String,Method> all = new HashMap<String, Method>();
            StringBuilder errors = new StringBuilder();
            int cnt = 0;
            StringBuilder classes = new StringBuilder();
            for (Class<?> c : testClasses()) {
                cnt = registerMethods(classes, c, cnt, test, all, errors);
            }
            for (Class<?> c : Knockout.allClasses()) {
                cnt = registerMethods(classes, c, cnt, test, all, errors);
            }
            if (errors.length() > 0) {
                errors.append("\nTesting classes: ").append(classes.toString());
                fail("Missing method: " + errors + "\nAdd them as method with empty bodies");
            }
            assert cnt > 0 : "Some methods found";
            return all;
        }

        private static int registerMethods(StringBuilder classes, Class<?> c, int cnt, Class<?> test, Map<String, Method> all, StringBuilder errors) throws SecurityException {
            if (classes.length() > 0) {
                classes.append(", ");
            }
            classes.append(c.getName());
            for (Method method : c.getMethods()) {
                if (method.getAnnotation(KOTest.class) != null) {
                    cnt++;
                    String name = method.getName();
                    if (!name.startsWith("test")) {
                        name = "test" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                    }
                    try {
                        Method m = test.getMethod("run" + name);
                        all.put(name, method);
                    } catch (NoSuchMethodException ex) {
                        errors.append("\n").append(name);
                    }
                }
            }
            return cnt;
        }
    }
}

