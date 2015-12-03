package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Presenter for any Browser - a library from the "DukeScript Presenters" project.
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

import static com.dukescript.presenters.Browser.LOG;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.util.Arrays;
import java.util.logging.Level;

abstract class Show {
    static void show(String impl, URI page) throws IOException {
        try {
            Class<?> c = Class.forName("com.dukescript.presenters." + impl);
            Show show = (Show) c.newInstance();
            show.show(page);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            if (impl == null) {
                impl = "xdg-open";
            }
            LOG.log(Level.INFO, "Trying command line execution of {0}", impl);
            String[] cmdArr = {
                impl, page.toString()
            };
            LOG.log(Level.INFO, "Launching {0}", Arrays.toString(cmdArr));
            final Process process = Runtime.getRuntime().exec(cmdArr);
            try {
                process.waitFor();
            } catch (InterruptedException ex1) {
                throw (InterruptedIOException) new InterruptedIOException().initCause(ex1);
            }
        }
    }

    protected abstract void show(URI page) throws IOException;
}
