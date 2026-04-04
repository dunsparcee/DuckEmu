import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl


plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.serialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "DuckEmu"
            isStatic = true
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
        }
        commonMain.dependencies {
            implementation(libs.kamel.image)
            implementation(libs.kamel.image.default)
            implementation(libs.kstore)
            implementation(libs.kstore.file)
            implementation(libs.material.icons.extended)
            implementation(libs.kotlinx.serialization.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.okio)
            implementation(libs.lexilabs.basic.sound)
            implementation(libs.filekit.ui)
            implementation(libs.filekit.core)
            implementation(libs.compose.keyhandler)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation("net.java.jinput:jinput:2.0.10")
            runtimeOnly("net.java.jinput:jinput:2.0.10:natives-all")
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation(libs.ktor.client.java)
        }
    }
}

val unpackJinputNatives by tasks.registering(Copy::class) {
    val nativesJar = configurations.getByName("jvmRuntimeClasspath")
        .resolvedConfiguration
        .resolvedArtifacts
        .first { it.name == "jinput" && it.classifier == "natives-all" }
        .file
    from(zipTree(nativesJar)) {
        include("*.dll", "*.so", "*.dylib", "*.jnilib")
    }
    into(layout.buildDirectory.dir("jinput-natives"))
}

afterEvaluate {
    listOf("run", "jvmRun").forEach { taskName ->
        tasks.findByName(taskName)?.let { task ->
            task.dependsOn(unpackJinputNatives)
            (task as JavaExec).jvmArgs(
                "-Djava.library.path=${layout.buildDirectory.dir("jinput-natives").get().asFile.absolutePath}"
            )
        }
    }
}


android {
    namespace = "io.duckemu"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.duckemu"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

compose.desktop {

    application {
        mainClass = "io.duckemu.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "duckemu"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(file("icon.icns"))
            }
            windows {
                iconFile.set(file("icon.ico"))
            }
            linux {
                iconFile.set(file("icon.png"))
            }
        }
    }
}
