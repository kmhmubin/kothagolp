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
# DYNAMIC CLASS LOADING — keep classes instantiated via reflection
# ──────────────────────────────────────────────────────────────────────────────
-keepclassmembers class * extends com.kmhmubin.kothagolp.provider.MainProvider {
    public <init>();
}

# ──────────────────────────────────────────────────────────────────────────────
# DEBUGGING — preserve stack traces in crash logs
# ──────────────────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
