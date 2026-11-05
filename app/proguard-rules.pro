# Rental.iO Security & Obfuscation Proguard Rules

# Keep Moshi JSON data models
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keep class dev.andikune.rentalio.data.model.** { *; }

# Keep Room database and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }

# Keep DeviceAdminReceiver and services
-keep public class dev.andikune.rentalio.receiver.AdminReceiver { *; }
-keep public class dev.andikune.rentalio.service.KioskForegroundService { *; }

# Keep Retrofit and OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
