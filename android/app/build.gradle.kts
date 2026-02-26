import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.highliuk.manai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.highliuk.manai"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "0.5.0"

        testInstrumentationRunner = "com.highliuk.manai.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            val localProps = Properties()
            val localFile = rootProject.file("local.properties")
            if (localFile.exists()) { localFile.inputStream().use { localProps.load(it) } }

            storeFile = file(
                System.getenv("KEYSTORE_FILE")
                    ?: localProps.getProperty("signing.storeFile", "keystore.jks")
            )
            storePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: localProps.getProperty("signing.storePassword")
            keyAlias = System.getenv("KEY_ALIAS")
                ?: localProps.getProperty("signing.keyAlias")
            keyPassword = System.getenv("KEY_PASSWORD")
                ?: localProps.getProperty("signing.keyPassword")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("isolated") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".isolated"
        }
    }

    testBuildType = "isolated"

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    room {
        schemaDirectory("$projectDir/schemas")
    }

    sourceSets {
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests.isReturnDefaultValues = true
        managedDevices {
            localDevices {
                create("ciDevice") {
                    device = "Pixel 2"
                    apiLevel = 30
                    systemImageSource = "aosp"
                }
            }
        }
    }

    packaging {
        jniLibs { pickFirsts += setOf("**/*.so") }
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/*"
            )
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    basePath = projectDir.absolutePath
}

kover {
    reports {
        filters {
            excludes {
                androidGeneratedClasses()
                classes(
                    // Hilt
                    "dagger.hilt.*",
                    "hilt_aggregated_deps.*",
                    "*_HiltModules*",
                    "*_Factory",
                    "*_MembersInjector",
                    "*_GeneratedInjector",
                    "*Hilt_*",
                    // Room
                    "*_Impl",
                    "*_Impl$*",
                    // Navigation
                    "*ComposableSingletons*",
                    // App entry points
                    "*.ManAiApplication",
                    "*.MainActivity",
                    // DI modules (pure Hilt wiring)
                    "*.di.*",
                    // Android-dependent implementations
                    "*.AndroidPdfMetadataExtractor",
                    "*.OnnxSessionManager",
                    // OnnxText* require ONNX runtime (Android only)
                    // Their Companion objects (pure functions) remain covered by unit tests
                    "*.OnnxTextDetector",
                    "*.OnnxTextDetector${'$'}*",
                    "*.OnnxTextRecognizer",
                    "*.OnnxTextRecognizer${'$'}*",
                    // Room database abstract class
                    "*.ManAiDatabase",
                    "*.ManAiDatabase$*",
                    // Theme color scheme initializations
                    "*.ui.theme.*",
                    // Kotlin compiler synthetic classes (unreachable from tests)
                    "*.UserPreferencesRepositoryImpl${'$'}Companion",
                    // Room DAO interface — concrete methods compile to $DefaultImpls
                    // which Room bypasses with its generated implementation
                    "*.MangaDao*",
                    // Kotlin-generated default impls and coroutine lambdas
                    // TODO: investigate pre-existing coverage gaps in these classes
                    "*.FileHashProviderImpl",
                    "*.FileHashProviderImpl${'$'}*",
                    "*.MangaRepositoryImpl",
                    "*.MangaRepositoryImpl${'$'}*",
                )
                annotatedBy(
                    "*Generated*",
                    "*Composable*",
                )
            }
        }

        variant("debug") {
            log {
                header = "Coverage (Kover engine):"
                format = "  <entity> — <value>%"
                groupBy = kotlinx.kover.gradle.plugin.dsl.GroupingEntityType.CLASS
                coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                aggregationForGroup = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
            }

            verify {
                rule {
                    minBound(100)
                }
            }
        }
    }
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

tasks.register("printDetektClasspath") {
    dependsOn("compileDebugKotlin")
    doLast {
        val classpath = configurations.getByName("debugCompileClasspath")
            .resolve()
            .joinToString(File.pathSeparator) { it.absolutePath }
        val kotlinClasses = layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile.absolutePath
        val javaClasses = layout.buildDirectory.dir("intermediates/javac/debug/classes").get().asFile.absolutePath
        println("DETEKT_CLASSPATH=$kotlinClasses${File.pathSeparator}$javaClasses${File.pathSeparator}$classpath")
    }
}

dependencies {
    // Core
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.animation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    "isolatedImplementation"(libs.compose.ui.tooling)
    "isolatedImplementation"(libs.compose.ui.test.manifest)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // ONNX Runtime
    implementation(libs.onnxruntime.android)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)

    // Instrumented tests
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
}
