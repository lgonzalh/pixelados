# Reglas de ofuscación/mimificación para las builds de release.
#
# Los modelos guardados (lienzos) se serializan con kotlinx.serialization:
# sin estas reglas, R8 puede borrar los serializadores generados y los dibujos
# guardados dejarían de abrir.

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class com.pixelados.** {
    *** Companion;
}

-keepclasseswithmembers class com.pixelados.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.pixelados.**$$serializer { *; }
-keepclassmembers class com.pixelados.**$$serializer {
    *** INSTANCE;
}
