# Chickie Runway — R8 rules.
# Release currently ships with isMinifyEnabled=false (staged R8). These rules are ready for when
# minification is enabled after a verified non-minified release.

# ---- Kotlinx Serialization ----
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt

# Keep generated serializers and companion lookups for @Serializable classes (data + nav routes).
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# App models and type-safe navigation routes are serialized by name.
-keep @kotlinx.serialization.Serializable class com.chickierunway.game.data.** { *; }
-keep @kotlinx.serialization.Serializable class com.chickierunway.game.ui.navigation.** { *; }

-dontwarn kotlinx.serialization.**
