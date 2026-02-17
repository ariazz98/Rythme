plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.aria.rythme"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.aria.rythme"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    // AndroidX 核心库
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // MVI 架构依赖
    implementation(libs.androidx.lifecycle.viewmodel.compose)  // ViewModel for Compose
    implementation(libs.androidx.lifecycle.runtime.compose)    // Lifecycle runtime for Compose
    implementation(libs.kotlinx.coroutines.core)              // Kotlin 协程核心库
    implementation(libs.kotlinx.coroutines.android)           // Kotlin 协程 Android 扩展
    
    // Koin 依赖注入
    implementation(libs.koin.android)                         // Koin for Android
    implementation(libs.koin.androidx.compose)                // Koin for Compose
    
    // 单元测试
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)          // 协程测试
    testImplementation(libs.koin.test)                        // Koin 测试
    
    // Android 测试
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    
    // Debug 工具
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}