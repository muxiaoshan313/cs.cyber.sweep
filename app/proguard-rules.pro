-obfuscationdictionary www.txt
# 指定class模糊字典
-classobfuscationdictionary www.txt
# 指定package模糊字典
-packageobfuscationdictionary www.txt

# Prevent warning messages about Retrofit
-dontwarn retrofit2.**

# Keep Retrofit classes
-keep class retrofit2.** { *; }

# Keep annotations used by Retrofit
-keepattributes Signature
-keepattributes Exceptions

# Keep OkHttp classes
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Prevent warning messages about javax annotations
-dontwarn javax.annotation.**
-dontwarn javax.inject.**

# Keep OkHttp internal classes
-keepclassmembers class okhttp3.internal.** { *; }


-keep class com.google.firebase.** { *; }

-keep class com.google.** { *; }
-keep class com.airbnb.** { *; }
-keep class com.alibaba.** { *; }
-keepclassmembers class scsc.step.cash.api.response.** { *; }
-keepclassmembers class scsc.step.cash.bean.UserInfo { *; }



# Firebase Analytics
-keep class com.google.android.gms.measurement.** { *; }
-keep class com.google.firebase.analytics.** { *; }

# Firebase Messaging
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.android.gms.iid.** { *; }

# Firebase Crashlytics
-keep class io.fabric.api.** { *; }
-keep class io.fabric.sdk.android.** { *; }
-keep class com.crashlytics.** { *; }

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.tencent.mmkv.** { *; }

# Firebase Database
-keep class com.google.firebase.database.** { *; }

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase ML Kit
-keep class com.google.mlkit.** { *; }

# Firebase Performance Monitoring
-keep class com.google.firebase.perf.** { *; }
-keep class com.google.firebase.perf.network.** { *; }

# Firebase Dynamic Links
-keep class com.google.firebase.dynamiclinks.** { *; }

# Firebase Remote Config
-keep class com.google.firebase.remoteconfig.** { *; }

# Google Play Services Base
-keep class com.google.android.gms.** { *; }

# Google Play Services Location
-keep class com.google.android.gms.location.** { *; }

# Google Play Services Ads
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.ads.identifier.** { *; }

# Google Play Services Maps
-keep class com.google.android.gms.maps.** { *; }

# Google Play Services Places
-keep class com.google.android.gms.location.places.** { *; }
-keep class scsc.step.cash.daemon.AttributionCheckTask {
    public <init>();
    public void start(android.app.Application);
}
-dontwarn javax.lang.model.element.Modifier
