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

import java.net.URI;
import java.util.Map;
import java.util.concurrent.Executor;
import net.java.html.BrwsrCtx;
import org.netbeans.html.boot.spi.Fn;
import org.netbeans.html.context.spi.Contexts;
import org.netbeans.html.json.spi.Technology;
import org.netbeans.html.json.spi.Transfer;
import org.netbeans.html.json.tck.KnockoutTCK;
import org.json.JSONException;
import org.json.JSONObject;
import org.netbeans.html.ko4j.KO4J;

public final class Knockout extends KnockoutTCK {
    static Class[] allClasses() {
        return testClasses();
    }
    private Fn fn;

    @Override
    public BrwsrCtx createContext() {
        final Fn.Presenter ap = Fn.activePresenter();
        KO4J ko = new KO4J(ap);
        Object tyrus;
        try {
            tyrus = Class.forName("org.netbeans.html.wstyrus.TyrusContext").newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        Contexts.Builder cb = Contexts.newBuilder().
            register(Technology.class, ko.knockout(), 10).
            register(Transfer.class, (Transfer)tyrus, 10);
        if (ap instanceof Executor) {
            cb.register(Executor.class, (Executor)ap, 10);
        }
        cb.register(Fn.Presenter.class, ap, 10);
        return cb.build();
    }

    @Override
    public boolean canFailWebSocketTest() {
        return true;
    }

    @Override
    public Object createJSON(Map<String, Object> values) {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            try {
                json.put(entry.getKey(), entry.getValue());
            } catch (JSONException ex) {
                throw new IllegalStateException(ex);
            }
        }
        return json;
    }

    @Override
    public Object executeScript(String script, Object... arguments) {
        Fn.Presenter p = Fn.activePresenter();
        if (fn == null) {
            fn = p.defineFn(
                "var f = new Function(s);\n" +
                "return f.apply(null, args);\n",
                "s", "args"
            );
        }
        try {
            return fn.invoke(null, script, arguments);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public URI prepareURL(String content, String mimeType, String[] parameters) {
        return ContentURLHandler.register(content, mimeType, parameters);
    }
}
