import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// ============ VERSIONADO SECUENCIAL ============
// El APK generado usa el versionCode leído aquí (configuración); cuando el
// APK se genera exitosamente, installPixeladosApk avanza el contador en
// app/version.properties para el siguiente build. Así la etiqueta avanza
// solo en cada GENERACIÓN real de APK: alfa → beta → gamma → delta → ...
val versionProps = Properties().apply {
    val f = rootProject.file("app/version.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
var currentVersionCode = (versionProps.getProperty("versionCode") ?: "1").toInt()
val currentVersionName = versionProps.getProperty("versionName") ?: "1.0"
val buildStartTime = System.currentTimeMillis()

android {
    namespace = "com.pixelados"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.pixelados"
        // Android 8.0 (API 26) en adelante: así entra en teléfonos más viejos
        // (el guardado en galería tiene camino alternativo para API < 29).
        minSdk = 26
        targetSdk = 36
        versionCode = currentVersionCode
        versionName = currentVersionName
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "pixelados.apk"
        }
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE,NOTICE}"
        }
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Core AndroidX
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.core:core-ktx:1.13.1")
    // Splash screen con el logo de la app (relevo automático al tema normal)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Serialization para guardar proyectos
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Photo Picker
    implementation("androidx.activity:activity:1.9.0")

    // MediaStore para exportar
    implementation("androidx.media:media:1.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// ============ AUTO-INSTALACIÓN TRAS CADA GENERACIÓN DE APK ============
// Tras cada assemble (debug o release) se instala el APK en el dispositivo
// conectado / emulador configurado, sin excepciones. Si no hay dispositivo,
// advierte en consola pero no rompe la compilación.
val adbPath: String = run {
    val sdk = System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: run {
            val lp = rootProject.file("local.properties")
            if (lp.exists()) Properties().apply { lp.inputStream().use { load(it) } }
                .getProperty("sdk.dir") ?: ""
            else ""
        }
    val home = if (!sdk.isNullOrBlank()) sdk else {
        // Fallback típico de Android Studio en Windows
        "${System.getProperty("user.home")}/AppData/Local/Android/Sdk"
    }
    val ext = if (org.gradle.internal.os.OperatingSystem.current().isWindows) ".exe" else ""
    val candidates = listOf(
        "$home/platform-tools/adb$ext",
        "$home/platform-tools/adb"
    )
    candidates.firstOrNull { file(it).exists() } ?: candidates.first()
}

val debugApkPath = file("build/outputs/apk/debug/pixelados.apk")

tasks.register("installPixeladosApk") {
    description = "Instala app/build/outputs/apk/debug/pixelados.apk en el dispositivo conectado vía adb"
    group = "pixelados"
    doLast {
        val apk = debugApkPath
        if (!apk.exists()) {
            println(">>> [installPixeladosApk] APK no encontrado en ${apk.path}")
            return@doLast
        }
        // 1) Versionado secuencial: solo si el APK se generó en ESTA ejecución
        if (apk.lastModified() >= buildStartTime) {
            val newCode = currentVersionCode + 1
            rootProject.file("app/version.properties").writeText(
                """
                # Versionado secuencial de Pixelados.
                # versionCode se incrementa automáticamente en cada generación del APK
                # (task installPixeladosApk, tras empaquetar con éxito) y define la
                # etiqueta de build: 1=alfa, 2=beta, 3=gamma, ... (ver VersionHelper.kt)
                versionCode=$newCode
                versionName=$currentVersionName
                """ .trimIndent() + "\n"
            )
            println(
                ">>> [installPixeladosApk] APK generado con versionCode=$currentVersionCode " +
                    "(Ver $currentVersionName). Próximo build: versionCode=$newCode"
            )
        } else {
            println(">>> [installPixeladosApk] APK sin cambios (up-to-date); no se avanza la versión.")
            return@doLast
        }

        // 2) Publicar en <raíz>/apk/pixelados.apk (nombre fijo según especificación)
        val published = rootProject.file("apk/pixelados.apk")
        published.parentFile.mkdirs()
        apk.copyTo(published, overwrite = true)
        println(">>> [installPixeladosApk] APK publicado en ${published.path}")

        // 3) Instalación automática en el dispositivo/emulador conectado
        val adb = adbPath
        val devicesOutput = providers.exec {
            commandLine(adb, "devices")
        }.standardOutput.asText.get()
        val devices = devicesOutput.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("List of devices") && !it.endsWith("offline") }
            .map { it.substringBefore("\t") }
            .filter { it.isNotBlank() }
            .toList()
        if (devices.isEmpty()) {
            println(">>> [installPixeladosApk] Sin dispositivo/emulador conectado. APK listo en ${apk.path} (no se instaló).")
        } else {
            devices.forEach { device ->
                println(">>> [installPixeladosApk] Instalando en $device ...")
                val exit = providers.exec {
                    commandLine(adb, "-s", device, "install", "-r", "-d", apk.absolutePath)
                }.result.get().exitValue
                println(
                    if (exit == 0) ">>> [installPixeladosApk] Instalado correctamente en $device"
                    else ">>> [installPixeladosApk] ERROR instalando en $device (exit=$exit)"
                )
            }
        }
    }
}

tasks.matching { it.name in setOf("assembleDebug") }.configureEach {
    finalizedBy("installPixeladosApk")
}
