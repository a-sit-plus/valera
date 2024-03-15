
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
            export("at.asitplus.wallet:vclib:3.4.0")
            export("at.asitplus.wallet:idacredential:3.4.0")
            export("at.asitplus:kmmresult:1.5.3")
            export(napier())
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("androidx.biometric:biometric:1.2.0-alpha05")

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api("at.asitplus.wallet:vclib-openid:3.4.0")
                api("at.asitplus.wallet:vclib:3.4.0")
                api("at.asitplus.wallet:idacredential:3.4.0")
                implementation(serialization("json"))
                api(napier())
                implementation("androidx.datastore:datastore-preferences-core:1.1.0-beta02")
                implementation("androidx.datastore:datastore-core-okio:1.1.0-beta02")
                implementation("org.jetbrains.kotlinx:atomicfu:0.23.2")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-cio:$ktorVersion")
                implementation ("io.ktor:ktor-client-logging:$ktorVersion")
                implementation ("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.8.2")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")

                implementation ("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
                implementation ("io.ktor:ktor-client-cio-jvm:$ktorVersion")
                implementation ("io.ktor:ktor-client-logging:$ktorVersion")
                implementation ("io.ktor:ktor-client-logging-jvm:$ktorVersion")
                implementation ("uk.uuid.slf4j:slf4j-android:1.7.30-0")

                implementation("androidx.camera:camera-camera2:1.3.2")
                implementation("androidx.camera:camera-lifecycle:1.3.2")
                implementation("androidx.camera:camera-view:1.3.2")
                implementation("com.google.accompanist:accompanist-permissions:0.30.1")
                implementation("com.google.mlkit:barcode-scanning:17.2.0")

            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.compose.ui:ui-test-junit4")
                implementation("androidx.compose.ui:ui-test-manifest")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies{
                implementation("io.ktor:ktor-client-darwin:$ktorVersion")
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    packaging {
        resources.excludes.add("META-INF/versions/9/previous-compilation-data.bin")
    }
    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api33") {
                    device = "Pixel 2"
                    apiLevel = 33
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}
