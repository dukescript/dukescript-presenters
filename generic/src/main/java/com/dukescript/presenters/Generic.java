package com.dukescript.presenters;

/*
 * #%L
 * DukeScript Generic Presenter - a library from the "DukeScript Presenters" project.
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

abstract class Generic implements Fn.Presenter, Fn.KeepAlive, Flushable {
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
        + "  console.warn(e);\n"
        + "}\n",
        
        "init=(function(global) {"
            + "\n  var fncns = new Array();"
            + "\n  var js2j = new Array();"
            + "\n  function jobject(id,value) { this.id = id; this.v = value; return this; };"
            + "\n  jobject.prototype['native'] = true;"
            + "\n  jobject.prototype.valueOf = function() { return this.v ? this.v : '[jobject ' + this.id + ']'; };"
            + "\n  jobject.prototype.toString = jobject.prototype.valueOf;"
            + "\n  var toVM = global['@2'];"
            + "\n  delete global['@2'];"
            + "\n  if (typeof toVM !== 'function') {"
            + "\n    throw 'toVM should be a function: ' + toVM;"
            + "\n  }"
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
            + "\n          }"
            + "\n          r = l;"
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
            + "\n  impl.key = @1;"
            + "\n  global.ds = function(key) {"
            + "\n    if (key != impl.key) {"
            + "\n      impl = null;"
            + "\n      console.warn('Surprising access to Java with ' + key);"
            + "\n    }"
            + "\n    return impl;"
            + "\n  };"
            + "\n  impl.toJava = toJava;"
            + "\n  impl.rg = function(id, fn) {"
            + "\n    fncns[id] = fn;"
            + "\n  };"
            + "\n  impl.fn = function(index, n, self) {"
            + "\n    var args = Array.prototype.slice.call(arguments, 3);"
            + "\n    try {"
            + "\n      var fn = fncns[index];"
            + "\n      if (typeof fn !== 'function') throw 'Cannot find function at index: ' + index + ' in ' + fn + ' apply: ' + (fn ? fn.apply : undefined);"
            + "\n      var r = fn.apply(self, args);"
            + "\n      if (n) toJava('r', r);"
            + "\n    } catch (err) {"
            + "\n      if (typeof console !== 'undefined') console.warn('Error ' + err + ' at:\\n' + err.stack);"
            + "\n      if (n) toVM('r', 'error', '' + err + ' at:\\n' + err.stack, null, null);"
            + "\n    }"
            + "\n  };"
            + "\n  impl.o = function(i) {"
            + "\n    return js2j[i];"
            + "\n  };"
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
            + "\n  impl.v = function(i) {"
            + "\n    return fncns[i];"
            + "\n  };"
            + "\n  impl.toVM = toVM;"
            + "\n  impl.toVM('r', 'OK', 'Initialized', null, null);"
            + "\n})(this);",

        "error=Cannot initialize DukeScript: @1",
        "version=$version"
    })
    final void init() {
        if (msg != null) {
            for (;;) {
                try {
                    log(Level.FINE, "Awaiting as of {0}", msg);
                    initialized.await();
                    log(Level.FINE, "Waiting is over");
                    return;
                } catch (InterruptedException ex) {
                    log(Level.INFO, "Interrupt", ex);
                }
            }
        }
        this.msg = "";
        String welcome = "";
        callbackFn(welcome, new OnReady() {
            @Override
            public void callbackReady(String clbk) {
                log(Level.FINE, "callbackReady with {0}", clbk);
                loadJS(Strings.begin(clbk).toString());
                log(Level.FINE, "checking OK state");
                if (!assertOK()) {
                    final CharSequence err = Strings.error(msg);
                    log(Level.WARNING, "no OK: {0}", err);
                    throw new IllegalStateException(err.toString());
                }
                log(Level.FINE, "assertOK");

                loadJS(Strings.init(key, clbk).toString());

                log(Level.FINE, "callbackReady: countingDown");
                initialized.countDown();
            }
        });
    }

    /** @return the name of the callback function */
    abstract void callbackFn(String welcome, OnReady onReady);
    abstract void loadJS(String js);
    
    /** Dispatches callback from JavaScript back into appropriate
     * Java implementation.
     * @param method the type of call to make
     * @param a1 first argument
     * @param a2 second argument
     * @param a3 third argument
     * @param a4 fourth argument
     * @return returned string
     * @throws Exception if something goes wrong
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
    abstract void drainQueue();

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
            "  while (v !== null && v.indexOf && v.indexOf('javascript:') === 0) {\n" +
            "    var script = v.substring(11);\n" +
            "    try {\n" +
            "      var r = eval.call(null, script);\n" +
            "    } catch (e) {  console.warn('error: ' + e + ' executing: ' + script + ' at:\\n' + e.stack); }\n" +
            "    v = ds(@3).toVM('jr', null, null, null, null);" +
            "  }\n" +
            "  return @4 ? eval('(' + v + ')') : v;\n" +
            "};\n",
        
        "fnFoot=ds(@2).rg(@1, jsvm);\n"
    })
    final Integer exportVm(Object vm) {
        int jNumber = registerObject(vm, false, null, null);
        int vmNumber = COUNTER.getAndIncrement();
        StringBuilder sb = new StringBuilder();
        sb.append(Strings.fnHead());
        for (Method m : vm.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) {
                continue;
            }
            final Class<?>[] types = m.getParameterTypes();
            boolean instanceMethod = 
                types.length > 0 && 
                m.getName().startsWith(types[0].getName().replace('.', '_') + "$");
            int params = instanceMethod ? types.length - 1 : types.length;
            sb.append(Strings.fnName(m.getName()));
            String sep;
            if (instanceMethod) {
                sb.append(Strings.fnThiz());
                sep = Strings.fnSep();
            } else {
                sep = "";
            }
            for (int i = 0; i < params; i++) {
                sb.append(sep);
                sb.append(Strings.fnParam(i));
                sep = Strings.fnSep();
            }
            sb.append(Strings.fnClose());
            if (!instanceMethod) {
                sb.append(Strings.fnNoThiz());
            }
            sb.append(Strings.fnBegin(key));
            for (int i = 0; i < params; i++) {
                sb.append(Strings.fnPPar(i, i == 0 ? "" : ","));
            }
            sb.append(Strings.fnBody(jNumber, m.getName(), key, evalJS));
        }
        sb.append(Strings.fnFoot(vmNumber, key));
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
        if (Strings.v_null().equals(typeof)) { // NOI18N
            return null;
        }
        if (Strings.v_number().equals(typeof)) { // NOI18N
            return Double.valueOf(res);
        }
        if (Strings.v_java().equals(typeof)) { // NOI18N
            return findObject(Integer.parseInt(res));
        }
        if (Strings.v_object().equals(typeof)) { // NOI18N
            return new JSObject(Integer.parseInt(res));
        }
        if (Strings.v_array().equals(typeof)) { // NOI18N
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
        if (Strings.v_boolean().equals(typeof)) { // NOI18N
            return Boolean.valueOf(res);
        }
        if (Strings.v_error().equals(typeof)) { // NOI18N
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
            sb.append(Strings.v_null());
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

    interface OnReady {
        void callbackReady(String name);
    }
    
    private class Item implements Runnable {
        final Item prev;
        Boolean done;

        final Method method;
        final Object thiz;
        final Object[] params;
        final Thread thread;
        Object result;
        
        Item(Item prev, Method method, Object thiz, Object[] params) {
            this.prev = prev;
            this.method = method;
            this.thiz = thiz;
            this.params = adaptParams(method, Arrays.asList(params));
            this.toExec = null;
            this.thread = Thread.currentThread();
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
            this.thread = Thread.currentThread();
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
        log(Level.FINE, "result@{0}: {1}", typeof, res);
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
            boolean first = call == null || Thread.currentThread() == call.thread;
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
                drainQueue();
                lock().wait(10);
            }
        }
    }

    private StringBuilder deferred;
    private Collection<Object> arguments = new LinkedList<Object>();

    @Override
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
                exec(Strings.flushExec(key).toString());
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
                    drainQueue();
                    lock().wait(10);
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
                    for (int i = 0; i < 100; i++) {
                        if ("OK".equals(msg) || "Initialized".equals(msg)) {
                            break;
                        }
                        drainQueue();
                        lock().wait(100);
                    }
                } catch (InterruptedException ex) {
                    // OK, go on and check
                }
            }
            return "OK".equals(msg) || "Initialized".equals(msg);
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
            sb.append(Strings.registerFn(id, key));
            String sep = "";
            boolean isVm = false;
            for (String n : names) {
                sb.append(sep).append(n);
                sep = ",";
                isVm = false;
                if (Strings.v_vm().equals(n)) {
                    isVm = true;
                }
            }
            sb.append(Strings.registerCode(code));
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
            sb.append(Strings.invokeImplFn(id, wait4js, key));
            encodeObject(thiz, false, sb, null);
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
