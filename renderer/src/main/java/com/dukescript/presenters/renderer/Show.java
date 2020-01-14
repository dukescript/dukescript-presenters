package com.dukescript.presenters.renderer;

/*
 * #%L
 * Desktop Browser Renderer - a library from the "DukeScript Presenters" project.
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


import com.sun.jna.Pointer;
import java.io.IOException;
import java.net.URI;
import org.netbeans.html.boot.spi.Fn;

/** @deprecated use {@link org.netbeans.html.presenters.render.Show}.
 */
@Deprecated
public final class Show {
    private final org.netbeans.html.presenters.render.Show delegate;
    
    Show(org.netbeans.html.presenters.render.Show delegate) {
        this.delegate = delegate;
    }
    
    /** @deprecated use {@link org.netbeans.html.presenters.render.Show#show(java.lang.String, java.net.URI)}
     * 
     * @param impl the name of implementation to use, can be <code>null</code>
     * @param page the page URL
     * @throws IOException if something goes wrong
     */
    @Deprecated
    public static void show(String impl, URI page) throws IOException {
        org.netbeans.html.presenters.render.Show.show(impl, page);
    }
    
    /** Initializes native browser window.
     * 
     * @param presenter the presenter that will be using the returned value
     * @param onPageLoad callback when page finishes loading
     * @param onContext callback when {@link #jsContext()} becomes available
     * @param headless should the window appear on the monitor or not?
     *   useful for testing
     * @return object to query and control the browser window
     */
    public static Show open(Fn.Presenter presenter, Runnable onPageLoad, Runnable onContext, boolean headless) {
        org.netbeans.html.presenters.render.Show delegate = org.netbeans.html.presenters.render.Show.open(presenter, onPageLoad, onContext, headless);
        return new Show(delegate);
    }

    /** Loads a page into the browser window.
     * 
     * @param page the URL to load
     * @throws IOException if something goes wrong
     */
    public void show(URI page) throws IOException {
        delegate.show(page);
    }
    
    /** Access to JavaScriptCore API of the browser window.
     * @return JavaScriptCore instance or <code>null</code> if not supported
     *   for this browser
     */
    public JSC jsc() {
        throw new UnsupportedOperationException();
    }

    /** Access to JavaScriptCore context.
     * @return the context or <code>null</code> if not supported or not 
     *   yet available
     */
    public Pointer jsContext() {
        return delegate.jsContext();
    }
    
    /** Executes a runnable on "UI thread".
     * @param command runnable to execute
     */
    public void execute(Runnable command) {
        delegate.execute(command);
    }
}
