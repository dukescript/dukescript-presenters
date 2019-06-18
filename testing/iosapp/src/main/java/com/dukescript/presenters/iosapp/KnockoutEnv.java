package com.dukescript.presenters.iosapp;

/*
 * #%L
 * iOS Testing App - a library from the "DukeScript Presenters" project.
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


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import net.java.html.BrwsrCtx;
import net.java.html.js.JavaScriptBody;
import org.netbeans.html.boot.spi.Fn;
import org.netbeans.html.json.tck.KnockoutTCK;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = KnockoutTCK.class)
public class KnockoutEnv extends KnockoutTCK {
    static URI server;
    
    static Class[] tsts() throws Exception {
        server = DynamicHTTP.initServer();
        return testClasses();
    }

    @Override
    public BrwsrCtx createContext() {
        return Test.CTX;
    }

    @Override
    public Object createJSON(Map<String, Object> values) {
        Object json = putValue(null, null, null);
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            json = putValue(json, entry.getKey(), entry.getValue());
        }
        return json;
    }

    private Fn jsonFn;

    private Object putValue(Object json, String key, Object value) {
        if (jsonFn == null) {
            jsonFn = Fn.activePresenter().defineFn(
                    "if (json === null) json = new Object();"
                    + "if (key !== null) json[key] = value;"
                    + "return json;",
                    "json", "key", "value"
            );
        }
        try {
            return jsonFn.invoke(null, json, key, value);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private Fn executeScript;

    @Override
    public Object executeScript(String script, Object[] arguments) {
        if (executeScript == null) {
            executeScript = Fn.activePresenter().defineFn(
                    "var f = new Function(s); "
                    + "return f.apply(null, args);",
                    "s", "args"
            );
        }
        try {
            return executeScript.invoke(null, script, arguments);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @JavaScriptBody(args = { "msg" }, body = 
        "document.getElementById('result-text').value = msg;"
    )
    static native void textArea(String msg);
    
    @JavaScriptBody(args = { "r" }, javacall = true, body = 
        "var b = document.getElementById('result-button');\n" +
        "b.removeAttribute('hidden');\n" +
        "b.onclick = function() { r.@java.lang.Runnable::run()(); };\n"
    )
    private static native void button(Runnable r);
    
    static void exitButton(final int value) {
        button(new Runnable() {
            @Override
            public void run() {
                System.exit(value);
            }
        });
    }

    @Override
    public URI prepareURL(String content, String mimeType, String[] parameters) {
        try {
            final URL baseURL = server.toURL();
            StringBuilder sb = new StringBuilder();
            sb.append("/dynamic?mimeType=").append(mimeType);
            for (int i = 0; i < parameters.length; i++) {
                sb.append("&param").append(i).append("=").append(parameters[i]);
            }
            String mangle = content.replace("\n", "%0a")
                    .replace("\"", "\\\"").replace(" ", "%20");
            sb.append("&content=").append(mangle);

            URL query = new URL(baseURL, sb.toString());
            URLConnection c = query.openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
            URI connectTo = new URI(br.readLine());
            
            Test.LOG.log(Level.INFO, "Prepared URL: {0}", connectTo);
            
            return connectTo;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
