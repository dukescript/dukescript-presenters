package com.dukescript.presenters.androidapp.test;

/*
 * #%L
 * Android Integration Tests - a library from the "DukeScript Presenters" project.
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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.java.html.json.Models;

final class ContentURLHandler extends URLStreamHandler {
    static {
        URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
            public URLStreamHandler createURLStreamHandler(String protocol) {
                if ("dynamic".equals(protocol)) {
                    return new ContentURLHandler();
                }
                return null;
            }
        });
    }
    private static Map<Integer,ContentConn> MAP = new ConcurrentHashMap<Integer, ContentConn>();
    
    public static synchronized URI register(
        String content, String mimeType, String... parameters
    ) {
        ContentConn c = new ContentConn(null, content, mimeType, parameters, new HashMap<String, List<String>>());
        final int s = MAP.size();
        MAP.put(s, c);
        try {
            return new URI("dynamic://" + s);
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(ex);
        }
    }
    

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        try {
            return MAP.get(Integer.parseInt(u.getHost())).clone(u);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    private static final class ContentConn extends HttpURLConnection {
        private ByteArrayInputStream is;
        private ByteArrayOutputStream os;
        private String[] parameters;
        private String mimeType;
        private String content;
        private Map<String, List<String>> props;

        ContentConn(
                URL u, String content, String mimeType, String[] parameters,
                Map<String, List<String>> props
        ) {
            super(u);
            this.content = content;
            this.mimeType = mimeType;
            this.parameters = parameters;
            this.props = props;
        }

        URLConnection clone(URL u) {
            return new ContentConn(u, content, mimeType, parameters, props);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            connect();
            return is;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            if (os == null) {
                os = new ByteArrayOutputStream();
            }
            return os;
        }

        @Override
        public String getHeaderField(String name) {
            if ("content-type".equals(name)) {
                return mimeType;
            }
            return null;
        }

        @Override
        public void setRequestProperty(String key, String value) {
            List<String> list = props.get(key);
            if (list == null) {
                list = Models.asList(value);
                props.put(key, list);
            } else {
                list.add(value);
            }
        }

        @Override
        public Map<String, List<String>> getRequestProperties() {
            return this.props;
        }

        @Override
        public String getRequestProperty(String key) {
            List<String> list = props.get(key);
            if (list == null || list.isEmpty()) {
                return null;
            } else {
                return list.get(0);
            }
        }


        @Override
        public void connect() throws IOException {
            if (is == null) {
                String s = content;
                String q = url.getQuery();
                for (int i = 0; i < parameters.length; i++) {
                    String subst;
                    if ("http.method".equals(parameters[i])) { // NOI18N
                        subst = this.method;
                    } else if ("http.requestBody".equals(parameters[i])) { // NOI18N
                        subst = new String(os.toByteArray(), "UTF-8"); // NOI18N
                    } else if (parameters[i].startsWith("http.header.")) {
                        String key = parameters[i].substring(12);
                        subst = getRequestProperty(key);
                    } else if (q != null) {
                        String p = parameters[i] + "=";
                        int b = q.indexOf(p);
                        if (b == -1) {
                            continue;
                        }
                        b += p.length();
                        int e = q.indexOf('&', b);
                        if (e == -1) {
                            e = q.length();
                        }
                        subst = q.substring(b, e);
                    } else {
                        continue;
                    }
                    if (subst != null) {
                        s = s.replace("$" + i, subst);
                    }
                }
                is = new ByteArrayInputStream(s.getBytes("UTF-8"));
            }
        }

        @Override
        public void disconnect() {
            is = null;
        }

        @Override
        public boolean usingProxy() {
            return false;
        }

    }
}
