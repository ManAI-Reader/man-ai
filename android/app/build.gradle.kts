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
        versionCode = 4
        versionName = "0.3.0"

        testInstrumentationRunner = "com.highliuk.manai.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = System.getenv("KEYSTORE_FILE")
            if (keystoreFile != null) {
                storeFile = file(keystoreFile)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
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
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
        }
    }

    testBuildType = "staging"

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
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
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
                    // Room database abstract class
                    "*.ManAiDatabase",
                    "*.ManAiDatabase$*",
                    // Theme color scheme initializations
                    "*.ui.theme.*",
                    // Private companion synthetic getters (unreachable from tests)
                    "*.UserPreferencesRepositoryImpl${'$'}Companion",
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
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    "stagingImplementation"(libs.compose.ui.tooling)
    "stagingImplementation"(libs.compose.ui.test.manifest)

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
