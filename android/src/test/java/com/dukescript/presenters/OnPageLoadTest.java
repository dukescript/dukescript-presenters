package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Presenter for Android - a library from the "DukeScript Presenters" project.
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


import android.content.Context;
import java.io.Reader;
import java.net.URL;
import org.netbeans.html.boot.spi.Fn;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OnPageLoadTest implements Fn.Presenter {
    private String name;

    public OnPageLoadTest() {
    }

    @BeforeMethod
    public void cleanName() {
        this.name = null;
    }

    public static final class NoArgs {
        public static void noArgs() {
            Fn.activePresenter().defineFn("Called");
        }
    }

    @Test
    public void invokesNoArgumentMethod() throws Exception {
        Android.invokeOnPageLoad(this, null, NoArgs.class, "noArgs");
        assertEquals(this.name, "Called");
    }

    public static final class StringsArgs {
        public static void strings(String[] args) {
            Fn.activePresenter().defineFn("Called" + args.length);
        }
    }

    @Test
    public void invokesStringArrayArgumentMethod() throws Exception {
        Android.invokeOnPageLoad(this, null, StringsArgs.class, "strings");
        assertEquals(this.name, "Called0");
    }

    public static final class ContextArgs {
        public static void context(Context c) {
            Fn.activePresenter().defineFn("Called:" + c);
        }
    }

    @Test
    public void invokesContextArgumentMethod() throws Exception {
        Android.invokeOnPageLoad(this, null, ContextArgs.class, "context");
        assertEquals(this.name, "Called:null");
    }

    @Override
    public Fn defineFn(String name, String... strings) {
        assertNull(this.name, "No defined fn yet");
        this.name = name;
        return null;
    }

    @Override
    public void displayPage(URL url, Runnable r) {
    }

    @Override
    public void loadScript(Reader reader) throws Exception {
    }
}
