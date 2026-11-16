# Quick Delivery Release ProGuard / R8 Rules
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*

# Kotlin Serialization & Coroutines
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class kotlinx.coroutines.android.** { *; }

# Firebase Firestore @IgnoreExtraProperties & Cloud Data Models
-keepclassmembers class com.example.data.cloud.** { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keepclassmembers @com.google.firebase.firestore.IgnoreExtraProperties class * {
    <fields>;
    <methods>;
}

# Google Identity / Credential Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
