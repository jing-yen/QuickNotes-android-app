plugins {
}

    android {
    compileSdkVersion 30
    buildToolsVersion "30.0.3"

    defaultConfig {
      applicationId "com.jingyen.notes"
      minSdkVersion 23
      targetSdkVersion 30
      versionCode 1
      versionName "1.0"

      testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
       release {
           minifyEnabled false
           proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
       }
    }
    }

  dependencies {

  }