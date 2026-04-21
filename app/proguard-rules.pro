# Keep data model classes used with kotlinx.serialization
-keepattributes *Annotation*
-keep @kotlinx.serialization.Serializable class ** { *; }

# Keep kotlinx.serialization internals
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }

# DataStore
-keep class androidx.datastore.** { *; }

# Kotlin
-dontwarn kotlin.**
-keep class kotlin.Metadata { *; }
