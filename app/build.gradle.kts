import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.zeno.robot"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.zeno.robot"
        minSdk = 26
        targetSdk = 36
        versionCode = 47
        versionName = "1.3.14"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
        buildConfigField("String", "ZENO_API_URL", "\"\"")
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
    }
    buildFeatures { compose = true; buildConfig = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint {
        // Le projet reste volontairement sur la pile Kotlin 1.9.24 / Compose Compiler 1.5.14
        // déjà validée. Les mises à jour Jetpack récentes imposent une migration Kotlin/Compose
        // plus large et ne doivent pas être mélangées à un correctif de stabilité.
        disable += "GradleDependency"
        // Faux positif du lint récent : Context.stopService(Intent explicite) compare le composant,
        // pas l'identité de l'instance Intent comme un listener SAM.
        disable += "ImplicitSamInstance"
        warningsAsErrors = true
        abortOnError = true
    }
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
        file("src/main/res/drawable/zeno_robot.webp").delete()
        val target = file("src/main/res/drawable-nodpi/zeno_robot.webp")
        target.parentFile.mkdirs()
        target.writeBytes(robotBytes)
        println("Robot Zeno officiel installé: ${robotBytes.size} octets")
    }
}

val installZenoIcon by tasks.registering {
    doLast {
        val source = file("src/main/assets/zeno_icon_discret.b64")
        require(source.exists()) { "Icône Zeno discrète introuvable" }
        val iconBytes = Base64.getDecoder().decode(source.readText().trim())
        file("src/main/res/drawable/zeno_icon_discret.webp").delete()
        val target = file("src/main/res/drawable-nodpi/zeno_icon_discret.webp")
        target.parentFile.mkdirs()
        target.writeBytes(iconBytes)
        println("Icône Zeno discrète installée: ${iconBytes.size} octets")
    }
}

tasks.named("preBuild").configure {
    dependsOn(installZenoRobot, installZenoIcon)
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
