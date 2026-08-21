plugins {
    id("com.android.application")
    id("at.asitplus.gradle.conventions")
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

val apkSignerPassword =
    (findProperty("android.cert.password") as String?) ?: System.getenv("ANDROID_CERT_PASSWORD")

android {
    namespace = "at.asitplus.wallet.app.android"

    sourceSets["main"].apply {
        manifest.srcFile("src/androidMain/AndroidManifest.xml")
        kotlin.directories += "src/androidMain/kotlin"
        res.directories += "src/androidMain/res"
        assets.directories += "src/androidMain/assets"
    }

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
        resources.excludes += ("META-INF/AL2.0")
        resources.excludes += ("META-INF/LGPL2.1")
        resources.excludes += ("META-INF/LICENSE.md")
    }
    // post_permissions for mulitpaz
    lint {
        baseline = file("lint-baseline.xml")
    }
}
dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.registry.provider)
    implementation(libs.koin.core)
    implementation(libs.multipaz)
    implementation(libs.datastore.preferences.core)
    implementation(libs.core.splashscreen)
}
