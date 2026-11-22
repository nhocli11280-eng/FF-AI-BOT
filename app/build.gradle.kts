plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.ff.aibot"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.ff.aibot"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin { jvmToolchain(17) }

dependencies {
    implementation("com.google.mediapipe:tasks-vision:0.10.14")
}

val downloadObjectModel by tasks.registering {
    val modelFile = file("src/main/assets/efficientdet_lite0.tflite")
    outputs.file(modelFile)
    doLast {
        if (!modelFile.exists() || modelFile.length() < 1_000_000L) {
            modelFile.parentFile.mkdirs()
            val url = uri("https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/float32/1/efficientdet_lite0.tflite").toURL()
            url.openConnection().apply { connectTimeout = 30000; readTimeout = 120000 }.getInputStream().use { input ->
                modelFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn(downloadObjectModel) }
