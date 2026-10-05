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

# --- R8 (isMinifyEnabled) kurallari ---
# Stack trace'ler okunabilsin (Sentry mapping dosyasiyla cozer).
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# kotlinx.serialization: @Serializable siniflarin serializer'lari yansima ile bulunur.
-keepclassmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.lunosfer.dreamap.**$$serializer { *; }
-keep @kotlinx.serialization.Serializable class io.lunosfer.dreamap.** { *; }
-dontwarn kotlinx.serialization.**

# Retrofit arayuzleri ve Gson/Moshi benzeri yansima ile okunan veri siniflari.
-keep interface io.lunosfer.dreamap.data.network.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn io.ktor.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Veri modelleri (JSON <-> alan adi eslesmesi)
-keep class io.lunosfer.dreamap.data.model.** { *; }
