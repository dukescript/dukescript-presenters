package com.dukescript.presenters.androidapp;

/*
 * #%L
 * Android Test Application - a library from the "DukeScript Presenters" project.
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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Processor.class)
@SupportedAnnotationTypes("com.dukescript.presenters.androidapp.JUnitTestMethods")
public class JUnitTestMethodsProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(JUnitTestMethods.class)) {
            JUnitTestMethods ju = e.getAnnotation(JUnitTestMethods.class);
            if (ju == null) {
                continue;
            }
            PackageElement pkg = processingEnv.getElementUtils().getPackageOf(e);
            try {
                TypeMirror sn;
                try {
                    ju.superclass().getName();
                    throw new IllegalStateException();
                } catch (MirroredTypeException ex) {
                    sn = ex.getTypeMirror();
                }

                JavaFileObject sf = processingEnv.getFiler().createSourceFile(pkg.toString() + "." + ju.name(), e);
                Writer w = sf.openWriter();
                w.append("package ").append(pkg.toString()).append(";\n");
                w.append("abstract class ").append(ju.name()).append(" extends ").append(sn.toString()).append(ju.generics()).append(" {\n");
                w.append("  private final java.io.StringWriter output = new java.io.StringWriter();\n");
                w.append("  private final java.io.PrintWriter printer= new java.io.PrintWriter(output);\n");
                w.append("\n");

                Element esn = processingEnv.getTypeUtils().asElement(sn);
                for (Element ee : esn.getEnclosedElements()) {
                    if (ee.getKind() != ElementKind.CONSTRUCTOR) {
                        continue;
                    }
                    ExecutableElement constructor = (ExecutableElement) ee;

                    w.append("  ").append(ju.name()).append("(");
                    String sep = "";
                    for (VariableElement ve : constructor.getParameters()) {
                        final TypeMirror vet = processingEnv.getTypeUtils().erasure(ve.asType());
                        w.append(sep).append(vet.toString()).append(" ").append(ve.getSimpleName());
                        sep = ", ";
                    }
                    w.append(") {\n");
                    w.append("    super(");
                    sep = "";
                    for (VariableElement ve : constructor.getParameters()) {
                        w.append(sep).append(ve.getSimpleName());
                        sep = ", ";
                    }
                    w.append(");\n");
                    w.append("  }\n");
                }

                final TypeMirror ko;
                try {
                    ju.annotatedBy().getName();
                    throw new IllegalStateException();
                } catch (MirroredTypeException ex) {
                    ko = ex.getTypeMirror();
                }

                final List<? extends TypeMirror> tests;
                try {
                    ju.tests();
                    throw new IllegalStateException();
                } catch (MirroredTypesException ex) {
                    tests = ex.getTypeMirrors();
                }

                StringBuilder testAll = new StringBuilder();
                testAll.append("  public void test").append(ju.name()).append("() {\n");

                for (TypeMirror test : tests) {
                    Element et = processingEnv.getTypeUtils().asElement(test);
                    for (Element ee : et.getEnclosedElements()) {
                        if (ee.getKind() != ElementKind.METHOD) {
                            continue;
                        }
                        boolean found = false;
                        for (AnnotationMirror am : ee.getAnnotationMirrors()) {
                            if (processingEnv.getTypeUtils().isSameType(am.getAnnotationType(), ko)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            continue;
                        }
                        String name = ee.getSimpleName().toString();;
                        if (!name.startsWith("test")) {
                            name = "test" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
                        }
                        w.append("  public void run" + name + "() {\n");
                        w.append("    setName(\"" + name + "\");\n");
                        w.append("    try {\n");
                        w.append("      runTest();\n");
                        w.append("    } catch (Throwable t) {\n");
                        w.append("      t.printStackTrace(printer);\n");
                        w.append("    } finally {\n");
                        w.append("      printer.flush();\n");
                        w.append("    }\n");
                        w.append("  }\n");

                        testAll.append("    run").append(name).append("();\n");
                    }
                }

                testAll.append("  }\n");

                w.append(testAll.toString());
                w.append("}\n");
                w.close();
            } catch (IOException ex) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, ex.getMessage(), e);
            }
        }
        return true;
    }

}
