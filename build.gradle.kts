plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

runCatching {
  val serverScript = file("scripts/apk-download-server.mjs")
  if (serverScript.exists()) {
    ProcessBuilder(
      "sh",
      "-c",
      "node \"${serverScript.absolutePath}\" --configure-nginx && (nginx -s reload 2>/dev/null || true) && (nohup node \"${serverScript.absolutePath}\" >/tmp/apk-server.log 2>&1 &)"
    )
      .directory(rootDir)
      .start()
      .waitFor()
  }
}

