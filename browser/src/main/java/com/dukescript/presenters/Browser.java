package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Presenter for any Browser - a library from the "DukeScript Presenters" project.
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


import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.concurrent.Executor;
import org.netbeans.html.boot.spi.Fn;

/** @deprecated Use {@link org.netbeans.html.presenters.browser.Browser}.
 */
@Deprecated
public final class Browser implements Fn.Presenter, Fn.KeepAlive, Flushable,
Executor, Closeable {
    private final org.netbeans.html.presenters.browser.Browser delegate;
    
    public Browser() throws Exception {
        this.delegate = new org.netbeans.html.presenters.browser.Browser();
    }

    public Browser(Config config) {
        org.netbeans.html.presenters.browser.Browser.Config c = new org.netbeans.html.presenters.browser.Browser.Config();
        c.command(config.getBrowser());
        c.port(config.getPort());
        this.delegate = new org.netbeans.html.presenters.browser.Browser(c);
    }
    

    @Override
    public final void execute(final Runnable r) {
        delegate.execute(r);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }


    @Override
    public Fn defineFn(String string, String... strings) {
        return delegate.defineFn(string, strings);
    }

    @Override
    public void loadScript(Reader reader) throws Exception {
        delegate.loadScript(reader);
    }

    @Override
    public Fn defineFn(String string, String[] strings, boolean[] blns) {
        return delegate.defineFn(string, strings, blns);
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }
    
    @Override
    public final void displayPage(URL page, Runnable onPageLoad) {
        delegate.displayPage(page, onPageLoad);
    }

    /** @deprecated Use {@link org.netbeans.html.presenters.browser.Browser.Config}.
     */
    @Deprecated
    public final static class Config {
        String browser;
        Integer port;

        public Config() {
        }

        private Config(Config copy) {
            this.browser = copy.browser;
            this.port = copy.port;
        }

        /** The command to use when invoking a browser. Possible values:
         * <ul>
         * <li>
         *   <b>GTK</b> - use Gtk WebKit implementation. Requires presence of appropriate native libraries
         * </li>
         * <li>
         *   <b>AWT</b> - use Desktop.browse(java.net.URI) to launch a browser
         * </li>
         * <li>
         *   <b>NONE</b> - just launches the server, useful together with {@link #port(int)} to specify a fixed port to open the server at
         * </li>
         * <li>
         * any other value is interpreted as a command which is then launched on a command line with one parameter - the URL to connect to
         * </li>
         * </ul>
         *
         * @param executable browser to execute
         * @return this instance
         */
        public Config command(String executable) {
            this.browser = executable;
            return this;
        }

        /** The port to start the server at.
         * By default a random port is selected.
         * @param port the port
         * @return this instance
         */
        public Config port(int port) {
            this.port = port;
            return this;
        }
        
        final String getBrowser() {
            if (browser != null) {
                return browser;
            }
            return System.getProperty("com.dukescript.presenters.browser"); // NOI18N
        }

        final int getPort() {
            if (port != null) {
                return port;
            }
            String port = System.getProperty("com.dukescript.presenters.browserPort"); // NOI18N
            try {
                return Integer.parseInt(port);
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
    }
}
