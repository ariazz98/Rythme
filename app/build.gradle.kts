plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
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
        buildConfig = true
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
    
    // 媒体播放依赖
    implementation(libs.androidx.media3.exoplayer)            // ExoPlayer 核心
    implementation(libs.androidx.media3.session)              // MediaSession 支持
    implementation(libs.androidx.media3.ui)                   // UI 组件
    implementation(libs.androidx.palette)                     // 调色板（提取封面主题色）
    
    // UI 依赖
    implementation(libs.androidx.compose.material.icons)      // Material Icons
    implementation(libs.coil.compose)                         // Coil 图片加载
    
    // 数据存储依赖
    implementation(libs.androidx.datastore.preferences)       // DataStore Preferences
    implementation(libs.androidx.room.runtime)                // Room 数据库
    implementation(libs.androidx.room.ktx)                    // Room Kotlin 扩展
    ksp(libs.androidx.room.compiler)                          // Room 注解处理器
    
    // Debug 工具
    debugImplementation(libs.androidx.compose.ui.tooling)
}