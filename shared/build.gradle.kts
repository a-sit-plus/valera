
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization

val ktorVersion = extra["ktor.version"] as String

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("at.asitplus.gradle.conventions")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            export("at.asitplus.wallet:vclib:3.2.0")
            export("at.asitplus.wallet:idacredential:3.2.1")
            export("at.asitplus:kmmresult:1.5.3")
            export(napier())
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api("at.asitplus.wallet:vclib:3.2.0")
                api("at.asitplus.wallet:vclib-openid:3.2.0")
                api("at.asitplus.wallet:idacredential:3.2.1")
                implementation(serialization("json"))
                api(napier())
                implementation("androidx.datastore:datastore-preferences-core:1.1.0-alpha07")
                implementation("androidx.datastore:datastore-core-okio:1.1.0-alpha07")
                implementation("org.jetbrains.kotlinx:atomicfu:0.21.0")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation ("io.ktor:ktor-client-logging:$ktorVersion")
                implementation ("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            }
        }
        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.8.1")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")

                implementation ("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
                implementation ("io.ktor:ktor-client-cio-jvm:$ktorVersion")
                implementation ("io.ktor:ktor-client-logging:$ktorVersion")
                implementation ("io.ktor:ktor-client-logging-jvm:$ktorVersion")
                implementation ("uk.uuid.slf4j:slf4j-android:1.7.30-0")

                implementation("androidx.camera:camera-camera2:1.3.0")
                implementation("androidx.camera:camera-lifecycle:1.3.0")
                implementation("androidx.camera:camera-view:1.3.0")
                implementation("com.google.accompanist:accompanist-permissions:0.30.1")
                implementation("com.google.mlkit:barcode-scanning:17.2.0")

            }
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "at.asitplus.wallet.app.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}
