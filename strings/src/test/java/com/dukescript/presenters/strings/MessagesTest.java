package com.dukescript.presenters.strings;

/*
 * #%L
 * Strings Helpers - a library from the "DukeScript Presenters" project.
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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class MessagesTest {
    
    public MessagesTest() {
    }

    @Messages({
        "helloWorld=Hello World!",
        "hello=Hello @1!"
    })
    @Test
    public void testSomeMethod() {
        final String hw = Strings.helloWorld();
        assertEquals(hw, "Hello World!");
        assertEquals(Strings.hello("Guys").toString(), "Hello Guys!");
    }

    @Messages({
        "helloTwice=Hello @1! Hello @1!"
    })
    @Test
    public void testRepeatThePattern() {
        final CharSequence hw = Strings.helloTwice("Guys");
        assertEquals(hw.toString(), "Hello Guys! Hello Guys!");
    }
    
    @Messages({
        "newLine=x\ny"
    })
    @Test public void newLines() {
        assertEquals(Strings.newLine(), "x\ny");
    }

    @Messages(
        "annotationProcessorOption=$value"
    )
    @Test
    public void valueFromAnnotationProcessor() {
        assertEquals(Strings.annotationProcessorOption(), "myValue");
    }
    
    @Messages({
        "order=2nd: @2 1st: @1"
    })
    @Test public void checkOrder() {
        assertEquals(Strings.order("1", "2").toString(), "2nd: 2 1st: 1");
    }
    
    @Messages({
"quoted=(function() {" +
"    var logo = document.createElement(\"div\");\n" +
"    logo.style.height = \"100vh\";\n" +
"    logo.style.width = \"100vw\";\n"
    })
    @Test public void reallyQuoted() {
        String exp = "(function() {" +
"    var logo = document.createElement(\"div\");\n" +
"    logo.style.height = \"100vh\";\n" +
"    logo.style.width = \"100vw\";\n";
        assertEquals(Strings.quoted(), exp, "Should be the same");
    }
    
    @Messages({
        "svg=    var svg = \"<svg version=\\\"1.1\\\" xmlns=\\\"http://www.w3.org/2000/svg\\\" xmlns:xlink=\\\"http://www.w3.org/1999/xlink\\\" x=\\\"0px\\\" y=\\\"0px\\\" width=\\\"100%\\\"\\n\" +\n"
    })
    @Test public void svgTest() {
        String exp = "    var svg = \"<svg version=\\\"1.1\\\" xmlns=\\\"http://www.w3.org/2000/svg\\\" xmlns:xlink=\\\"http://www.w3.org/1999/xlink\\\" x=\\\"0px\\\" y=\\\"0px\\\" width=\\\"100%\\\"\\n\" +\n";
        assertEquals(Strings.svg(), exp, "Quoted well");
    }
}
