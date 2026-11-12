# ProGuard / R8 Obfuscator and Optimization Rules
-repackageclasses com.andorid.crackmaster.internal
-allowaccessmodification

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Strip debug attributes
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Preserve Android Entry Points and Components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.lifecycle.ViewModel

# Preserve Data Models for JSON serialization
-keepclassmembers class com.andorid.crackmaster.api.** { *; }

# Keep Libsu
-keep class com.topjohnwu.superuser.** { *; }
-dontwarn com.topjohnwu.superuser.**

# Keep Kotlin intrinsics and metadata safe
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keepattributes *Annotation*
