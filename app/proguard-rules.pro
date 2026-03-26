# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Suppress warnings for optional/unused dependencies ---
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# --- Cling UPnP (uses reflection for XML binding, SOAP actions, service types) ---
# Keep annotated and reflected classes in Cling's model layer
-keep class app.vbt.hyperupnp.upnp.cling.model.** { *; }
-keep class app.vbt.hyperupnp.upnp.cling.support.model.** { *; }
-keep class app.vbt.hyperupnp.upnp.cling.binding.** { *; }
-keep class app.vbt.hyperupnp.upnp.cling.transport.** { *; }

# Keep Cling service types (instantiated by reflection)
-keep class app.vbt.hyperupnp.upnp.cling.registry.** { *; }
-keep class app.vbt.hyperupnp.upnp.cling.controlpoint.** { *; }
-keep class app.vbt.hyperupnp.upnp.cling.protocol.** { *; }

# Keep Seamless XML/HTTP utilities used by Cling
-keep class app.vbt.hyperupnp.upnp.seamless.xml.** { *; }
-keep class app.vbt.hyperupnp.upnp.seamless.http.** { *; }

# --- Jetty (used by Cling for HTTP transport) ---
-dontwarn org.eclipse.jetty.**
-keep class org.eclipse.jetty.server.** { *; }
-keep class org.eclipse.jetty.servlet.** { *; }
-keep class org.eclipse.jetty.client.** { *; }
-keep class org.eclipse.jetty.util.thread.** { *; }
-keep class org.eclipse.jetty.http.** { *; }
-keep class org.eclipse.jetty.io.** { *; }

# --- javax.servlet (required by Jetty) ---
-dontwarn javax.servlet.**
-keep class javax.servlet.http.HttpServlet { *; }
-keep class javax.servlet.http.HttpServletRequest { *; }
-keep class javax.servlet.http.HttpServletResponse { *; }
-keep interface javax.servlet.Servlet { *; }

# --- Android UPnP service (bound service, instantiated by framework) ---
-keep class app.vbt.hyperupnp.androidupnp.AndroidUpnpServiceImpl { *; }

# --- Keep standard XML classes used by Cling's XML parsing ---
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**
