import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zeno.robot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zeno.robot"
        minSdk = 26
        targetSdk = 35
        versionCode = 29
        versionName = "1.2.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "ZENO_API_URL", "\"\"")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

val installZenoRobot by tasks.registering {
    doLast {
        val parts = fileTree("src/main/assets") {
            include("zeno_robot_*.b64")
        }.files.sortedBy { it.name }
        require(parts.isNotEmpty()) { "Robot Zeno officiel introuvable" }

        val encoded = buildString {
            parts.forEach { append(it.readText().trim()) }
        }
        val robotBytes = Base64.getDecoder().decode(encoded)
        val target = file("src/main/res/drawable/zeno_robot.webp")
        target.parentFile.mkdirs()
        target.writeBytes(robotBytes)
        println("Robot Zeno officiel installé: ${robotBytes.size} octets")
    }
}

tasks.named("preBuild").configure {
    dependsOn(installZenoRobot)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom); androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.mlkit:translate:17.0.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
