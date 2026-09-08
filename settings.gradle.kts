pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "Memoir"
include(":app")
include(":core:model")
include(":core:storage")
include(":core:ai")
include(":core:design")
include(":data:content")
include(":data:model")
include(":data:preferences")
include(":feature:onboarding")
include(":feature:intake")
include(":feature:main")
include(":feature:queue")
include(":feature:search")
include(":feature:settings")
include(":background:analysis")
