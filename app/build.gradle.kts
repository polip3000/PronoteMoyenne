plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.chaquopy)
    id("kotlin-parcelize")
}

android {
    namespace = "fr.algorythmice.pronotemoyenne"
    compileSdk {
        version = release(36)
    }

    buildFeatures{
        viewBinding = true
    }

    defaultConfig {
        applicationId = "fr.algorythmice.pronotemoyenne"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    chaquopy {
        defaultConfig{
            pip{
                install("beautifulsoup4 >= 4.8.2")
                install("pycryptodome >= 3.9.4")
                install("requests >= 2.22.0")
                install("autoslot >= 2022.12.1")

                install("mypy")
                install("types-requests")
                install("types-beautifulsoup4")
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.gson)
}