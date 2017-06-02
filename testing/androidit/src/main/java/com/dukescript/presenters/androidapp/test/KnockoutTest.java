
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
import static com.dukescript.presenters.androidapp.test.AndroidTest.runMethod;
import java.lang.reflect.Method;
import java.util.Map;
import org.netbeans.html.json.tck.KOTest;

@JUnitTestMethods(annotatedBy = KOTest.class,
    tests = {
        net.java.html.json.tests.ConvertTypesTest.class,
        net.java.html.json.tests.JSONTest.class,
        net.java.html.json.tests.KnockoutTest.class,
        net.java.html.json.tests.MinesTest.class,
        net.java.html.json.tests.OperationsTest.class,
        net.java.html.json.tests.WebSocketTest.class,
        net.java.html.json.tests.GCKnockoutTest.class
    },
    name = "KnockoutBase",
    superclass = ActivityInstrumentationTestCase2.class,
    generics = "<com.dukescript.presenters.androidapp.TestActivity>"
)
public class KnockoutTest extends KnockoutBase {
    private static Map<String,Method> toRun;

    public KnockoutTest() {
        super(TestActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        if (toRun == null) {
            toRun = Knockout.assertMethods(KnockoutTest.class);
        }
        super.setUp();
        // register the TCK
        Knockout ko = new Knockout();
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
}
