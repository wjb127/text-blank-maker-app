plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// properties 변수를 여기서 정의
val properties = org.jetbrains.kotlin.konan.properties.Properties().apply {
    load(project.rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.seungbeen.textblankmaker"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.seungbeen.textblankmaker"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // OpenAI API 키 설정
        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${properties.getProperty("openai.api.key", "")}\""
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = properties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = properties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = properties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
