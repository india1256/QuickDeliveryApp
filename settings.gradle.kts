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

rootProject.name = "Quick Delivery"
include(":app")
// include(":rider")
// include(":admin")
include(":core")

runCatching {
  val serverScript = java.io.File(rootDir, "scripts/apk-download-server.mjs")
  if (serverScript.exists()) {
    ProcessBuilder(
      "sh",
      "-c",
      "node \"${serverScript.absolutePath}\" --configure-nginx && (nginx -s reload 2>/dev/null || true) && (pgrep -f apk-download-server.mjs >/dev/null || nohup node \"${serverScript.absolutePath}\" >/tmp/apk-server.log 2>&1 &)"
    )
      .directory(rootDir)
      .start()
      .waitFor()
  }
}

