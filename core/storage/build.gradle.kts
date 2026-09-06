plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

android {
    namespace = "minmul.memoir.core.storage"
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

room3 {
    schemaDirectory("$projectDir/schemas")
}

configurations.configureEach {
    if (name.contains("UnitTestRuntimeClasspath", ignoreCase = true)) {
        resolutionStrategy.dependencySubstitution {
            // Android AAR only ships device .so files; host JVM tests need the desktop natives.
            val sqliteBundledJvm = "androidx.sqlite:sqlite-bundled-jvm:${libs.versions.sqlite.get()}"
            substitute(module("androidx.sqlite:sqlite-bundled")).using(module(sqliteBundledJvm))
            substitute(module("androidx.sqlite:sqlite-bundled-android")).using(module(sqliteBundledJvm))
        }
    }
}

dependencies {
    api(libs.androidx.datastore.preferences)
    implementation(projects.core.model)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.bundled)
    ksp(libs.androidx.room3.compiler)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
