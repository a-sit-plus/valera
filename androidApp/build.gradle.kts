plugins {
    kotlin("multiplatform")
    id("com.android.application")
    id("at.asitplus.gradle.conventions")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget()
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
            implementation(libs.play.services.identity.credentials)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.registry.provider)

            implementation(libs.multipaz)
            implementation(libs.datastore.preferences.core)
        }
    }
}

val apkSignerPassword =
    (findProperty("android.cert.password") as String?) ?: System.getenv("ANDROID_CERT_PASSWORD")

android {
    namespace = "at.asitplus.wallet.app.android"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore.p12")
            storePassword = apkSignerPassword
            keyAlias = "key0"
            keyPassword = apkSignerPassword
        }
        create("release") {
            storeFile = file("keystore.p12")
            storePassword = apkSignerPassword
            keyAlias = "key0"
            keyPassword = apkSignerPassword
        }
    }
    defaultConfig {
        applicationId = "at.asitplus.wallet.app.android"
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = (findProperty("version.code") as String).toInt()
        versionName = findProperty("version.name") as String
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources.excludes += ("META-INF/versions/9/OSGI-INF/MANIFEST.MF")
    }
    // post_permissions for mulitpaz
    lint {
        baseline = file("lint-baseline.xml")
    }
}
dependencies {
    implementation(libs.core.splashscreen)
}

repositories {
    mavenLocal()
}