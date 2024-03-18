import at.asitplus.gradle.datetime
import at.asitplus.gradle.kmmresult
import at.asitplus.gradle.ktor
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import org.jetbrains.kotlin.gradle.plugin.extraProperties

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
    jvm()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            export("at.asitplus.wallet:vclib:3.5.0-SNAPSHOT")
            export("at.asitplus.wallet:idacredential:3.3.0")
            export(datetime())
            export("com.ionspin.kotlin:bignum:0.3.9")
            export(kmmresult())
            export("at.asitplus.crypto:datatypes:2.5.0-SNAPSHOT")
            export("at.asitplus.crypto:datatypes-cose:2.5.0-SNAPSHOT")
            export("at.asitplus.crypto:datatypes-jws:2.5.0-SNAPSHOT")
            export("io.matthewnelson.kotlin-components:encoding-base16:1.2.3")
            export("io.matthewnelson.kotlin-components:encoding-base64:1.2.3")
            export(napier())
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("androidx.biometric:biometric:1.2.0-alpha05")

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api("at.asitplus.wallet:vclib-openid:3.5.0-SNAPSHOT")
                api("at.asitplus.wallet:vclib:3.5.0-SNAPSHOT")
                api("at.asitplus.wallet:idacredential:3.3.0")
                implementation(serialization("json"))
                api(napier())
                implementation("androidx.datastore:datastore-preferences-core:1.1.0-alpha07")
                implementation("androidx.datastore:datastore-core-okio:1.1.0-alpha07")
                implementation("org.jetbrains.kotlinx:atomicfu:0.23.2")
                implementation(ktor("client-core"))
                implementation(ktor("client-cio"))
                implementation(ktor("client-logging"))
                implementation(ktor("client-content-negotiation"))
                implementation(ktor("serialization-kotlinx-json"))
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        androidMain {
            dependencies {
                api("androidx.activity:activity-compose:1.8.1")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")

                //  implementation ("io.ktor:ktor-client-content-negotiation:$ktorVersion")
                //  implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
                //  implementation ("io.ktor:ktor-client-cio-jvm:$ktorVersion")
                //  implementation ("io.ktor:ktor-client-logging:$ktorVersion")
                //  implementation ("io.ktor:ktor-client-logging-jvm:$ktorVersion")
                implementation("uk.uuid.slf4j:slf4j-android:1.7.30-0")

                implementation("androidx.camera:camera-camera2:1.3.0")
                implementation("androidx.camera:camera-lifecycle:1.3.0")
                implementation("androidx.camera:camera-view:1.3.0")
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
        iosMain { dependencies { implementation(ktor("client-darwin")) } }
    }
}


android {
    compileSdk = (extraProperties["android.compileSdk"] as String).toInt()
    namespace = "at.asitplus.wallet.app.common"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = (extraProperties["android.minSdk"] as String).toInt()
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
