plugins {
    alias(libs.plugins.android.library)
}

android {
    buildFeatures { buildConfig = true }
    namespace = "minmul.memoir.core.ai"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(projects.core.model)
    implementation(libs.play.ai.delivery)
    implementation(libs.play.services.base)
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.play.services.mlkit.text.recognition.korean)
    implementation(libs.play.services.mlkit.text.recognition.japanese)
    implementation(libs.play.services.mlkit.text.recognition.chinese)
    implementation(libs.play.services.mlkit.text.recognition.devanagari)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
