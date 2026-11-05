pluginManagement {
    repositories {
        // set by tools/build-apk/build_windows.ps1 -Mirror: the Alibaba mirrors answer where google() does not
        if (providers.gradleProperty("aemu.mirror").isPresent) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (providers.gradleProperty("aemu.mirror").isPresent) {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "AEmulator"
include(":app")
