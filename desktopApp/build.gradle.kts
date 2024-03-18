import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm()
    sourceSets {
         jvmMain  {
            dependencies {
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.desktop.currentOs)
                //implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation(project(":shared"))
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinMultiplatformComposeDesktopApplication"
            packageVersion = "1.0.0"
        }
    }
}
