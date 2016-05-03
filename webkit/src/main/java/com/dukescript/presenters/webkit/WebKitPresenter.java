package com.dukescript.presenters.webkit;

/*
 * #%L
 * WebKit Presenter - a library from the "DukeScript Presenters" project.
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

import com.dukescript.presenters.renderer.JSC;
import com.dukescript.presenters.renderer.Show;
import com.dukescript.presenters.strings.Messages;
import com.sun.jna.Callback;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.PointerByReference;
import java.io.Reader;
import java.io.StringReader;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.html.boot.spi.Fn;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = Fn.Presenter.class)
public final class WebKitPresenter implements Fn.Presenter, Fn.KeepAlive, Executor {
    private static final Logger LOG = Logger.getLogger(WebKitPresenter.class.getName());
    private final Show shell;
    private Runnable onPageLoad;
    private OnFinalize onFinalize;
    private Pointer ctx;
    private Pointer javaClazz;
    private final Map<Object,Object> toJava = new HashMap<>();
    private Pointer arrayLength;
    private Pointer valueTrue;
    private Pointer valueFalse;
    private String onPageApp;

    public WebKitPresenter() {
        this(false);
    }
    
    WebKitPresenter(boolean headless) {
        String system = System.getProperty("os.name");
        shell = Show.open(this, new Runnable() {
            @Override
            public void run() {
                onPageLoad.run();
            }
        }, new Runnable() {
            @Override
            public void run() {
                jsContext(shell.jsContext());
            }
        }, headless);
    }
    
    @Override
    public Fn defineFn(String code, String... names) {
        return defineFn(code, names, null);
    }
    @Override
    public Fn defineFn(String code, String[] names, boolean[] keepAlive) {
        JSC jsc = shell.jsc();
        Pointer[] jsNames = new Pointer[names.length];
        for (int i = 0; i < jsNames.length; i++) {
            jsNames[i] = jsc.JSStringCreateWithUTF8CString(names[i]);
        }
        Pointer jsCode = jsc.JSStringCreateWithUTF8CString(code);
        PointerByReference exc = new PointerByReference();
        Pointer fn = jsc.JSObjectMakeFunction(ctx, null, names.length, jsNames, jsCode, null, 1, exc);
        if (fn == null) {
            throw new IllegalStateException("Cannot initialize function: " + exc.getValue());
        }
        
        jsc.JSStringRelease(jsCode);
        for (Pointer jsName : jsNames) {
            jsc.JSStringRelease(jsName);
        }
        return new JSCFn(fn, keepAlive);
    }

    @Override
    public void displayPage(URL page, Runnable onPageLoad) {
        this.onPageLoad = onPageLoad;
        this.onPageApp = findCalleeClassName();
        try {
            if ("jar".equals(page.getProtocol())) {
                page = UnJarResources.extract(page);
            }

            shell.show(page.toURI());
        } catch (Throwable t) {
            LOG.log(Level.SEVERE, onPageApp, t);
        }
    }

    @Override
    public void loadScript(Reader code) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (;;) {
            int ch = code.read();
            if (ch == -1) {
                break;
            }
            sb.append((char)ch);
        }
        Pointer script = shell.jsc().JSStringCreateWithUTF8CString(sb.toString());
        PointerByReference ex = new PointerByReference();
        shell.jsc().JSEvaluateScript(ctx, script, null, null, 1, ex);
        shell.jsc().JSStringRelease(script);
        if (ex.getValue() != null) {
            throw new Exception(convertToString(shell.jsc(), ex.getValue()));
        }
    }
    
    Pointer[] convertFromJava(Object... args) throws Exception {
        return convertFromJava(args, null);
    }
    Pointer[] convertFromJava(Object[] args, boolean[] keepAlive) throws Exception {
        JSC jsc = shell.jsc();
        Pointer[] arr = new Pointer[args.length];
        for (int i = 0; i < arr.length; i++) {
            Object v = args[i];
            if (v == null) {
                v = jsc.JSValueMakeNull(ctx);
            } else if (v instanceof Number) {
                v = jsc.JSValueMakeNumber(ctx, ((Number)v).doubleValue());
            } else if (v instanceof Boolean) {
                v = ((Boolean)v) ? valueTrue : valueFalse;
            } else if (v instanceof String) {
                Pointer str = jsc.JSStringCreateWithUTF8CString((String)v);
                v = jsc.JSValueMakeString(ctx, str);
            } else if (v instanceof Enum) {
                Pointer str = jsc.JSStringCreateWithUTF8CString(((Enum)v).name());
                v = jsc.JSValueMakeString(ctx, str);
            } else if (v instanceof Character) {
                v = jsc.JSValueMakeNumber(ctx, (Character)v);
            } else if (v instanceof JSObject) {
                v = ((JSObject)v).value;
            } else if (v instanceof int[]) {
                int[] numbers = (int[])v;
                Pointer[] content = new Pointer[numbers.length];
                for (int j = 0; j < content.length; j++) {
                    content[j] = jsc.JSValueMakeNumber(ctx, numbers[j]);
                }
                v = jsc.JSObjectMakeArray(ctx, content.length, content, null);
            } else if (v instanceof double[]) {
                double[] numbers = (double[])v;
                Pointer[] content = new Pointer[numbers.length];
                for (int j = 0; j < content.length; j++) {
                    content[j] = jsc.JSValueMakeNumber(ctx, numbers[j]);
                }
                v = jsc.JSObjectMakeArray(ctx, content.length, content, null);
            } else if (v instanceof Object[]) {
                Pointer[] content = convertFromJava((Object[])v);
                v = jsc.JSObjectMakeArray(ctx, content.length, content, null);
            } else if (v.getClass().isArray()) {
                int len = Array.getLength(v);
                Object[] boxed = new Object[len];
                for (int j = 0; j < len; j++) {
                    boxed[j] = Array.get(v, j);
                }
                Pointer[] content = convertFromJava(boxed);
                v = jsc.JSObjectMakeArray(ctx, content.length, content, null);
            } else if (v.getClass().getSimpleName().equals("$JsCallbacks$")) {
                Pointer vm = jsc.JSObjectMake(ctx, null, null);
                for (Method method : v.getClass().getMethods()) {
                    if (method.getDeclaringClass() != v.getClass()) {
                        continue;
                    }
                    Pointer name = jsc.JSStringCreateWithUTF8CString(method.getName());
                    FnCallback fnC = new FnCallback(v, method);
                    toJava.put(fnC, fnC);
                    Pointer fn = jsc.JSObjectMakeFunctionWithCallback(ctx, null, fnC);
                    jsc.JSObjectSetProperty(ctx, vm, name, fn, 0, null);
                    jsc.JSStringRelease(name);
                }
                v = vm;
            } else {
                Pointer p = jsc.JSObjectMake(ctx, javaClazz, v);
                if (keepAlive == null || keepAlive[i]) { 
                    toJava.put(p, v);
                } else {
                    toJava.put(p, new WeakVal(v));
                }
                v = p;
            }
            arr[i] = (Pointer) v;
        }
        return arr;
    }
    
    final String convertToString(JSC jsc, Pointer value) {
        int type = jsc.JSValueGetType(ctx, value);
        if (type == 5) {
            Pointer toStr = jsc.JSStringCreateWithUTF8CString("this.toString()");
            value = jsc.JSEvaluateScript(ctx, toStr, value, null, 0, null);
            jsc.JSStringRelease(toStr);
        }
        Object ret = convertToJava(jsc, String.class, value);
        return ret != null ? ret.toString() : "<null value>";
    }
    
    final Object convertToJava(JSC jsc, Class<?> expectedType, Pointer value) throws IllegalStateException {
        int type = jsc.JSValueGetType(ctx, value);
        /*
        typedef enum {
        kJSTypeUndefined,
        kJSTypeNull,
        kJSTypeBoolean,
        kJSTypeNumber,
        kJSTypeString,
        kJSTypeObject
        } JSType;
        */
        switch (type) {
            case 0: 
            case 1:
                return null;
            case 2: {
                double probability = jsc.JSValueToNumber(ctx, value, null);
                if (expectedType == boolean.class) {
                    expectedType = Boolean.class;
                }
                return expectedType.cast(probability >= 0.5);
            }
            case 3: {
                Double ret = jsc.JSValueToNumber(ctx, value, null);
                if (expectedType.isInstance(ret) || expectedType == double.class) {
                    return ret;
                }
                if (expectedType == Integer.class || expectedType == int.class) {
                    return ret.intValue();
                }
                if (expectedType == Float.class || expectedType == float.class) {
                    return ret.floatValue();
                }
                if (expectedType == Long.class || expectedType == long.class) {
                    return ret.longValue();
                }
                if (expectedType == Short.class || expectedType == short.class) {
                    return ret.shortValue();
                }
                if (expectedType == Byte.class || expectedType == byte.class) {
                    return ret.byteValue();
                }
                if (expectedType == Character.class || expectedType == char.class) {
                    return (char)ret.intValue();
                }
                throw new ClassCastException("Cannot convert double to " + expectedType);
            }
            case 4: {
                Pointer val = jsc.JSValueToStringCopy(ctx, value, null);
                int max = jsc.JSStringGetMaximumUTF8CStringSize(val);
                Memory mem = new Memory(max);
                jsc.JSStringGetUTF8CString(val, mem, max);
                return expectedType.cast(mem.getString(0));
            }
            case 5: {
                Object ret;
                if (jsc.JSValueIsObjectOfClass(ctx, value, javaClazz)) {
                    ret = toJava.get(value);
                    if (ret instanceof WeakVal) {
                        ret = ((WeakVal)ret).get();
                    } else if (ret == null) {
                        ret = jsc.JSObjectGetPrivate(value);
                    }
                } else {
                    PointerByReference ex = new PointerByReference();
                    Pointer checkArray = jsc.JSObjectCallAsFunction(ctx, arrayLength, null, 1, new Pointer[] { value }, ex);
                    if (checkArray == null) {
                        throw new RuntimeException(convertToString(jsc, ex.getValue()));
                    }
                    int len = (int)jsc.JSValueToNumber(ctx, checkArray, null);
                    if (len >= 0) {
                        Object[] arr = new Object[len];
                        for (int i = 0; i < len; i++) {
                            Pointer val = jsc.JSObjectGetPropertyAtIndex(ctx, value, i, null);
                            arr[i] = convertToJava(jsc, Object.class, val);
                        }
                        return arr;
                    }
                    ret = new JSObject(this, value);
                }
                return expectedType.cast(ret);
            }
            default:
                throw new IllegalStateException("Uknown type: " + type);
        }
    }

    @Override
    public void execute(Runnable command) {
        shell.execute(command);
    }

    final void jsContext(Pointer ctx) {
        this.ctx = ctx;

        onFinalize = new WebKitPresenter.OnFinalize();
        javaClazz = shell.jsc().JSClassCreate(new JSC.JSClassDefinition(onFinalize));
        {
            JSC j = shell.jsc();
            Pointer jsGlobal = ctx;
            Pointer arrArg = j.JSStringCreateWithUTF8CString("x");
            Pointer arrT = j.JSStringCreateWithUTF8CString("var res = x.constructor === Array ? x.length : -1; return res;");
            Pointer arrFn = j.JSObjectMakeFunction(jsGlobal, null, 1, new Pointer[]{arrArg}, arrT, null, 0, null);
            arrayLength = arrFn;
            j.JSValueProtect(ctx, arrFn);
        }
        {
            JSC j = shell.jsc();
            Pointer trueScr = j.JSStringCreateWithUTF8CString("true");
            valueTrue = j.JSEvaluateScript(ctx, trueScr, null, null, 1, null);
            j.JSStringRelease(trueScr);
            j.JSValueProtect(ctx, valueTrue);
            int vT = j.JSValueGetType(ctx, valueTrue);
            assert vT == 2;
        }
        {
            JSC j = shell.jsc();
            Pointer falseScr = j.JSStringCreateWithUTF8CString("false");
            valueFalse = j.JSEvaluateScript(ctx, falseScr, null, null, 1, null);
            j.JSValueProtect(ctx, valueFalse);
            j.JSStringRelease(falseScr);
            int vF = j.JSValueGetType(ctx, valueFalse);
            assert vF == 2;
        }
    }


    @Messages({
"logo=(function() {" +
"    var logo = document.createElement(\"div\");\n" +
"    logo.id = \"dukescript.logo\";\n" +
"    logo.style.height = \"100vh\";\n" +
"    logo.style.width = \"100vw\";\n" +
"    logo.style.background = \"white\";\n" +
"    logo.style.position = \"absolute\";\n" +
"    logo.style.top = 0;\n" +
"    logo.style.zIndex = 4553425342;\n" +
"    \n" +
"    var p = document.createElement(\"p\");\n" +
"    logo.appendChild(p);\n" +
"    p.style.margin = 0;\n" +
"    p.style.position = \"absolute\";\n" +
"    p.style.top = \"30%\";\n" +
"    p.style.left = \"30%\";\n" +
"    p.style.height = \"40%\";\n" +
"    p.style.width = \"40%\";\n" +
"    \n" +
"    var svg = \"<svg version=\\\"1.1\\\" xmlns=\\\"http://www.w3.org/2000/svg\\\" xmlns:xlink=\\\"http://www.w3.org/1999/xlink\\\" x=\\\"0px\\\" y=\\\"0px\\\" width=\\\"100%\\\"\\n\" +\n" +
"\"height=\\\"100%\\\" viewBox=\\\"0 0 209 181\\\" enable-background=\\\"new 0 0 209 181\\\" xml:space=\\\"preserve\\\">\\n\" +\n" +
"\"<g id=\\\"Ebene_1\\\">\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" d=\\\"M33.814,13.423c-2.216-1.016-6.216-1.081-5.683,2.575c5.65,38.757,8.177,47.849,5.196,74.984\\n\" +\n" +
"\"c-1.543,14.044-4.297,22.644-6.056,32.889c-2.77,16.129-0.63,34.074,2.02,39.015c8.038,14.99,30.609-8.535,38.525-12.376\\n\" +\n" +
"\"c25.482-12.363,37.457,15.546,56.185,17.99C163.812,172.24,108.5,47.667,33.814,13.423z M129.14,159.032\\n\" +\n" +
"\"c-4.923,8.854-22.351-4.202-30.14-10.282c-38.75-30.25-71.25,55-67.744-17.985c1.324-16.151,5.432-24.649,7.139-39.727\\n\" +\n" +
"\"c1.448-12.792,1.438-9.705,1.583-13.29c2.648,3.858,7.855,8.585,13.225,9.906c10.403,2.559,24.584-1.584,30.009-13.153\\n\" +\n" +
"\"c1.954-4.168,2.435-9.075,1.881-13.187C113.5,91.667,139.167,141,129.14,159.032z\\\"/>\\n\" +\n" +
"\"<path fill-rule=\\\"evenodd\\\" clip-rule=\\\"evenodd\\\" fill=\\\"#E1001A\\\" d=\\\"M47.313,62.976c5.111-7.83,16.167-11.39,24.691-7.951\\n\" +\n" +
"\"c8.524,3.439,11.292,12.578,6.182,20.408c-5.111,7.83-16.167,11.391-24.691,7.951C44.971,79.945,42.203,70.806,47.313,62.976\\n\" +\n" +
"\"L47.313,62.976z\\\"/>\\n\" +\n" +
"\"<path fill-rule=\\\"evenodd\\\" clip-rule=\\\"evenodd\\\" fill=\\\"#FFFFFF\\\" d=\\\"M58.695,70.492c1.747-2.363,1.299-5.309-1-6.578\\n\" +\n" +
"\"c-2.299-1.269-5.581-0.383-7.328,1.98c-1.746,2.362-1.298,5.307,1.001,6.577C53.667,73.74,56.948,72.853,58.695,70.492\\n\" +\n" +
"\"L58.695,70.492z\\\"/>\\n\" +\n" +
"\"<path display=\\\"none\\\" fill-rule=\\\"evenodd\\\" clip-rule=\\\"evenodd\\\" fill=\\\"#1A171B\\\" d=\\\"M85.125,68.609\\n\" +\n" +
"\"c14.157-2.964,20.223-26.151,20.968-40.204c0.746-14.054-2.686-14.209-4.076-19.01c1.642-5.759,5.725-7.691,11.035-1.536\\n\" +\n" +
"\"c1.786,4.494,3.574,8.988,5.361,13.483c1.337,4.405,4.013,4.798,6.53,0.806c5.498-5.875,9.973-11.497,17.651-14.515\\n\" +\n" +
"\"c12.792-1.794,18.479,6.215,11.394,11.388c-7.085,5.173-13.945,8.24-18.51,12.519c4.389,0.952,10.321-3.283,14.712-2.331\\n\" +\n" +
"\"c18.93,5.019,11.883,15.029,0.049,12.978c-6.791-1.284-13.566-1.886-16.503,3.207c3.861,2.576,8.861,4.783,12.722,7.36\\n\" +\n" +
"\"c11.061,7.188,5.707,15.32-7.015,8.828c-8.936-4.924-12.259-6.066-18.36-5.95c-11.895,0.943-26.993,1.06-30.522,30.53\\n\" +\n" +
"\"C88.748,80.31,86.936,74.459,85.125,68.609L85.125,68.609z\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"1.0056\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M93.406,62.477\\n\" +\n" +
"\"c0,0,7.341,5.69,20.429,4.124c13.088-1.565,18.05-3.781,18.05-3.781l-0.511,5.528c0,0-13.711,4.595-21.563,4.433\\n\" +\n" +
"\"s-11.477-2.008-11.477-2.008\\\"/>\\n\" +\n" +
"\"</g>\\n\" +\n" +
"\"<g id=\\\"Ebene_3\\\" display=\\\"none\\\" opacity=\\\"0.5\\\">\\n\" +\n" +
"\"<path display=\\\"inline\\\" fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.1698\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M34.557,141.526\\n\" +\n" +
"\"c-4.617-1.773-4.743-3.455-6.242-7.292l0.224,0.241l-0.82-0.793l2.125,0.962c-7.183-0.726-7.9-6.019-1.444-9.032l-0.372,0.268\\n\" +\n" +
"\"c-0.036-1.206,4.094-3.132,5.292-3.651l-0.242,0.268l0.432-0.387c4.032,2.754,4.47,4.017,3.54,5.389\\n\" +\n" +
"\"c-0.391,0.044-0.763,0.149-1.105,0.303l-0.003-0.004c-3.002,1.345-4.039,2.692-4.145,5.605L31.871,133l2.348-0.966\\n\" +\n" +
"\"c0.212,0.457,0.538,0.859,0.942,1.177c0.25,0.634,0.519,1.287,0.86,1.944l0.804,0.233c-0.255-0.059-0.52-0.093-0.792-0.096\\n\" +\n" +
"\"c-1.183-0.014-2.242,0.529-2.896,1.363c-2.223-0.553-4.585-2.038-6.004-3.553c-3.089-3.299,7.954-17.846,11.178-6.347\\n\" +\n" +
"\"c0.049,0.174-0.273,0.434-0.77,0.721c1.904,0.038,3.413,1.506,3.364,3.283c-0.035,1.268-0.853,2.363-2.012,2.882\\n\" +\n" +
"\"c-0.382,0.172-0.426,0.352-0.903,0.382c0.018-0.03,0.046-0.072,0.077-0.124c0.177-0.033,0.353-0.079,0.52-0.137\\n\" +\n" +
"\"c-0.18,0.83-0.494,1.468-0.903,1.946c1.07,0.563,1.776,1.641,1.742,2.868c-0.049,1.787-1.654,3.225-3.573,3.203\\n\" +\n" +
"\"C35.392,141.771,34.957,141.683,34.557,141.526L34.557,141.526z M33.945,130.973c-0.355,0.458-0.706,0.963-1.03,1.534l-0.373-3.567\\n\" +\n" +
"\"c0.319,0.312,0.896,0.608,1.545,0.885c0.11-0.337,0.275-0.654,0.488-0.937c-0.35,0.158-0.641,0.294-0.815,0.393l0.164-0.092\\n\" +\n" +
"\"l-3.339,2.556l0.146-0.135l-0.499,0.742l-1.269-4.635l0.831,0.401l-0.037-0.01c2.282,0.209,3.419,1.123,4.192,2.392\\n\" +\n" +
"\"c-0.005,0.058-0.01,0.116-0.011,0.176C33.934,130.776,33.937,130.876,33.945,130.973L33.945,130.973z M37.2,135.494l0.016,0.004\\n\" +\n" +
"\"l0,0.001C37.21,135.498,37.205,135.496,37.2,135.494L37.2,135.494z\\\"/>\\n\" +\n" +
"\"<path display=\\\"inline\\\" fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.1697\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M45.602,85.672\\n\" +\n" +
"\"l-0.032,0.031c-0.021,0.02-0.04,0.04-0.062,0.06l-0.543,0.525l0.084-0.168c0.007-0.005,0.016-0.01,0.023-0.015\\n\" +\n" +
"\"c-1.059,2.888-8.112,14.721-12.837,19.039c-0.52,0.951-1.592,1.6-2.804,1.586c-1.683-0.019-3.021-1.314-2.978-2.88\\n\" +\n" +
"\"c0.025-0.894,0.497-1.687,1.207-2.201c0.877-1.405,3.868-4.971,4.419-5.626l-0.107,0.09c2.036-3.583,4.97-6.784,7.121-10.57\\n\" +\n" +
"\"l-0.095,0.077c0.357-1.553,0.906-2.584,2.107-3.747l0.004,0.004c0.577-0.589,1.408-0.959,2.324-0.949\\n\" +\n" +
"\"c1.683,0.019,3.021,1.313,2.977,2.88C46.39,84.521,46.087,85.174,45.602,85.672L45.602,85.672L45.602,85.672z\\\"/>\\n\" +\n" +
"\"<path display=\\\"inline\\\" fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.17\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M32.518,104.058\\n\" +\n" +
"\"c0.954,4.241,4.552,8.191,5.688,12.981c0.831,3.508,3.221,8.321,3.161,11.884c0,0.028-0.002,0.055-0.002,0.083\\n\" +\n" +
"\"c-0.045,1.613-1.495,2.913-3.228,2.893c-1.733-0.02-3.111-1.353-3.067-2.966c0.005-0.17,0.027-0.335,0.062-0.497\\n\" +\n" +
"\"c0-0.072-0.012-0.144-0.042-0.211l-0.077,0.408c-2.235-5.226-2.141-11.37-5.248-16.464c-1.604-2.349-2.825-4.222-3.422-6.956\\n\" +\n" +
"\"c-0.029-0.105-0.053-0.214-0.069-0.323c-0.001-0.008-0.003-0.018-0.005-0.026l0.002,0c-0.02-0.146-0.03-0.295-0.026-0.446\\n\" +\n" +
"\"c0.045-1.613,1.495-2.913,3.228-2.893C31.047,101.545,32.329,102.648,32.518,104.058L32.518,104.058z\\\"/>\\n\" +\n" +
"\"</g>\\n\" +\n" +
"\"<g id=\\\"Ebene_4\\\">\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.1808\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M34.617,81.243\\n\" +\n" +
"\"c0,0-16.439,4.068-20.909,9.503c-5.853,7.117-4.167,18.627-4.167,18.627l6.21,0.377c0,0-2.881-12.027,0.842-16.295\\n\" +\n" +
"\"s15.607-5.785,17.97-4.154S34.617,81.243,34.617,81.243z\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.1752\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M10.797,107.187\\n\" +\n" +
"\"c0,0,9.829-3.177,14.463,3.474c4.319,6.198,0.877,11.09-2.949,10.843c-3.08-0.199-1.366-5.578-3.538-8.816\\n\" +\n" +
"\"c-1.289-1.922-8.214-3.695-8.214-3.695L10.797,107.187z\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.1728\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M11.438,108.057\\n\" +\n" +
"\"c0,0,6.668,2.21,7.534,9.913c0.866,7.702-4.003,10.526-6.65,9.023c-3.372-1.915,0.906-6.976,0.85-9.198s-2.921-8.345-2.921-8.345\\n\" +\n" +
"\"L11.438,108.057z\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.1611\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M11.061,108.197\\n\" +\n" +
"\"c0,0,3.935,5.571,0.359,11.439c-3.576,5.868-8.328,3.213-8.261,0.265c0.099-4.403,2.694-2.323,3.875-3.899\\n\" +\n" +
"\"c1.181-1.576,2.342-7.558,2.342-7.558L11.061,108.197z\\\"/>\\n\" +\n" +
"\"</g>\\n\" +\n" +
"\"<g id=\\\"Õ_xBA__x2264__x201E__x5F_1\\\">\\n\" +\n" +
"\"<g>\\n\" +\n" +
"\"<path fill=\\\"#E44D26\\\" d=\\\"M155.704,135.004l-39.568-8.776l-13.817-97.022l95.498-5.021l-3.576,97.921L155.704,135.004z\\n\" +\n" +
"\"M155.704,135.004\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#F16529\\\" d=\\\"M186.467,116.291l3.056-83.678l-39.036,2.053l4.84,92.048L186.467,116.291z M186.467,116.291\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#EBEBEB\\\" d=\\\"M153.017,82.775l-0.629-11.955l-15.819,0.832l-1.736-12.186l16.912-0.889l-0.629-11.955l-0.041,0.002\\n\" +\n" +
"\"l-29.937,1.574l0.454,3.192l4.671,32.792L153.017,82.775z M153.017,82.775\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#EBEBEB\\\" d=\\\"M154.021,101.869l-0.052,0.017l-13.503-2.895l-1.352-9.49l-6.47,0.34l-5.531,0.291l2.662,18.683\\n\" +\n" +
"\"l24.846,5.512l0.054-0.018L154.021,101.869z M154.021,101.869\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#FFFFFF\\\" d=\\\"M180.137,69.361l-3.221,0.169l-24.569,1.292l0.629,11.955l14.722-0.774l-0.573,15.578l-13.144,4.3\\n\" +\n" +
"\"l0.654,12.437l24.151-8.082l0.074-2.029l1.155-31.622L180.137,69.361z M180.137,69.361\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#FFFFFF\\\" d=\\\"M181.023,45.049l-29.948,1.575l0.39,7.425l0.237,4.502l0.001,0.028l28.839-1.516l0,0l0.039-0.002\\n\" +\n" +
"\"l0.098-2.699l0.226-6.09L181.023,45.049z M181.023,45.049\\\"/>\\n\" +\n" +
"\"</g>\\n\" +\n" +
"\"</g>\\n\" +\n" +
"\"<g id=\\\"Ebene_5\\\">\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.9941\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M196.856,42.269\\n\" +\n" +
"\"c-4.961,0.632-15.361,7.522-10.62,12.342s12.368-2.849,13.583-5.493C200.636,47.34,199.689,41.908,196.856,42.269z\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.9941\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M199.336,56.076\\n\" +\n" +
"\"c-4.93-0.843-18.85,4.493-15.512,10.372c3.338,5.879,15.132-0.352,16.915-2.65C202.262,61.835,202.513,56.619,199.336,56.076z\\\"/>\\n\" +\n" +
"\"<path fill=\\\"#1A171B\\\" stroke=\\\"#1A171B\\\" stroke-width=\\\"0.9117\\\" stroke-miterlimit=\\\"2.6131\\\" d=\\\"M195.869,69.206\\n\" +\n" +
"\"c-4.586-0.049-11.338,1.877-10.051,7.232c1.237,5.148,10.566,1.719,11.936-0.57C199.061,73.681,199.241,69.242,195.869,69.206z\\\"/>\\n\" +\n" +
"\"</g>\\n\" +\n" +
"\"</svg>\\n\" +\n" +
"\"\";\n" +
"     p.innerHTML = svg;        \n" +
"     \n" +
"     var tech = document.createElement(\"div\");\n" +
"     tech.innerHTML = 'Powered by <a href=\"http://www.dukescript.com\">DukeScript.com</a> Technology';\n" +
"     tech.style.textAlign = \"center\";\n" +
"     tech.style.position = \"absolute\";\n" +
"     tech.style.bottom = 0;\n" +
"     tech.style.width = \"100%\";\n" +
"     tech.style.fontFamily = \"sans-serif\";\n" +
"     tech.style.fontSize = \"x-large\";\n" +
"     logo.appendChild(tech);\n" +
"     var script = null;\n" +
"     function closeLogo() {\n" +
"         document.body.removeChild(script);\n" +
"         document.body.removeChild(logo);\n" +
"     }\n" +
"     function warnGPL() {\n" +
"         tech.innerHTML = 'Powered by GPLv3 Technology. Visit <a href=\"http://www.dukescript.com\">DukeScript.com</a> for licensing options.';\n" +
"     }\n" +
"     var b = document.createElement(\"button\");\n" +
"     b.innerHTML = \"X\";\n" +
"     b.addEventListener('click', closeLogo)\n" +
"     logo.appendChild(b);\n" +
"     function showLogo() {\n" +
"       var body = document.getElementsByTagName('body')[0];\n" +
"       if (body) {\n" +
"         body.appendChild(logo);\n" +
"         script = document.createElement('script');\n" +
"         var x = window.innerWidth || document.documentElement.clientWidth || body.clientWidth;\n" +
"         var y = window.innerHeight || document.documentElement.clientHeight || body.clientHeight;\n" +
"         var url = 'https://dukescript.com/presenters/version-@1.js?type=@2&app=@3&width=' + x + '&height=' + y;\n" +
"         script.src = url;\n" +
"         script.onerror = warnGPL;\n" +
"         body.appendChild(script);\n" +
"         window.setTimeout(closeLogo, 15000);\n" +
"       } else {\n" + 
"         window.setTimeout(showLogo, 100);\n" +
"       }\n" +
"     }\n" +
"     showLogo();\n" +
"})();",
        "version=$version"
    })
    final void onPageLoad() {
        final String who = "WebKitPresenter:" + shell.getClass().getSimpleName();
        CharSequence logo = Strings.logo(Strings.version(), who, onPageApp);
        try {
            loadScript(new StringReader(logo.toString()));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        onPageLoad.run();
    }
    
    private static final class JSObject {
        private final Pointer value;

        public JSObject(WebKitPresenter p, Pointer val) {
            this.value = val;
            p.protect(this, val);
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof JSObject && value.equals(((JSObject)other).value);
        }
    }
    
    private static final ReferenceQueue<? super Object> QUEUE = new ReferenceQueue<Object>();
    private static final Set<Protector> ALL = new HashSet<>();
    private void protect(Object obj, Pointer pointer) {
        JSC jsc = shell.jsc();
        jsc.JSValueProtect(ctx, pointer);
        ALL.add(new Protector(obj, pointer));
        cleanProtected();
    }

    private void cleanProtected() {
        for (;;) {
            Protector p = (Protector)QUEUE.poll();
            if (p == null) {
                break;
            }
            ALL.remove(p);
            p.unprotect();
        }
    }
    private final class Protector extends PhantomReference<Object> {
        private final Pointer pointer;
        
        public Protector(Object referent, Pointer p) {
            super(referent, QUEUE);
            this.pointer = p;
        }

        public void unprotect() {
            JSC jsc = shell.jsc();
            jsc.JSValueUnprotect(ctx, pointer);
        }
    }
        
    private final class JSCFn extends Fn {
        private final Pointer fn;
        private final boolean[] keepAlive;
        
        public JSCFn(Pointer fn, boolean[] keepAlive) {
            this.fn = fn;
            this.keepAlive = keepAlive;
            protect(this, fn);
        }

        @Override
        public Object invoke(Object thiz, Object... args) throws Exception {
            cleanProtected();
            JSC jsc = shell.jsc();
            Pointer[] arr = convertFromJava(args, keepAlive);
            Pointer jsThis = thiz == null ? null : convertFromJava(thiz)[0];
            PointerByReference exception = new PointerByReference();
            Pointer ret = jsc.JSObjectCallAsFunction(ctx, fn, jsThis, arr.length, arr, exception);
            if (exception.getValue() != null) {
                throw new Exception(convertToString(jsc, exception.getValue()));
            }
            
            return convertToJava(jsc, Object.class, ret);
        }
    }

    
    public final class FnCallback implements Callback {
        private final Object vm;
        private final Method method;

        public FnCallback(Object vm, Method method) {
            this.vm = vm;
            this.method = method;
        }
        
        public Pointer call(
            Pointer jsContextRef, Pointer jsFunction, Pointer thisObject,
            int argumentCount, PointerByReference ref, Pointer exception
        ) throws Exception {
            JSC jsc = shell.jsc();
            int size = Native.getNativeSize(Pointer.class);
            Object[] args = new Object[argumentCount];
            for (int i = 0, offset = 0; i < argumentCount; i++, offset += size) {
                args[i] = convertToJava(jsc, method.getParameterTypes()[i], ref.getPointer().getPointer(offset));
            }
            return convertFromJava(method.invoke(vm, args))[0];
        }
    }
    
    private final class OnFinalize implements Callback {
        public void callback(Pointer obj) {
            Object data = shell.jsc().JSObjectGetPrivate(obj);
            java.util.Iterator<Map.Entry<Object,Object>> it = toJava.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Object,Object> entry = it.next();
                if (entry.getValue() == data) {
                    it.remove();
                    break;
                }
            }
        }
    }
    
    private static final class WeakVal extends WeakReference<Object> {
        public WeakVal(Object referent) {
            super(referent);
        }
    }

    private static String findCalleeClassName() {
        StackTraceElement[] frames = new Exception().getStackTrace();
        for (StackTraceElement e : frames) {
            String cn = e.getClassName();
            if (cn.startsWith("com.dukescript.presenters.")) { // NOI18N
                continue;
            }
            if (cn.startsWith("org.netbeans.html.")) { // NOI18N
                continue;
            }
            if (cn.startsWith("net.java.html.")) { // NOI18N
                continue;
            }
            if (cn.startsWith("java.")) { // NOI18N
                continue;
            }
            if (cn.startsWith("javafx.")) { // NOI18N
                continue;
            }
            if (cn.startsWith("com.sun.")) { // NOI18N
                continue;
            }
            return cn;
        }
        return "org.netbeans.html"; // NOI18N
    }
}
