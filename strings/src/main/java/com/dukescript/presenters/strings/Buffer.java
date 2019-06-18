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


import java.util.List;
import java.util.ArrayList;
import java.util.Random;

final class Buffer {
    private final int segmentLen;
    private final int[] begins;
    final StringBuilder buf;

    Buffer(CharSequence sb, int segmentLen, long seed) {
        this.segmentLen = segmentLen;
        int add = sb.length() % segmentLen == 0 ? 0 : 1;
        this.begins = new int[sb.length() / segmentLen + add];
        Random r = new Random(seed);
        for (int i = 0; i < begins.length; i++) {
            begins[i] = i;
        }
        for (int i = begins.length - 1; i > 0; i--) {
            int mt = r.nextInt(i);
            int t = begins[i];
            begins[i] = begins[mt];
            begins[mt] = t;
        }
        int[] rev = new int[begins.length];
        for (int i = 0; i < begins.length; i++) {
            rev[begins[i]] = i;
        }

        this.buf = new StringBuilder();
        for (int i = 0; i < begins.length; i++) {
            begins[i] *= segmentLen;
            final int at = rev[i] * segmentLen;
            int end = Math.min(at + segmentLen, sb.length());
            buf.append(sb.subSequence(at, end));
            while (end < at + segmentLen) {
                buf.append(' ');
                end++;
            }
        }
    }

    char charAt(int pos) {
        int seg = pos / segmentLen;
        int off = pos % segmentLen;

        int index = begins[seg] + off;
        return buf.charAt(index);
    }

    List<Integer> segments(int from, int end) {
        List<Integer> arr = new ArrayList<Integer>();
        BIG:
        while (from < end) {
            int i = from / segmentLen;
            int offset = from % segmentLen;
            int avail = segmentLen - offset;
            int needed = end - from;

            final int f = begins[i] + offset;
            assert f >= 0;
            arr.add(f);
            if (avail <= needed) {
                final int e = begins[i] + segmentLen;
                assert e >= f;
                arr.add(e);
                from += avail;
            } else {
                final int e = f + needed;
                assert e >= f;
                arr.add(e);
                from = end;
            }
        }
        return arr;
    }

    CharSequence subSequence(int from, int end) {
        StringBuilder sb = new StringBuilder();
        Integer frm = null;
        final List<Integer> sgmnts = segments(from, end);
        for (Integer i1 : sgmnts) {
            if (frm == null) {
                frm = i1;
                continue;
            }
            sb.append(buf.subSequence(frm, i1));
            frm = null;
        }
        return sb;
    }
}
