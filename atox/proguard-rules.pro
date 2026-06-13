# Add project specific ProGuard rules here.
-dontwarn org.conscrypt.**
-dontwarn okhttp3.internal.platform.**

# Keep JNI classes and their data models
-keep class ltd.evilcorp.core.tox.** { *; }

# Keep Room database, DAOs, and entities
-keep class ltd.evilcorp.core.db.** { *; }

# Keep domain model classes for database mapping and backup serialization
-keep class ltd.evilcorp.domain.features.**.model.** { *; }
