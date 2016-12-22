# Presenters

[![Build Status](https://travis-ci.org/dukescript/dukescript-presenters.svg?branch=master)](https://travis-ci.org/dukescript/dukescript-presenters)

## The Glue Between Java and JavaScript!

The presenters are the glue between your **Java** and **JavaScript** code
in your [DukeScript](http://dukescript.com) application. 
They make sure that your *Java* [methods with JavaScriptBody annotation](http://bits.netbeans.org/html+java/1.3/net/java/html/js/package-summary.html)
can properly pass their parameters to the *JavaScript engine* and receive results back.

The presenters make your application extremelly portable. By selecting the right presenter
when packaging your application you make sure it can run everywhere - on the *desktop*,
on *iOS*, on *Android*, in a plugin-less browser, on your Raspberry PI, via JDK's
Nashorn script engine & co. Everywhere.

### The Default Presenters

The [HTML/Java project](http://bits.netbeans.org/html+java/) offers two presenters. One
that uses **JavaFX WebView** and is suitable for running your applications on any desktop
that supports **Java**. Another that uses **Nashorn** script engine, suitable for testing
or execution of headless applications. These two presenters are excellent for normal use
and you can stick with them, however we believe those presenters can be made even better.

The aim of the *DukeScript Presenters* project is to expand the deployment options 
and sharpen the excellence of presentation by creating additional presenters.

### The Mobile Presenters

Any modern application written these days has to consider **iOS** and **Android** devices
as deployment targets. This is addressed by two presenters developer as part of this project:
* the [Android](https://github.com/dukescript/dukescript-presenters/tree/master/android) one
* the [iOS](https://github.com/dukescript/dukescript-presenters/tree/master/ios) one

The simplest way to use these presenters is to follow the [getting started tutorial](https://dukescript.com/getting_started.html). The **client-ios** and
**client-android** subprojects with properly configured presenters will be created for you.

### The WebKit Presenter

There is an alternative presenter for your *Mac OS X* and *Linux* desktop applications -
it avoids overhead of *JavaFX* and directly talks to native *WebKit* libraries giving you
access to the most recent version of *WebKit* features and close integration with the 
underlaying operating system - including native looking fonts and gestures. To use this 
presenter replace the existing presenter (probably `net.java.html.boot.fx`) by:

```xml
<dependency>
  <groupId>com.dukescript.presenters</groupId>
  <artifactId>webkit</artifactId>
  <version>1.0</version>
</dependency>
```

### Any Browser Presenter

For situations where the native WebKit libraries aren't present, we offer an alternative:
launch any browser yourself and let us connect your **Java** code with it! 
This is the task for the *browser* presenter. 
It starts local server and launches specified browser that connects to the server.
To use it specify following in your `pom.xml` file:

```xml
<dependency>
  <groupId>com.dukescript.presenters</groupId>
  <artifactId>browser</artifactId>
  <version>1.0</version>
</dependency>
```
The actual browser to be launched can be influenced by value of `com.dukescript.presenters.browser` property. It can have following values:

* **GTK** - use Gtk WebKit implementation. Requires presence of appropriate native libraries
* **AWT** - use Desktop.browse(java.net.URI) to launch a browser
* **NONE** - just launches the server, useful together with `com.dukescript.presenters.browserPort property` that can specify a fixed port to open the server at
* any other value is interpreted as a command which is then launched on a command line with one parameter - the URL to connect to

If the property is not specified the system tries GTK mode first, followed by AWT and then tries to execute `xdg-open` (default LINUX command to launch a browser from a shell script).

The *browser presenter* has been successfully used to deploy [DukeScript](http://dukescript.com) application on Raspberry PI.

## License

The DukeScript Presenters are licensed under **GPLv3** license, but the [DukeScript Support](https://dukescript.com/index.html#support) is ready to offer your more business friendly license.
