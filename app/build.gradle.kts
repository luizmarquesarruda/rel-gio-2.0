plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.luizmarquesarruda.relogio20"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("boolean", "IS_TEST", providers.gradleProperty("IS_TEST").orElse("false").get())
        applicationId = "com.luizmarquesarruda.relogio20"
        minSdk = 26
        targetSdk = 36
        versionCode = 9
        versionName = "0.4.0-alpha.3"
        testInstrumentationRunner = "android.test.runner.AndroidJUnitRunner"
    }


    flavorDimensions += "distribution"
    productFlavors {
        create("diagnostic") {
            dimension = "distribution"
            applicationIdSuffix = ".diagnostic"
            versionNameSuffix = "-diagnostic"
        }
        create("companion") {
            dimension = "distribution"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
}
