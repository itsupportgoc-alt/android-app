# JNI entry points must keep their names (Java_com_wordcraft_... symbols in libwordcraft_core.so).
-keepclasseswithmembers class com.wordcraft.documentcore.WordCraftNativeKt {
    native <methods>;
}
-keep class com.wordcraft.documentcore.** { *; }
