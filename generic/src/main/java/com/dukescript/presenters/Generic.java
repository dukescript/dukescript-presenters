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

import static com.dukescript.presenters.Strings.*;
import com.dukescript.presenters.strings.Messages;
import java.io.Flushable;
import java.io.IOException;
import java.io.Reader;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import org.netbeans.html.boot.spi.Fn;

public abstract class Generic implements Fn.Presenter, Fn.KeepAlive, Flushable {
    private String msg;
    private Item call;
    private final NavigableSet<Exported> exported;
    private final int key;
    private final boolean synchronous;
    private final boolean evalJS;
    private final String type;
    private final String app;
    private final CountDownLatch initialized = new CountDownLatch(1);
    
    Generic(
        boolean synchronous, boolean evalJS, String type, String app
    ) {
        this.exported = new TreeSet<Exported>();
        this.key = (int)(System.currentTimeMillis() / 777) % 1000;
        this.synchronous = synchronous;
        this.evalJS = evalJS;
        this.type = type;
        this.app = app;
    }
    
    final Object lock() {
        return initialized;
    }
    
    abstract void log(Level level, String msg, Object... args);
    
    @Messages({
        "begin=try {\n"
        + "  @1('r', 'OK', 'OK', null, null);\n"
        + "} catch (e) {\n"
        + "  alert(e);\n"
        + "}\n",
        
        "init=(function(global) {"
            + "\n  var fncns = new Array();"
            + "\n  var js2j = new Array();"
            + "\n  function jobject(id,value) { this.id = id; this.v = value; return this; };"
            + "\n  jobject.prototype['native'] = true;"
            + "\n  jobject.prototype.valueOf = function() { return this.v ? this.v : '[jobject ' + this.id + ']'; };"
            + "\n  var toVM = global['@2'];"
            + "\n  delete global['@2'];"
            + "\n  function toJava(method, r) {"
            + "\n      var t = typeof r;"
            + "\n      if (t === 'function') t = 'object';"
            + "\n      if (t === 'undefined' || r === null) {"
            + "\n        t = 'null';"
            + "\n        r = null;"
            + "\n      } else if (t === 'object') {"
            + "\n        if (r['native']) {"
            + "\n          t = 'java';"
            + "\n          r = r.id;"
            + "\n        } else if (Object.prototype.toString.call(r) === '[object Array]') {"
            + "\n        t = 'array';"
            + "\n        var l = r.length + ':';"
            + "\n        for (var i = 0; i < r.length; i++) {"
            + "\n            var toObj = toJava(null, r[i]);"
            + "\n            l += toObj.length + ':' + toObj;"
            + "\n          }\n"                
            + "\n          r = l;\n"
            + "\n        } else {"
            + "\n          var size = js2j.length;"
            + "\n          js2j.push(r);"
            + "\n          r = size;"
            + "\n        }"
            + "\n      }"
            + "\n      if (method !== null) toVM(method, t, r, null, null);"
            + "\n      else return t + ':' + r;"
            + "\n  }"
            + "\n  var impl = {};"
            + "\n  global.ds = function(key) {"
            + "\n    if (key != @1) {\n"
            + "\n      impl = null;\n"
            + "\n      alert('Surprising access to Java with ' + key);"
            + "\n    }"
            + "\n    return impl;"
            + "\n  };"
            + "\n  impl.toJava = toJava;"
            + "\n  impl.vm = function(id, vm) { fncns[id] = vm; };"
            + "\n  impl.rg = function(id, fn) { fncns[id] = fn; };"
            + "\n  impl.fn = function() {"
            + "\n    var index = arguments[0];"
            + "\n    var n = arguments[1];"
            + "\n    var self = arguments[2];"
            + "\n    var args = Array.prototype.slice.call(arguments, 3);"
            + "\n    try {"
            + "\n      var r = fncns[index].apply(self, args);"
            + "\n      if (n) toJava('r', r);"
            + "\n    } catch (err) {"
            + "\n      if (typeof console !== 'undefined') console.warn('Error ' + err);"
            + "\n      if (n) toVM('r', 'error', '' + err, null, null);"
            + "\n    }"
            + "\n  };"
            + "\n  impl.o = function(i) {\nreturn js2j[i];\n};"
            + "\n  impl.j = function(n,v) {"
            + "\n   var r = new jobject(n,v);"
            + "\n   if (arguments.length > 2) {"
            + "\n     for (var i = 2; i < arguments.length; i++) {"
            + "\n       r[i - 2] = arguments[i];"
            + "\n     }"
            + "\n     r.length = arguments.length - 2;"
            + "\n   }"
            + "\n   return r;"
            + "\n  };"
            + "\n  impl.v = function(i) {\nreturn fncns[i];\n};"
            + "\n  impl.toVM = toVM;"
            + "\n  impl.toVM('r', 'OK', 'Initialized', null, null);"
            + "\n})(this);",

        "error=Cannot initialize DukeScript!",
        "version=$version"
    })
    final void init() {
        if (msg != null) {
            for (;;) {
                try {
                    initialized.await();
                    return;
                } catch (InterruptedException ex) {
                    log(Level.INFO, "Interrupt", ex);
                }
            }
        }
        this.msg = "";
        String clbk = callbackFn(Strings.logo(
            Strings.version(), type, app
        ).toString());
        loadJS(begin(clbk).toString());
        if (!assertOK()) {
            throw new IllegalStateException(error());
        }
        
        loadJS(Strings.init(key, clbk).toString());
        
        initialized.countDown();
    }

    @Messages(
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
"})();"
    )
    /** @return the name of the callback function */
    abstract String callbackFn(String welcome);
    abstract void loadJS(String js);
    
    /** Dispatches callbacks from JavaScript back into appropriate
     * Java implementation.
     */
    final String callback(String method, 
        String a1, String a2, String a3, String a4
    ) throws Exception {
        if ("r".equals(method)) {
            result(a1, a2);
            return null;
        } else if ("c".equals(method)) {
            return javacall(a1, a2, a3, a4);
        } else if ("jr".equals(method)) {
            return javaresult();
        } else {
            throw new IllegalArgumentException(method);
        }
    }
    
    abstract void dispatch(Runnable r);

    /** Makes sure all pending calls into JavaScript are immediately 
     * performed. 
     * 
     * @throws IOException if something goes wrong
     */
    @Override
    public void flush() throws IOException {
        if (initialized.getCount() == 0) {
            flushImpl();
        }
    }
    
    @Override
    public Fn defineFn(String code, String[] names, boolean[] keepAlive) {
        init();
        return new GFn(code, names, keepAlive);
    }    
    
    @Override
    public Fn defineFn(String code, String... names) {
        init();
        return new GFn(code, names, null);
    }
    
    private static final class Key extends WeakReference<Object> {
        private int hash;
        
        public Key(Object obj) {
            super(obj);
            this.hash = System.identityHashCode(obj);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 47 * hash + this.hash;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key other = (Key)obj;
                if (hash != other.hash) {
                    return false;
                }
                return true;
            }
            return false;
        }
    }
    
    private Map<Key,Integer> ids = new HashMap<Key, Integer>();
    int identityHashCode(Object o) {
        Key k = new Key(o);
        Integer val = ids.get(k);
        if (val == null) {
            int s = ids.size();
            ids.put(k, s);
            return s;
        }
        return val;
    }
    
    final int registerObject(Object o, boolean weak, boolean[] justAdded, String[] valueOf) {
        if (o instanceof Enum && valueOf != null) {
            valueOf[0] = o.toString();
        }
        int id = identityHashCode(o);
        for (;;) {
            Object exp = findObject(id);
            if (o == exp) {
                return id;
            }
            if (exp == null) {
                if (justAdded != null) {
                    justAdded[0] = true;
                }
                exported.add(new Exported(id, weak, o));
                return id;
            }
            throw new IllegalStateException("Collision!");
        }
    }
    
    final Object findObject(int id) {
        Exported obj = exported.floor(new Exported(id, false, null));
        return obj == null || obj.id != id ? null : obj.get();
    }
    
    @Messages({
        "fnHead=var jsvm = {};\n",
        "fnName=jsvm.@1 = function(",
        "fnThiz=thiz",
        "fnNoThiz=  var thiz = null;\n",
        "fnSep=,",
        "fnParam=p@1",
        "fnClose=) {\n",
        "fnBegin=  var encParams = ds(@1).toJava(null, [",
        "fnPPar=@2 p@1",
        "fnBody=]);\n" +
            "  var v = ds(@3).toVM('c', @1, '@2', thiz ? thiz.id : null, encParams);\n" +
//            "  alert('toVM: ' + v + ' inner: ' + (v !== null && v.indexOf && v.indexOf('javascript:') === 0) + ' typeof: ' + (typeof v) +\n" +
            "  while (v !== null && v.indexOf && v.indexOf('javascript:') === 0) {\n" +
            "    var script = v.substring(11);\n" +
//            "    alert('script: ' + script +\n" +
            "    try {\n" +
            "      var r = eval.call(null, script);\n" +
//            "    alert('r: ' + r +\n" +
            "    } catch (e) {  alert('error: ' + e + ' executing: ' + script); }\n" +
            "    v = ds(@3).toVM('jr', null, null, null, null);" +
//            "    alert('javaresult: ' + v +\n" +
            "  }\n" +
            "  return @4 ? eval('(' + v + ')') : v;\n" +
            "};\n",
        
        "fnFoot=ds(@2).rg(@1, jsvm);\n"
    })
    final Integer exportVm(Object vm) {
        int jNumber = registerObject(vm, false, null, null);
        int vmNumber = COUNTER.getAndIncrement();
        StringBuilder sb = new StringBuilder();
        sb.append(fnHead());
        for (Method m : vm.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) {
                continue;
            }
            final Class<?>[] types = m.getParameterTypes();
            boolean instanceMethod = 
                types.length > 0 && 
                m.getName().startsWith(types[0].getName().replace('.', '_') + "$");
            int params = instanceMethod ? types.length - 1 : types.length;
            sb.append(fnName(m.getName()));
            String sep;
            if (instanceMethod) {
                sb.append(fnThiz());
                sep = fnSep();
            } else {
                sep = "";
            }
            for (int i = 0; i < params; i++) {
                sb.append(sep);
                sb.append(fnParam(i));
                sep = fnSep();
            }
            sb.append(fnClose());
            if (!instanceMethod) {
                sb.append(fnNoThiz());
            }
            sb.append(fnBegin(key));
            for (int i = 0; i < params; i++) {
                sb.append(fnPPar(i, i == 0 ? "" : ","));
            }
            sb.append(fnBody(jNumber, m.getName(), key, evalJS));
        }
        sb.append(fnFoot(vmNumber, key));
        deferExec(sb);
        return vmNumber;
    }

    @Messages({
        "v_null=null",
        "v_number=number",
        "v_java=java",
        "v_object=object",
        "v_array=array",
        "v_boolean=boolean",
        "v_error=error"
    })
    final Object valueOf(String typeof, String res) {
        if (v_null().equals(typeof)) { // NOI18N
            return null;
        }
        if (v_number().equals(typeof)) { // NOI18N
            return Double.valueOf(res);
        }
        if (v_java().equals(typeof)) { // NOI18N
            return findObject(Integer.parseInt(res));
        }
        if (v_object().equals(typeof)) { // NOI18N
            return new JSObject(Integer.parseInt(res));
        }
        if (v_array().equals(typeof)) { // NOI18N
            int at = res.indexOf(':');
            int size = Integer.parseInt(res.substring(0, at));
            Object[] arr = new Object[size];
            at++;
            for (int i = 0; i < size; i++) {
                int next = res.indexOf(':', at);
                int length = Integer.parseInt(res.substring(at, next));
                at = next + 1 + length;
                arr[i] = valueOf(res.substring(next + 1, at));
            }
            return arr;
        }
        if (v_boolean().equals(typeof)) { // NOI18N
            return Boolean.valueOf(res);
        }
        if (v_error().equals(typeof)) { // NOI18N
            throw new IllegalStateException(res);
        }
        return res;
    }
    
    final Object valueOf(String typeAndValue) {
        int colon = typeAndValue.indexOf(':');
        return valueOf(typeAndValue.substring(0, colon), typeAndValue.substring(colon + 1));
    }

    final void encodeObject(Object a, boolean weak, StringBuilder sb, int[] vmId) {
        if (a == null) {
            sb.append(v_null());
        } else if (a.getClass().isArray()) {
            int len = Array.getLength(a);
            sb.append('[');
            String sep = "";
            for (int i = 0; i < len; i++) {
                Object o = Array.get(a, i);
                sb.append(sep);
                encodeObject(o, weak, sb, null);
                sep = ",";
            }
            sb.append(']');
        } else if (a instanceof Number) {
            sb.append(a.toString());
        } else if (a instanceof String) {
            sb.append('"');
            String s = (String)a;
            int len = s.length();
            for (int i = 0; i < len; i++) {
                char ch = s.charAt(i);
                switch (ch) {
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\"': sb.append("\\\""); break;
                    default:
                        sb.append(ch);
                        break;
                }
            }
            sb.append('"');
        } else if (a instanceof Boolean) {
            sb.append(a.toString());
        } else if (a instanceof Character) {
            sb.append((int)(Character)a);
        } else if (a instanceof JSObject) {
            sb.append("ds(").append(key).append(").o(").append(((JSObject) a).index).append(")");
        } else {
            if (vmId != null) {
                sb.append("ds(").append(key).append(").v(").append(vmId[0]).append(")");
            } else {
                String[] valueOf = { null };
                sb.append("ds(").append(key).append(").j(").append(registerObject(a, weak, null, valueOf));
                sb.append(",");
                encodeObject(valueOf[0], weak, sb, null);
                if (a instanceof Object[]) {
                    for (Object n : ((Object[])a)) {
                        sb.append(",");
                        encodeObject(n, weak, sb, null);
                    }
                }
                sb.append(")");
            }
        }
    }
    
    private class Item implements Runnable {
        final Item prev;
        Boolean done;

        final Method method;
        final Object thiz;
        final Object[] params;
        Object result;
        
        Item(Item prev, Method method, Object thiz, Object[] params) {
            this.prev = prev;
            this.method = method;
            this.thiz = thiz;
            this.params = adaptParams(method, Arrays.asList(params));
            this.toExec = null;
        }
        
        
        protected final String inJavaScript(boolean[] finished) {
            if (this.method != null) {
                return js(finished);
            } else {
                return sj(finished);
            }
        }
        protected final void inJava() {
            if (this.method == null) {
                return;
            }
            if (done == null) {
                done = false;
                try {
                    log(Level.FINE, "Calling {0}", method);
                    result = method.invoke(thiz, params);
                } catch (Exception ex) {
                    log(Level.SEVERE, "Cannot invoke " + method + " on " + thiz + " with " + Arrays.toString(params), ex);
                } finally {
                    done = true;
                    log(Level.FINE, "Result: {0}", result);
                }
            }
        }
        
        @Override public void run() {
            synchronized (lock()) {
                log(Level.FINE, "run: {0}", this);
                inJava();
                lock().notifyAll();
            }
        }
        

        protected String js(boolean[] finished) {
            if (Boolean.TRUE.equals(done)) {
                StringBuilder sb = new StringBuilder();
                encodeObject(result, false, sb, null);
                finished[0] = true;
                return sb.toString();
            }
            return null;
        }

        private final String toExec;
        private String typeof;
        
        public Item(Item prev, String toExec) {
            this.prev = prev;
            this.toExec = toExec;
            
            this.method = null;
            this.params = null;
            this.thiz = null;
        }

        protected String sj(boolean[] finished) {
            finished[0] = false;
            if (Boolean.TRUE.equals(done)) {
                return null;
            }
            done = true;
            return "javascript:" + toExec;
        }

        protected final void result(String typeof, String result) {
            if (this.method != null) {
                throw new UnsupportedOperationException();
            }
            this.typeof = typeof;
            this.result = result;
            log(Level.FINE, "result ({0}): {1} for {2}", typeof, result, toExec);
        }
    } // end of Item
    
    final void result(String typeof, String res) {
        synchronized (lock()) {
            if ("OK".equals(typeof)) {
                log(Level.FINE, "init: {0}", res);
                this.msg = res;
                lock().notifyAll();
                return;
            }
            call.result(typeof, res);
            call = call.prev;
            lock().notifyAll();
        }
    }

    final String javacall(
            String vmNumber, String fnName, String thizId, String encParams
    ) throws Exception {
        synchronized (lock()) {
            Object vm = findObject(Integer.parseInt(vmNumber));
            assert vm != null;
            final Object obj = thizId == null || "null".equals(thizId)
                    ? null : valueOf("java", thizId);
            Method method = null;
            for (Method m : vm.getClass().getMethods()) {
                if (m.getName().equals(fnName)) {
                    method = m;
                    break;
                }
            }
            assert method != null;
            List<Object> params = new ArrayList<Object>();
            if (obj != null) {
                params.add(obj);
            }
            params.addAll(Arrays.asList((Object[]) valueOf(encParams)));
            Object[] converted = adaptParams(method, params);
            boolean first = call == null;
            log(Level.FINE, "jc: {0}@{1}args: {2} is first: {3}, now: {4}", new Object[]{method.getName(), vm, params, first, call});
            call = new Item(call, method, vm, converted);
            if (first || synchronous) {
                if (call != null) {
                    dispatch(call);
                }
            } else {
                lock().notifyAll();
            }
            return javaresult();
        }
    }

    final String javaresult() throws IllegalStateException, InterruptedException {
        synchronized (lock()) {
            boolean[] finished = {false};
            for (;;) {
                if (deferred != null) {
                    deferred.insert(0, "javascript:");
                    String ret = deferred.toString();
                    deferred = null;
                    return ret;
                }
                finished[0] = false;
                String jsToExec = call.inJavaScript(finished);
                log(Level.FINE, "jr: {0} jsToExec: {1} finished: {2}", new Object[]{call, jsToExec, finished[0]});
                if (jsToExec != null) {
                    if (finished[0]) {
                        call = call.prev;
                    }
                    return jsToExec;
                }
                lock().wait();
            }
        }
    }

    private StringBuilder deferred;
    private Collection<Object> arguments = new LinkedList<Object>();

    public final void loadScript(final Reader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        char[] arr = new char[4092];
        for (;;) {
            int len = reader.read(arr);
            if (len == -1) {
                break;
            }
            sb.append(arr, 0, len);
        }
        deferExec(sb);
    }


    final void deferExec(StringBuilder sb) {
        synchronized (lock()) {
            log(Level.FINE, "deferExec: {0} empty: {1}, call: {2}", new Object[]{sb, deferred == null, call});
            if (deferred == null) {
                deferred = sb;
            } else {
                deferred.append(sb);
            }
        }
    }

    @Messages({
        "flushExec=\n\nds(@1).toJava('r',null);\n"
    })
    void flushImpl() {
        synchronized (lock()) {
            if (deferred != null) {
                log(Level.FINE, "flush: {0}", deferred);
                exec(flushExec(key).toString());
            }
        }
    }

    Object exec(String fn) {
        Object ret;
        boolean first;
        synchronized (lock()) {
            if (deferred != null) {
                deferred.append(fn);
                fn = deferred.toString();
                deferred = null;
                log(Level.FINE, "Flushing {0}", fn);
            }

            Item myCall;
            boolean load;
            if (call != null) {
                call = myCall = new Item(call, fn);
                lock().notifyAll();
                load = synchronous;
                first = false;
            } else {
                call = myCall = new Item(null, null);
                load = true;
                first = true;
            }
            if (load) {
                loadJS(fn);
            }
            for (;;) {
                if (myCall.typeof != null) {
                    break;
                }
                try {
                    lock().wait();
                } catch (InterruptedException ex) {
                    log(Level.SEVERE, null, ex);
                }
                if (call != null) {
                    call.inJava();
                }
                lock().notifyAll();
            }
            ret = valueOf(myCall.typeof, (String) myCall.result);
        }
        if (first) {
            arguments.clear();
        }
        return ret;
    }

    final boolean assertOK() {
        synchronized (lock()) {
            if (msg == null || msg.length() == 0) {
                try {
                    lock().wait(10000);
                } catch (InterruptedException ex) {
                    // OK, go on and check
                }
            }
            return "OK".equals(msg);
        }
    }
    
    private static Object[] adaptParams(Method toCall, List<Object> args) {
        final Object[] arr = new Object[args.size()];
        final Class<?>[] types = toCall.getParameterTypes();
        for (int i = 0; i < arr.length; i++) {
            arr[i] = adaptType(types[i], args.get(i));
        }
        return arr;
    }
    
    private static Object adaptType(Class<?> type, Object value) {
        if (type.isPrimitive() && value instanceof Number) {
            final Number n = (Number)value;
            if (type == Byte.TYPE) return n.byteValue();
            if (type == Short.TYPE) return n.shortValue();
            if (type == Integer.TYPE) return n.intValue();
            if (type == Long.TYPE) return n.longValue();
            if (type == Float.TYPE) return n.floatValue();
            if (type == Double.TYPE) return n.doubleValue();
            if (type == Character.TYPE) return (char)n.intValue();
        }
        return value;
    }
    
    private static final class JSObject {
        private final int index;

        public JSObject(int index) {
            this.index = index;
        }

        @Override
        public int hashCode() {
            return 37 * this.index;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final JSObject other = (JSObject) obj;
            return this.index == other.index;
        }

        @Messages({
            "jsObject=[jsobject-@1]"
        })
        @Override
        public String toString() {
            return Strings.jsObject(index).toString();
        }
        
    } // end of JSObject
    
    static final AtomicInteger COUNTER = new AtomicInteger(0);
    @Messages({
        "registerFn=ds(@2).rg(@1, function(",
        "registerCode=) {\n@1\n});",
        "v_vm=vm"
    })
    private final class GFn extends Fn {
        private final int id;
        private final int[] vmId;
        private final boolean[] keepAlive;
        
        public GFn(String code, String[] names, boolean[] ka) {
            super(Generic.this);
            this.id = COUNTER.getAndIncrement();
            this.keepAlive = ka;
            
            StringBuilder sb = new StringBuilder(1024);
            sb.append(registerFn(id, key));
            String sep = "";
            boolean isVm = false;
            for (String n : names) {
                sb.append(sep).append(n);
                sep = ",";
                isVm = false;
                if (v_vm().equals(n)) {
                    isVm = true;
                }
            }
            sb.append(registerCode(code));
            this.vmId = isVm ? new int[] { -1 } : null;
            deferExec(sb);
        }

        @Override
        public Object invoke(Object thiz, Object... args) throws Exception {
            return invokeImpl(true, thiz, args);
        }

        @Override
        public void invokeLater(Object thiz, Object... args) throws Exception {
            invokeImpl(false, thiz, args);
        }

        @Messages({
            "invokeImplFn=ds(@3).fn(@1, @2, "
        })
        private Object invokeImpl(boolean wait4js, Object thiz, Object... args) throws Exception {
            if (vmId != null && vmId[0] < 0) {
                vmId[0] = exportVm(args[args.length - 1]);
            }
            
            StringBuilder sb = new StringBuilder(256);
            sb.append(invokeImplFn(id, wait4js, key));
            encodeObject(thiz, keepAlive != null, sb, null);
            for (int i = 0; i < args.length; i++) {
                sb.append(", ");
                boolean weak = keepAlive != null && !keepAlive[i];
                encodeObject(args[i], weak, sb, i == args.length - 1 ? vmId : null);
            }
            sb.append(");");
            
            arguments.add(thiz);
            arguments.add(args);

            if (wait4js) {
                return exec(sb.toString());
            } else {
                deferExec(sb);
                return null;
            }
        }
    }
    
    private static final class Exported implements Comparable<Exported> {
        private final int id;
        private final Object obj;
        private final boolean ref;

        Exported(int id, boolean ref, Object obj) {
            this.id = id;
            this.obj = ref ? createReferenceFor(obj) : obj;
            this.ref = ref;
            WeakHolder.clean();
        }
        
        protected Object get() {
            if (ref) {
                return ((Reference<?>)obj).get();
            } else {
                return obj;
            }
        }
        
        @Override
        public int compareTo(Exported o) {
            return id - o.id;
        }

        private static Object createReferenceFor(Object obj) {
            Reference<Object> ref = new WeakReference<Object>(obj);
            if (obj instanceof Reference) {
                Reference<?> myRef = (Reference<?>) obj;
                if (obj.getClass().getName().equals("org.netbeans.html.ko4j.Knockout")) {
                    // workaround for #255677
                    WeakHolder h = new WeakHolder(myRef.get(), obj);
                    h.register();
                }
            }
            return ref;
        }
    }

    private static final class WeakHolder extends PhantomReference<Object> {
        private static final ReferenceQueue QUEUE = new ReferenceQueue();
        private static final Set<WeakHolder> active = new HashSet<WeakHolder>();
        private final Object knockout;

        public WeakHolder(Object referent, Object knockout) {
            super(referent, QUEUE);
            this.knockout = knockout;
        }

        static void clean() {
            for (;;) {
                WeakHolder h = (WeakHolder) QUEUE.poll();
                if (h == null) {
                    break;
                }
                active.remove(h);
            }
        }

        void register() {
            active.add(this);
        }
    }
}
