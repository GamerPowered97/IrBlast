# IrBlast R8 / ProGuard Optimization Rules

# Keep Domain Models used in state, serialization, and database persistence
-keep class com.example.irblast.domain.model.** { *; }

# Keep IR Encoders, Decoders, and Protocol Handlers
-keep class com.example.irblast.data.ir.** { *; }
-keepclassmembers enum com.example.irblast.data.ir.** { *; }

# Keep SQLite helper and database classes
-keep class com.example.irblast.data.db.** { *; }

# Keep Settings and Backup Manager
-keep class com.example.irblast.data.settings.** { *; }

# Retain reflection/annotation attributes needed by Compose and Kotlin
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Remove verbose logging in production release builds for maximum speed and zero latency
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}
