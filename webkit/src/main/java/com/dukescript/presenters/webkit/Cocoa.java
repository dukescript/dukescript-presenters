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

import com.sun.jna.Callback;
import com.sun.jna.FromNativeContext;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeMapped;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.io.Closeable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.html.boot.spi.Fn;

public final class Cocoa implements WebKitPresenter.Shell, Callback {
    private final WebKitPresenter presenter;
    private final JSC jsc;
    private final Queue<Runnable> queue = new ConcurrentLinkedQueue<>();

    
    private AppDidStart appDidStart;
    private Ready ready;
    private ContextCreated contextCreated;
    private Pointer NSApp;
    private String page;
    private Pointer appDelPtr;
    private Pointer doMainSelector;
    private Pointer webView;

    Cocoa(WebKitPresenter presenter) {
        this.presenter = presenter;
        this.jsc = (JSC) Native.loadLibrary("JavaScriptCore", JSC.class, Collections.singletonMap(Library.OPTION_ALLOW_OBJECTS, true));
    }

    @Override
    public JSC jsc() {
        return jsc;
    }

    @Override
    public void doShow(String page) {
        this.page = page;
        
        Native.loadLibrary("WebKit", WebKit.class);
        
        appDidStart = new AppDidStart();
        contextCreated = new ContextCreated();
        ready = new Ready();
        
        ObjC objC = ObjC.INSTANCE;
	Pointer appDelClass = objC.objc_allocateClassPair(objC.objc_getClass("NSObject"), "AppDelegate", 0);
	objC.class_addMethod(appDelClass, objC.sel_getUid("applicationDidFinishLaunching:"), appDidStart, "i@:@");
        doMainSelector = objC.sel_getUid("doMain");
	objC.class_addMethod(appDelClass, doMainSelector, this, "i@");
	objC.class_addMethod(appDelClass, objC.sel_getUid("webView:didCreateJavaScriptContext:forFrame:"), contextCreated, "v@:@:@");
	objC.class_addMethod(appDelClass, objC.sel_getUid("webView:didFinishLoadForFrame:"), ready, "v@:@");
	objC.objc_registerClassPair(appDelClass);

	long appDelObj = send(objC.objc_getClass("AppDelegate"), "alloc");
        appDelPtr = new Pointer(appDelObj);
	send(appDelPtr, "init");
        
        send(appDelPtr, 
            "performSelectorOnMainThread:withObject:waitUntilDone:", 
            doMainSelector, null, 1
        );
    }

    @Override
    public void execute(Runnable command) {
        queue.add(command);
        if (Fn.activePresenter() == presenter) {
            try {
                process();
            } catch (Exception ex) {
                Logger.getLogger(WebKitPresenter.class.getName()).log(Level.SEVERE, "Cannot process " + command, ex);
            }
        } else {
            send(appDelPtr,
                "performSelectorOnMainThread:withObject:waitUntilDone:",
                doMainSelector, null, 0
            );
        }
    }
    
    private void process() throws Exception {
        try (Closeable c = Fn.activate(presenter)) {
            for (;;) {
                Runnable r = queue.poll();
                if (r == null) {
                    break;
                }
                r.run();
            }
        }
    }
    
    public interface ObjC extends Library {

        public static ObjC INSTANCE = (ObjC) Native.loadLibrary("objc.A", ObjC.class);

        public boolean class_addMethod(Pointer cls, Pointer name, Callback imp, String types);
        
        public Pointer objc_allocateClassPair(Pointer cls, String name, int additionalBytes);
        
        public Pointer objc_getClass(String name);

        public long objc_msgSend(Pointer theReceiver, Pointer theSelector, Object... arguments);
        
        public void objc_registerClassPair(Pointer cls);

        public Pointer sel_getUid(String name);
    }

    static long send(Pointer obj, String selector, Object... args) {
        Pointer uid = ObjC.INSTANCE.sel_getUid(selector);
        return ObjC.INSTANCE.objc_msgSend(obj, uid, args);
    }
    
    public static interface WebKit extends Library {
    }
    
    public void callback(Pointer self) throws Exception {
        if (NSApp != null) {
            process();
            return;
        }
        
        ObjC objC = ObjC.INSTANCE;
	long res = send(objC.objc_getClass("NSApplication"), "sharedApplication");
	if (res == 0) {
            System.err.print("Failed to initialized NSApplication...  terminating...\n");
            System.exit(1);
	}
        NSApp = new Pointer(res);
	send(NSApp, "setDelegate:", self);
	res = send(NSApp, "run");   
        System.err.println("end res: " + res);
    }
    
    public final class AppDidStart implements Callback {
        Pointer window;
        
        AppDidStart() {
        }
        
        public long callback(Pointer self) {
            ObjC objC = ObjC.INSTANCE;
            window = new Pointer(send(objC.objc_getClass("NSWindow"), "alloc"));

            Rct r = new Rct(10, 10, 1500, 900);
            int mode = 15;
            
	    send(window, 
                "initWithContentRect:styleMask:backing:defer:", 
                r, mode, 0, false
            );
            send(window, "setTitle:", nsString("Browser demo"));
            Pointer webViewClass = objC.objc_getClass("WebView");
            long webViewId = send(webViewClass, "alloc");
            webView = new Pointer(webViewId);
            send(webView, "init");
            
            send(webView, "setFrameLoadDelegate:", self);
            
            Pointer frame = new Pointer(send(webView, "mainFrame"));
            
            Pointer urlClass = objC.objc_getClass("NSURL");
            Pointer url = new Pointer(send(urlClass, "URLWithString:", nsString(page)));
            Pointer requestClass = objC.objc_getClass("NSURLRequest");
            Pointer request = new Pointer(send(requestClass, "alloc"));
            send(request, "initWithURL:", url);
            
            send(window, "setContentView:", webView);
            send(frame, "loadRequest:", request);

            send(window, "becomeFirstResponder");
            send(window, "makeKeyAndOrderFront:", NSApp);
	    return 1;
        }
    }
    
    static Pointer nsString(String bd) {
        ObjC objC = ObjC.INSTANCE;
        Pointer stringClass = objC.objc_getClass("NSString");
        Pointer browserDemo = new Pointer(send(stringClass, "stringWithCString:encoding:", bd, 4));
        return browserDemo;
    }

    public final class ContextCreated implements Callback {
        ContextCreated() {
        }
        
        public void callback(Pointer webView, Pointer ctx, Pointer frame) {
            frame = new Pointer(send(frame, "mainFrame"));
            ctx = new Pointer(send(frame, "globalContext"));            
            
            presenter.jsContext(ctx);
        }
    }
    
    public final class Ready implements Callback {
        Ready() {
        }
        
        public void callback(Pointer p1, Pointer frame) {
            send(webView, "stringByEvaluatingJavaScriptFromString:", nsString("1 + 1"));
            presenter.onPageLoad();
        }
    }
    
    public static final class Rct extends Structure implements Structure.ByValue {
        public Flt x;
        public Flt y;
        public Flt width;
        public Flt height;

        public Rct() {
        }

        public Rct(double x, double y, double width, double height) {
            this.x = new Flt(x);
            this.y = new Flt(y);
            this.width = new Flt(width);
            this.height = new Flt(height);
        }

        @Override
        protected List getFieldOrder() {
            return Arrays.asList("x", "y", "width", "height");
        }
    }    
    
    public static final class Flt extends Number implements NativeMapped {

        private static final boolean SMALL = Native.LONG_SIZE == 4;
        private final double number;

        public Flt() {
            this(0);
        }

        public Flt(double d) {
            number = d;
        }

        @Override
        public float floatValue() {
            return (float) number;
        }

        @Override
        public double doubleValue() {
            return number;
        }

        @Override
        public int intValue() {
            return (int) number;
        }

        @Override
        public long longValue() {
            return (long) number;
        }

        @Override
        public Object fromNative(Object o, FromNativeContext fromNativeContext) {
            return new Flt(((Number) o).doubleValue());
        }

        @Override
        public Object toNative() {
            return SMALL ? floatValue() : number;
        }

        @Override
        public Class<?> nativeType() {
            return SMALL ? Float.class : Double.class;
        }
    }
    
}
