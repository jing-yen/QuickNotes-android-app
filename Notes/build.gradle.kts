plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlinx-serialization")
    id("com.squareup.sqldelight")
    id("com.google.gms.google-services")
}

android {
    signingConfigs {
        create("release") {
            storeFile = file("C:\\Users\\Public\\AndroidStudioProjects\\keystore.jks")
            storePassword = "Transpro2016"
            keyAlias = "key0"
            keyPassword = "Transpro2016"
        }
    }
    compileSdk = (30)
    buildToolsVersion = "30.0.3"

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.jingyen.notes"
        minSdk = (23)
        targetSdk = (30)
        versionCode = 3
        versionName = "0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        signingConfig = signingConfigs.getByName("release")
    }

    buildTypes {
       release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility  = JavaVersion.VERSION_1_8
        targetCompatibility  = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
    implementation("com.squareup.sqldelight:android-driver:1.5.0")
    implementation("com.squareup.sqldelight:coroutines-extensions-jvm:1.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.0")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.5.0")

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
    implementation ( platform("com.google.firebase:firebase-bom:27.1.0"))
    implementation ("com.google.firebase:firebase-analytics-ktx:19.0.0")
    implementation ("androidx.core:core-ktx:1.6.0")
    implementation ("androidx.appcompat:appcompat:1.4.0-alpha03")
    implementation ("androidx.emoji2:emoji2:1.0.0-alpha03")
    implementation ("androidx.emoji2:emoji2-bundled:1.0.0-alpha03")
    implementation ("com.google.android.material:material:1.4.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.0-beta02")
    implementation("androidx.datastore:datastore-preferences:1.0.0-rc01")
    implementation ("com.google.android.exoplayer:exoplayer-core:2.14.1")
    implementation("androidx.biometric:biometric:1.2.0-alpha03")
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
}