package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Generic Presenter - a library from the "DukeScript Presenters" project.
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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ValueOfTest {
    private Testing p;
    @BeforeMethod public void initInstance() {
        p = new Testing();
    }
    
    
    @Test public void parseSimpleArray() {
        Object res = p.valueOf("array:1:8:number:6");
        assertTrue(res instanceof Object[], "It is an array: " + res);
        Object[] arr = (Object[]) res;
        assertEquals(arr.length, 1, "One array item");
        assertEquals(arr[0], 6.0, "Value is six");
    }
}
