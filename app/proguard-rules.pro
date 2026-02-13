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

# Please add these rules to your existing keep rules in order to suppress warnings.
# This is generated automatically by the Android Gradle plugin.
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

# --- Cling / UPnP / Jetty Rules ---
-dontwarn org.fourthline.cling.**
-dontwarn org.eclipse.jetty.**
-dontwarn javax.servlet.**
-dontwarn javax.xml.**
-dontwarn org.w3c.dom.**
-dontwarn org.xml.sax.**

# Keep Cling classes (reflection used heavily)
-keep class org.fourthline.cling.** { *; }
-keep class app.vbt.hyperupnp.upnp.** { *; }

# Keep Jetty classes (used by Cling for HTTP)
-keep class org.eclipse.jetty.** { *; }

# Keep standard javax interfaces used by Cling/Jetty
-keep interface javax.servlet.** { *; }
-keep class javax.servlet.** { *; }
-keep class javax.xml.** { *; }

# Keep Android XML/DOM wrappers if used
-keep class org.w3c.dom.** { *; }
-keep class org.xml.sax.** { *; }

# Keep local UPnP Android service implementation
-keep class app.vbt.hyperupnp.androidupnp.** { *; }

# Keep Models that might be serialized/reflected
-keep class app.vbt.hyperupnp.models.** { *; }