import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    version.set("1.8.0")
    android.set(true)
    outputToConsole.set(true)
    ignoreFailures.set(true)
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
        reporter(ReporterType.SARIF)
    }
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
        versionCode = 10
        versionName = "0.4.0-alpha.4"
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

    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        // Lint is a blocking quality gate; keep warnings visible without hiding legacy debt.
        abortOnError = true
        checkReleaseBuilds = true
        checkAllWarnings = false
        warningsAsErrors = false
        htmlReport = true
        xmlReport = true
        sarifReport = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// AGP creates variant configurations after the build script is evaluated.
// Attach these dependencies when the exact configurations become available.
configurations.configureEach {
    when (name) {
        "diagnosticDebugImplementation" ->
            dependencies.add(project.dependencies.create("com.squareup.leakcanary:leakcanary-android:2.14"))
        "diagnosticBenchmarkImplementation" ->
            dependencies.add(project.dependencies.create("androidx.profileinstaller:profileinstaller:1.4.1"))
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.13.0")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    testImplementation("junit:junit:4.13.2")
}
