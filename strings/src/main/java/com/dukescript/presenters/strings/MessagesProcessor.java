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


import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import org.openide.util.lookup.ServiceProvider;
import com.dukescript.api.strings.Texts;

@ServiceProvider(service = Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public final class MessagesProcessor extends AbstractProcessor {

    public @Override Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Texts.class.getName());
    }

    public @Override boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        Map</*package*/String,Set<Element>> annotatedElementsByPackage = new HashMap<String,Set<Element>>();
        for (Element e : roundEnv.getElementsAnnotatedWith(Texts.class)) {
            Texts messages = e.getAnnotation(Texts.class);
            if (messages == null) { // bug in java.source, apparently; similar to #195983
                continue;
            }
            String pkg = findPackage(e);
            Set<Element> annotatedElements = annotatedElementsByPackage.get(pkg);
            if (annotatedElements == null) {
                annotatedElements = new HashSet<Element>();
                annotatedElementsByPackage.put(pkg, annotatedElements);
            }
            annotatedElements.add(e);
        }
        PACKAGE: for (Map.Entry<String,Set<Element>> packageEntry : annotatedElementsByPackage.entrySet()) {
            String pkg = packageEntry.getKey();
            Set<Element> annotatedElements = packageEntry.getValue();
            PackageElement pkgE = processingEnv.getElementUtils().getPackageElement(pkg);
            if (pkgE != null) {
                Set<Element> unscannedTopElements = new HashSet<Element>();
                unscannedTopElements.add(pkgE);
                try {
                    unscannedTopElements.addAll(pkgE.getEnclosedElements());
                } catch (/*NullPointerException,BadClassFile*/RuntimeException x) { // #196556
                    processingEnv.getMessager().printMessage(Kind.WARNING, "#196556: reading " + pkg + " failed with " + x + " in " + x.getStackTrace()[0] + "; do a clean build!");
                }
                unscannedTopElements.removeAll(roundEnv.getRootElements());
                addToAnnotatedElements(unscannedTopElements, annotatedElements);
            } else {
                processingEnv.getMessager().printMessage(Kind.WARNING, "Could not check for other source files in " + pkg);
            }
            Map</*key*/String,/*value*/String> pairs = new HashMap<String,String>();
            Map</*identifier*/String,Element> identifiers = new HashMap<String,Element>();
            Map</*key*/String,/*simplename*/String> compilationUnits = new HashMap<String,String>();
            for (Element e : annotatedElements) {
                String simplename = findCompilationUnitName(e);
                for (String keyValue : e.getAnnotation(Texts.class).value()) {
                    int i = keyValue.indexOf('=');
                    if (i == -1) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Bad key=value: " + keyValue, e);
                        continue;
                    }
                    String key = keyValue.substring(0, i);
                    if (key.isEmpty() || !key.equals(key.trim())) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Whitespace not permitted in key: " + keyValue, e);
                        continue;
                    }
                    Element original = identifiers.put(key, e);
                    if (original != null) {
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Duplicate key: " + key, e);
                        processingEnv.getMessager().printMessage(Kind.ERROR, "Duplicate key: " + key, original);
                        continue PACKAGE; // do not generate anything
                    }
                    String value = keyValue.substring(i + 1);
                    if (value.startsWith("$")) {
                        value = processingEnv.getOptions().get(value.substring(1));
                    }
                    pairs.put(key, value);
                    compilationUnits.put(key, simplename);
                }
            }
            Element[] elements = new HashSet<Element>(identifiers.values()).toArray(new Element[0]);
            try {
                Map</*identifier*/String,Method> methods = new TreeMap<String,Method>();
                
                for (Map.Entry<String, String> entry2 : pairs.entrySet()) {
                    String key = entry2.getKey();
                    String value = entry2.getValue();
                    SortedSet<Subst> substitutions = new TreeSet<Subst>();
                    
                    int i;
                    for (i = 0; ; i++) {
                        final String subst = "@" + (i + 1);
                        int pos = -1;
                        for (;;) {
                            int where = value.indexOf(subst, pos + 1);
                            if (where == -1) {
                                break;
                            }
                            substitutions.add(new Subst(where, subst.length(), i));
                            pos = where;
                        }
                        if (pos == -1) {
                            break;
                        }
                    }

                    methods.put(key, new Method(key, substitutions, value));
                }
                String fqn = pkg + ".Strings";
                Writer w = processingEnv.getFiler().createSourceFile(fqn, elements).openWriter();

                StringBuilder buf = new StringBuilder();
                for (Method method : methods.values()) {
                    method.at = buf.length();
                    buf.append(method.text);
                }
                
                String chunk = processingEnv.getOptions().get("stringSize");
                String seedStr = processingEnv.getOptions().get("stringSeed");
                
                int length = buf.length();
                long seed = 0;
                if (chunk != null && seedStr != null) {
                    length = Integer.parseInt(chunk);
                    if ("now".equals(seedStr)) {
                        seed = System.currentTimeMillis();
                    } else {
                        seed = Long.parseLong(seedStr);
                    }
                    processingEnv.getMessager().printMessage(Kind.NOTE, "Using string size " + length + " and seed " + seed);
                }
                
                Buffer b = new Buffer(buf, length, seed);
                
                
                try {
                    PrintWriter pw = new PrintWriter(w);
                    pw.println("package " + pkg + ";");
                    pw.println("/** Localizable strings for {@link " + pkg + "}. */");
                    pw.println("class Strings {");
                    for (Method method : methods.values()) {
                        pw.print(method.toString(b));
                    }
                    pw.println("    private void Strings() {}");
                    pw.print("    private static final String text = \"");
                    pw.print(b.buf.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n\" + \n\""));
                    pw.println("\";\n");
                    pw.println("}");
                    pw.flush();
                    pw.close();
                } finally {
                    w.close();
                }
            } catch (IOException x) {
                processingEnv.getMessager().printMessage(Kind.ERROR, "Could not generate files: " + x, elements[0]);
            }
        }
        return true;
    }

    private String findPackage(Element e) {
        switch (e.getKind()) {
        case PACKAGE:
            return ((PackageElement) e).getQualifiedName().toString();
        default:
            return findPackage(e.getEnclosingElement());
        }
    }

    private String findCompilationUnitName(Element e) {
        switch (e.getKind()) {
        case PACKAGE:
            return "package-info";
        case CLASS:
        case INTERFACE:
        case ENUM:
        case ANNOTATION_TYPE:
            switch (e.getEnclosingElement().getKind()) {
            case PACKAGE:
                return e.getSimpleName().toString();
            }
        }
        return findCompilationUnitName(e.getEnclosingElement());
    }

    private void addToAnnotatedElements(Collection<? extends Element> unscannedElements, Set<Element> annotatedElements) {
        for (Element e : unscannedElements) {
            if (e.getAnnotation(Texts.class) != null) {
                annotatedElements.add(e);
            }
            if (e.getKind() != ElementKind.PACKAGE) {
                addToAnnotatedElements(e.getEnclosedElements(), annotatedElements);
            }
        }
    }

    private static class Subst implements Comparable<Subst> {

        final int begin;
        final int len;
        final int arg;

        public Subst(int begin, int len, int arg) {
            this.begin = begin;
            this.len = len;
            this.arg = arg;
        }

        @Override
        public int compareTo(Subst o) {
            return begin - o.begin;
        }
    }

    private static class Method {
        final String key;
        final SortedSet<Subst> substitutions;
        final String text;
        int at;

        public Method(String name, SortedSet<Subst> substitutions, String text) {
            this.key = name;
            this.substitutions = substitutions;
            this.text = text;
        }
        
        public String toString(Buffer buf) {
            StringBuilder method = new StringBuilder();
            if (substitutions.isEmpty()) {
                method.append("    static String ");
            } else {
                method.append("    static CharSequence ");
            }
            method.append(key).append("(");
            {
                int params = 0;
                for (Subst sbst : substitutions) {
                    if (sbst.arg >= params) {
                        params = sbst.arg + 1;
                    }
                }
                String sep = "";
                for (int i = 0; i < params; i++) {
                    method.append(sep).append("Object arg").append(i);
                    sep = ", ";
                }
            }
            method.append(") {\n");
            method.append("        StringBuilder sb = new StringBuilder(").append(
                text.length() + substitutions.size() * 30
            ).append(");\n");
            int pos = 0;
            for (Subst subst : substitutions) {
                if (pos < subst.begin) {
                    List<Integer> segments = buf.segments(at + pos, at + subst.begin);
                    for (int i = 0; i < segments.size(); i += 2) {
                        int f = segments.get(i);
                        int e = segments.get(i + 1);
                        assert f <= e;
                        method.append("        sb.append(text.subSequence(").
                            append(f).append(", ")
                            .append(e).append("));\n");

                    }
                }
                method.append("        sb.append(arg").append(subst.arg).append(");\n");
                pos = subst.begin + subst.len;
            }
            if (pos < text.length()) {
                List<Integer> segments = buf.segments(at + pos, at + text.length());
                for (int i = 0; i < segments.size(); i += 2) {
                    int f = segments.get(i);
                    int e = segments.get(i + 1);
                    assert f <= e;
                    method.append("        sb.append(text.subSequence(").
                        append(f).append(", ")
                        .append(e).append("));\n");

                }
            }
            method.append("        return sb");
            if (substitutions.isEmpty()) {
                method.append(".toString()");
            }
            method.append(";\n");
            method.append("    }\n");
            
            return method.toString();
        }
    }
}
