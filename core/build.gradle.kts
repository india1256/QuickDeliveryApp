plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.example.core"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
    ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
    ignoreList.add(".*SECRET.*")
    ignoreList.add(".*PRIVATE.*")
    ignoreList.add(".*WEBHOOK.*")
    ignoreList.add(".*SMS_API_KEY.*")
    ignoreList.add(".*ADMIN_PIN.*")
}

dependencies {
    api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    api(platform(libs.androidx.compose.bom))
    api(platform(libs.firebase.bom))
    api(libs.firebase.firestore)
    api(libs.firebase.auth)
    api(libs.firebase.messaging)
    api(libs.androidx.credentials)
    api(libs.androidx.credentials.play.services)
    api(libs.googleid)
    api(libs.androidx.activity.compose)
    api(libs.androidx.compose.material.icons.core)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.graphics)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.navigation.compose)
    api(libs.coil.compose)
    api(libs.converter.moshi)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.coroutines.core)
    api(libs.logging.interceptor)
    api(libs.moshi.kotlin)
    api(libs.okhttp)
    api(libs.play.services.location)
    api(libs.retrofit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
