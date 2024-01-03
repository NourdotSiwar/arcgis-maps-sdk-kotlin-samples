apply plugin: 'com.android.application'
apply plugin: 'org.jetbrains.kotlin.android'

android {
    compileSdkVersion rootProject.ext.compileSdkVersion

    defaultConfig {
        applicationId "com.esri.arcgismaps.sample.addscenelayerwithelevation"
        minSdkVersion rootProject.ext.minSdkVersion
        targetSdkVersion rootProject.ext.targetSdkVersion
        versionCode rootProject.ext.versionCode
        versionName rootProject.ext.versionName
        buildConfigField("String", "API_KEY", API_KEY)
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "$kotlinCompilerExt"
    }

    namespace 'com.esri.arcgismaps.sample.addscenelayerwithelevation'
}

dependencies {
    // lib dependencies from rootProject build.gradle
    implementation "androidx.core:core-ktx:$ktxAndroidCore"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:$ktxLifecycle"
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:$ktxLifecycle"
    implementation "androidx.activity:activity-compose:$composeActivityVersion"
    // Jetpack Compose Bill of Materials
    implementation platform("androidx.compose:compose-bom:$composeBOM")
    // Jetpack Compose dependencies
    implementation "androidx.compose.ui:ui"
    implementation "androidx.compose.material3:material3"
    implementation "androidx.compose.ui:ui-tooling"
    implementation "androidx.compose.ui:ui-tooling-preview"
    implementation project(path: ':samples-lib')
}
