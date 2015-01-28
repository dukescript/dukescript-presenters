
package com.dukescript.presenters.strings;

/*
 * #%L
 * Strings Helpers - a library from the "DukeScript Presenters" project.
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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class BufferTest {
    static String lng = "Really long string that is easy to split into parts!";

    @Test public void splitAndRandomize() {
        long seed = System.currentTimeMillis();
        
        Buffer b = new Buffer(lng, 3, seed);
        
        for (int i = 0; i < lng.length(); i++) {
            for (int j = i; j < lng.length(); j++) {
                CharSequence mangled = b.subSequence(i, j);
                CharSequence real = lng.subSequence(i, j);
                
                assertEquals(mangled.toString(), real.toString(), "At seed " + seed + " subSequence(" + i + ", " + j + ") are the same");
            }
        }
    }
    
    @Test public void firstFailure() {
        long seed = 1394639167285L;
        
        Buffer b = new Buffer(lng, 3, seed);

        StringBuilder recover = new StringBuilder();
        for (int i = 0; i < lng.length(); i++) {
            recover.append(b.charAt(i));
        }
        
        assertEquals(recover.toString(), lng);
    }
    
}
