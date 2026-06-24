# Add project specific ProGuard rules here.

# ──────────────────────────────────────────────────────────────────────────────
# SOURCE API — classes referenced by dynamically-loaded sources APK
#
# The sources APK is compiled against these classes by name. R8 must NOT rename
# or remove them, or DexClassLoader will throw NoClassDefFoundError at runtime.
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.kmhmubin.kothagolp.provider.** { *; }
-keep interface com.kmhmubin.kothagolp.provider.** { *; }

# Domain model — returned/accepted by provider methods
-keep class com.kmhmubin.kothagolp.domain.model.** { *; }
-keep interface com.kmhmubin.kothagolp.domain.model.** { *; }

# NetworkClient — base class helper methods call this directly
-keep class com.kmhmubin.kothagolp.data.remote.NetworkClient { *; }
-keep class com.kmhmubin.kothagolp.data.remote.NetworkClient$** { *; }

# Keep Kotlin metadata so reflection inside DexClassLoader works
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# ──────────────────────────────────────────────────────────────────────────────
# KOTLIN RUNTIME — suspend functions compile to (params..., Continuation): Object
# R8 renames kotlin.coroutines.Continuation → e.g. "yl0". sources.apk was compiled
# against the real name; the JVM sees different method signatures → AbstractMethodError.
# Keep all kotlin.coroutines types so the binary API surface matches what sources.apk expects.
# ──────────────────────────────────────────────────────────────────────────────
-keep class kotlin.coroutines.** { *; }
-keep interface kotlin.coroutines.** { *; }
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.jvm.functions.** { *; }
-keepclassmembers class kotlin.** { *; }

# kotlinx.coroutines — providers in sources.apk use Dispatchers, withContext, etc.
# DexClassLoader uses parent-first delegation: main app's classloader resolves these.
# If R8 renames kotlinx.coroutines types, providers can't find them by original name.
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# DYNAMIC CLASS LOADING — keep classes instantiated via reflection
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.kmhmubin.kothagolp.provider.MainProvider {
    public <init>();
}

# ──────────────────────────────────────────────────────────────────────────────
# NETWORKING — entire data.remote package; CloudflareManager, WebViewSolver,
# NetworkException and MemoryCookieJar are referenced by name from sources APK
# loaded at runtime via DexClassLoader and must keep their original class names
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.kmhmubin.kothagolp.data.remote.** { *; }
-keep interface com.kmhmubin.kothagolp.data.remote.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# JSOUP — HTML parser used by all providers; R8 must not rename internal classes
# ──────────────────────────────────────────────────────────────────────────────
-keep public class org.jsoup.** { *; }
-keep interface org.jsoup.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# OKHTTP / OKIO — OkHttp 4.x ships consumer rules in its AAR; do NOT add broad
# -keep rules here. That keeps optional BouncyCastle/Conscrypt platform adapters
# which reference non-Android classes → NoClassDefFoundError at runtime.
# -dontwarn is safe (suppresses build warnings only).
# ──────────────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ──────────────────────────────────────────────────────────────────────────────
# KOTLINX SERIALIZATION — prevent R8 from stripping generated serializers
# ──────────────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep all generated $$serializer classes for app models
-keep,includedescriptorclasses class com.kmhmubin.kothagolp.**$$serializer { *; }
-keepclassmembers class com.kmhmubin.kothagolp.** {
    *** Companion;
}
-keepclasseswithmembers class com.kmhmubin.kothagolp.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable annotated classes and their members
-keep @kotlinx.serialization.Serializable class com.kmhmubin.kothagolp.** { *; }

# ──────────────────────────────────────────────────────────────────────────────
# GOOGLE API CLIENT + DRIVE + AUTH
# These libraries use reflection and dynamic JSON deserialization. R8 must not
# rename or remove them or GoogleClientSecrets.load() will throw at runtime,
# causing isConfigured() to silently return false.
# ──────────────────────────────────────────────────────────────────────────────
-keep class com.google.api.** { *; }
-keep interface com.google.api.** { *; }
-keep class com.google.auth.** { *; }
-keep interface com.google.auth.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.common.** { *; }
-dontwarn com.google.api.**
-dontwarn com.google.auth.**
-dontwarn com.google.common.**
-dontwarn javax.annotation.**

# ──────────────────────────────────────────────────────────────────────────────
# DEBUGGING — preserve stack traces in crash logs
# ──────────────────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
