plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// El plugin de Firebase solo se aplica si existe app/google-services.json.
// Sin ese archivo la app compila, pero al abrirla avisa que falta configurar Firebase.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn("⚠️  No se encontró app/google-services.json: la app se compilará SIN Firebase.")
}

android {
    namespace = "com.cami.maratondoomsday"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cami.maratondoomsday"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    // Llave de firma incluida en el proyecto para que el APK siempre se firme igual
    // (así se puede actualizar la app sin desinstalarla, sin importar dónde se compile).
    signingConfigs {
        create("maraton") {
            storeFile = file("maraton.keystore")
            storePassword = "maraton2026"
            keyAlias = "maraton"
            keyPassword = "maraton2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("maraton")
        }
        debug {
            signingConfig = signingConfigs.getByName("maraton")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    testImplementation("junit:junit:4.13.2")
}
