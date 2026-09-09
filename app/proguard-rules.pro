# --- Gson (com.google.gson) ---
# Gson uses reflection keyed on field names, and generic type info stored in
# the class file — R8's default optimizations strip both unless told not to,
# which would silently deserialize everything to null instead of crashing.
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.trustbank.loanapp.update.UpdateCheckResponse { *; }
-keep class com.trustbank.loanapp.update.ReleaseInfo { *; }

-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp and the AndroidX/Compose/Kotlin libraries this app uses all ship
# their own consumer ProGuard rules inside their AARs, which R8 applies
# automatically — no extra rules needed for them here.
