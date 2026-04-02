# Add project specific ProGuard rules here.

# Keep Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.carlauncher.musicplayer.data.model.** { *; }

# AndroidX
-keep class androidx.media.** { *; }
-keep class androidx.lifecycle.** { *; }

# Keep service and receiver
-keep class com.carlauncher.musicplayer.service.** { *; }
