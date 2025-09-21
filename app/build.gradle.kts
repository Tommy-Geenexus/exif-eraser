
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.io.FileInputStream
import java.util.Properties
import org.bouncycastle.util.encoders.Base64
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val keyStoreFile = "keystore.jks"
val keyStoreProperties = "keystore.properties"
val keyStoreBase64 = "KS_BASE_64"
val envKsPassword = "KS_PASSWORD"
val envKsKeyAlias = "KS_KEY_ALIAS"
val envKsKeyPassword = "KS_KEY_PASSWORD"

fun loadKeyStoreProperties(): Triple<String?, String?, String?> {
    val properties = Properties().apply {
        val file = File(projectDir.parent, keyStoreProperties)
        if (file.exists()) {
            load(FileInputStream(file))
        }
    }
    return Triple(
        first = properties
            .getOrDefault(envKsPassword, null)
            ?.toString()
            ?: System.getenv(envKsPassword),
        second = properties
            .getOrDefault(envKsKeyAlias, null)
            ?.toString()
            ?: System.getenv(envKsKeyAlias),
        third = properties
            .getOrDefault(envKsKeyPassword, null)
            ?.toString()
            ?: System.getenv(envKsKeyPassword)
    )
}

fun getOrCreateKeyStoreFile() = projectDir
    .parentFile
    .listFiles()
    ?.find { file -> file.extension == "jks" }
    ?.takeIf { it.exists() }
    ?: System.getenv(keyStoreBase64)
        ?.takeIf { it.isNotEmpty() }
        ?.let {
            File(projectDir.parentFile, keyStoreFile).apply {
                writeBytes(Base64.decode(it.toByteArray()))
            }
        }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.accrescent.bundletool)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.navigation.safe.args)
    alias(libs.plugins.spotless)
    alias(libs.plugins.versions)
    alias(libs.plugins.wire)
}

android {
    namespace = "com.none.tom.exiferaser"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.none.tom.exiferaser"
        minSdk = 28
        targetSdk = 36
        versionCode = 41
        versionName = "6.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val properties = loadKeyStoreProperties()
            storeFile = getOrCreateKeyStoreFile()
            storePassword = properties.first
            keyAlias = properties.second
            keyPassword = properties.third
            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
            enableV4Signing = true
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
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

bundletool {
    signingConfig {
        val properties = loadKeyStoreProperties()
        storeFile = getOrCreateKeyStoreFile()
        storePassword.set(properties.first)
        keyAlias.set(properties.second)
        keyAlias = properties.second
        keyPassword.set(properties.third)
    }
}

afterEvaluate {
    tasks.named("kspDebugKotlin") {
        dependsOn("generateDebugProtos")
    }
    tasks.named("kspReleaseKotlin") {
        dependsOn("generateReleaseProtos")
    }
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    fun isNonStable(version: String): Boolean {
        val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { keyWord ->
            version.uppercase().contains(keyWord)
        }
        val regex = "^[0-9,.v-]+(-r)?$".toRegex()
        val isStable = stableKeyword || regex.matches(version)
        return isStable.not()
    }
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "1.8"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "1.8"
}

detekt {
    baseline = file("$projectDir/config/baseline.xml")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ktlint {
    android = true
    filter {
        exclude { element -> element.file.path.contains("generated/") }
        include("**/*.kt")
    }
}

spotless {
    kotlin {
        ratchetFrom("origin/main")
        target("**/*.kt")
        licenseHeaderFile(rootProject.file("spotless/copyright.txt"))
    }
}

wire {
    kotlin {
        android = true
    }
}

dependencies {
    androidTestImplementation(libs.bundles.implementation.test.android)
    debugImplementation(libs.leakcanary)
    implementation(libs.bundles.implementation)
    ksp(libs.bundles.ksp)
    testImplementation(libs.bundles.implementation.test)
}
