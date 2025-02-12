include(":androidApp")
include(":shared")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://raw.githubusercontent.com/a-sit-plus/gradle-conventions-plugin/mvn/repo")
    }

    plugins {
        val agpVersion = extra["agp.version"] as String
 //       val composeVersion = extra["compose.version"] as String
        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
   //     id("org.jetbrains.compose").version(composeVersion)
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

//If we have a working composite build, use it!
if (File("./vck/signum").isDirectory && File("./vck/signum/build.gradle.kts").exists()) {
    logger.warn("Detected VC-K in ${File("./vck").absolutePath}.Including it as composite build.")
    logger.warn("Including VC-K and Signum as composite build.")
    logger.warn("If you do not want this, move the VC-K to another location!")
    includeBuild("./vck/signum") {
        dependencySubstitution {
            substitute(module("at.asitplus.wallet:indispensable")).using(project(":indispensable"))
            substitute(module("at.asitplus.signum:indispensable-josef")).using(project(":indispensable-josef"))
            substitute(module("at.asitplus.signum:indispensable-cosef")).using(project(":indispensable-cosef"))
            substitute(module("at.asitplus.signum:supreme")).using(project(":supreme"))
        }
    }
    includeBuild("./vck") {
        dependencySubstitution {
            substitute(module("at.asitplus.wallet:vck")).using(project(":vck"))
            substitute(module("at.asitplus.wallet:vck-openid")).using(project(":vck-openid"))
            substitute(module("at.asitplus.wallet:vck-rqes")).using(project(":vck-rqes"))
            substitute(module("at.asitplus.wallet:vck-openid-ktor")).using(project(":vck-openid-ktor"))
            substitute(module("at.asitplus.wallet:openid-data-classes")).using(project(":openid-data-classes"))
            substitute(module("at.asitplus.wallet:dif-data-classes")).using(project(":dif-data-classes"))
            substitute(module("at.asitplus.wallet:vck-rqes")).using(project(":vck-rqes"))
            substitute(module("at.asitplus.wallet:rqes-data-classes")).using(project(":rqes-data-classes"))
        }
    }
}

val vckVersion :String get() = settings.extra["vck.version"].toString()

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
    }

    versionCatalogs {
        create("vckOidCatalog") {
            from("at.asitplus.wallet:vck-openid-ktor-versionCatalog:$vckVersion")
        }
    }
    //because the other one does not provide the transitive VC-K dependency required for XFC export
   versionCatalogs {
        create("vckCatalog") {
            from("at.asitplus.wallet:vck-versionCatalog:$vckVersion")
        }
    }

}
