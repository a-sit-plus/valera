import at.asitplus.gradle.exportIosFramework
import at.asitplus.gradle.ktor
import at.asitplus.gradle.napier
import at.asitplus.gradle.serialization
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import at.asitplus.gradle.ktor
import at.asitplus.gradle.setupDokka
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree.Companion.test
import java.net.Socket
import kotlin.concurrent.thread

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("at.asitplus.gradle.conventions")
    id("kotlin-parcelize")
    kotlin("plugin.serialization")
    id("org.jetbrains.dokka")
    id("maven-publish")
    id("signing")
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        instrumentedTestVariant {
            sourceSetTree.set(KotlinSourceSetTree.test)
        }
    }
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.ExperimentalUnsignedTypes")
        }
        commonMain {
            dependencies {
                //api(project(":common"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                api(libs.vck.openid)
                api(libs.vck)
                api(libs.credential.mdl)
                api(libs.credential.ida)
                api(libs.credential.eupid)
                api(libs.credential.powerofrepresentation)
                api(libs.credential.certificateofresidence)
                api(libs.credential.eprescription)
                implementation(serialization("json"))
                api(napier())
                implementation("androidx.datastore:datastore-preferences-core:1.1.1")
                implementation("androidx.datastore:datastore-core-okio:1.1.1")
                implementation("org.jetbrains.androidx.navigation:navigation-compose:2.8.0-alpha10")
                api(libs.atomicfu)
                implementation(ktor("client-core"))
                implementation(ktor("client-cio"))
                implementation(ktor("client-logging"))
                implementation(ktor("client-content-negotiation"))
                implementation(ktor("serialization-kotlinx-json"))
                api(ktor("client-encoding"))
                api(libs.supreme)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-common"))

                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.uiTest)
            }
        }

        androidMain {
            dependencies {
                implementation("androidx.biometric:biometric:1.2.0-alpha05")
                api("androidx.activity:activity-compose:1.8.1")
                api("androidx.appcompat:appcompat:1.6.1")
                api("androidx.core:core-ktx:1.12.0")
                implementation("uk.uuid.slf4j:slf4j-android:1.7.30-0")
                implementation(ktor("client-android"))
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

exportIosFramework(
    name = "shared", transitiveExports = false,
    libs.vck,
    libs.vck.openid,
    libs.indispensable,
    libs.supreme,
    libs.kmmresult,
    libs.credential.ida,
    libs.credential.mdl,
    libs.credential.eupid,
    libs.credential.powerofrepresentation,
    libs.credential.certificateofresidence,
    libs.credential.eprescription,
    napier(),
)

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
    testBuildType = "debug"

    //just for instrumented tests
    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore.p12")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
        create("release") {
            storeFile = file("keystore.p12")
            storePassword = "123456"
            keyAlias = "key0"
            keyPassword = "123456"
        }
    }

    sourceSets.forEach {

        //allow plain traffic and set permissions
        if (it.name.lowercase().contains("test") || name.lowercase().contains("debug"))
            it.manifest.srcFile("src/androidInstrumentedTest/AndroidManifest.xml")
    }

    dependencies {
        //androidTestImplementation(libs.runner)
        //androidTestImplementation(libs.core)
        //androidTestImplementation(libs.rules)
        //androidTestImplementation(libs.kotest.runner.android)
        androidTestImplementation(ktor("client-cio"))
        androidTestImplementation(ktor("serialization-kotlinx-json"))
        androidTestImplementation(ktor("client-content-negotiation"))
        //testImplementation(libs.kotest.extensions.android)
    }


    packaging {
        resources.excludes += ("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
        resources.excludes.add("**/attach_hotspot_windows.dll")
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
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

val startVerifier = tasks.register<DefaultTask>("startVerifier") {
    doLast {
        if (!kotlin.runCatching { Socket("localhost", 8080) }.fold(onSuccess = { true }, onFailure = { false }))
            logger.lifecycle("Starting Verifier")
        else {
            logger.lifecycle("Shutting down Verifier")
            runCatching {
                HttpClients.createDefault().let { client ->
                    logger.lifecycle("Verifier response: ${client.execute(HttpGet("http://localhost:8080/shutdown")).statusLine.statusCode}")
                }
            }.getOrElse { logger.lifecycle("Verifier not running"); it.printStackTrace() }

        }
        thread(start = true, isDaemon = false) {
            exec {
                workingDir = rootDir
                executable = "./gradlew"
                args = listOf(":verifier:jvmTest")
            }
        }

        logger.lifecycle("Waiting for Verifier to start")
        while (kotlin.runCatching { Socket("localhost", 8080) }.fold(onSuccess = { true }, onFailure = { false })) {
            Thread.sleep(1000)
            logger.lifecycle("Waiting for Verifier to start")
        }
        logger.lifecycle("Verifier started")
    }
}

val javadocJar = setupDokka(
    baseUrl = "https://github.com/a-sit-plus/warden-supreme/tree/main/",
    multiModuleDoc = true
)

publishing {
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("WARDEN Supreme Client")
                description.set("Attestation mobile client; part of the WARDEN Supreme integrated key attestation suite")
                url.set("https://github.com/a-sit-plus/warden-supreme")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("JesusMcCloud")
                        name.set("Bernd Prünster")
                        email.set("bernd.pruenster@a-sit.at")
                    }
                    developer {
                        id.set("nodh")
                        name.set("Christian Kollmann")
                        email.set("christian.kollmann@a-sit.at")
                    }
                }
                scm {
                    connection.set("scm:git:git@github.com:a-sit-plus/warden-supreme.git")
                    developerConnection.set("scm:git:git@github.com:a-sit-plus/warden-supreme.git")
                    url.set("https://github.com/a-sit-plus/warden-supreme")
                }
            }
        }
    }
    repositories {
        mavenLocal {
            signing.isRequired = false
        }
        maven {
            url = uri(layout.projectDirectory.dir("..").dir("repo"))
            name = "local"
            signing.isRequired = false
        }
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}


signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    sign(publishing.publications)
}

