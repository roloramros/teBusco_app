import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.codram.terecojo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codram.terecojo"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val localProps = Properties().apply {
        val propsFile = rootProject.file("local.properties")
        if (propsFile.exists()) load(propsFile.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file("../te-busco-release-sign.jks")
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD") ?: ""
            keyAlias = localProps.getProperty("KEY_ALIAS") ?: ""
            keyPassword = localProps.getProperty("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://tebusco-dev.duckdns.org/\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://tebusco.duckdns.org/\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.swiperefreshlayout)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.google.maps)
    implementation(libs.google.places)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.glide)
    implementation(libs.exifinterface)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    annotationProcessor(libs.glide.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}