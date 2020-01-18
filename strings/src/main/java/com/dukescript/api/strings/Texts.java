package com.dukescript.api.strings;

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


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Define textual string data and use them from a single character buffer.
 * Various tools offer obfuscation of code, but not many obfuscate the strings.
 * This tool help you do that. Define your strings in {@linkplain RetentionPolicy#SOURCE
 * source} and let this tool mangle them into unreadable buffer of characters,
 * accessible by {@code Strings.xyz} methods.
 * <p>
 * This API can be used in compile-only mode (e.g. Maven scope provided) - it
 * uses an annotation processor that collects all {@link Texts @Texts} annotation
 * usages and generates completely independent {@code Strings} class to be
 * used in your application.
 * <p>
 * The processor can be configured via two parameters (passed to {@code javac}
 * with prefix {@code -A}):
 * <ul>
 *   <li><b>stringSize</b> - the size of string chunks to use - by default all characters</li>
 *   <li><b>stringSeed</b> - the seed to use to shuffle the texts - random if not specified</li>
 * </ul>
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Texts {
    /** Individual lines defining the texts. Use {@code key=message} format
     * for each element in the array. Use {@code {n}} starting with {@code n}
     * being zero to define substitutions into the text message.
     * <p>
     * Each key will result into {@code key()} method being generated into
     * the {@code Strings} class. If the message has parameters, they will
     * become substituable by the methods arguments.
     * @return array of {@code key=message} strings
     */
    String[] value();
}
