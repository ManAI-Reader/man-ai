import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
    alias(libs.plugins.detekt)
    jacoco
}

android {
    namespace = "com.highliuk.manai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.highliuk.manai"
        minSdk = 26
        targetSdk = 34
        versionCode = 7
        versionName = "0.6.0"

        testInstrumentationRunner = "com.highliuk.manai.HiltTestRunner"

        buildConfigField("Boolean", "DEBUG_ML", (System.getenv("DEBUG_ML") ?: "false"))
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
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }

    testBuildType = "isolated"

    @Suppress("UnstableApiUsage")
    testCoverage {
        jacocoVersion = "0.8.12"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
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

jacoco {
    toolVersion = "0.8.12"
}

configurations.matching { it.name.startsWith("jacoco") }.configureEach {
    resolutionStrategy.force("org.jacoco:org.jacoco.agent:0.8.12")
    resolutionStrategy.force("org.jacoco:org.jacoco.ant:0.8.12")
    resolutionStrategy.force("org.jacoco:org.jacoco.core:0.8.12")
    resolutionStrategy.force("org.jacoco:org.jacoco.report:0.8.12")
}

detekt {
    buildUponDefaultConfig = true
    allRules = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    basePath = projectDir.absolutePath
}

val jacocoExcludes = listOf(
    // Android generated
    "**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*",
    // Hilt generated
    "dagger/hilt/**", "hilt_aggregated_deps/**",
    "**/*_HiltModules*.*", "**/*_Factory.*", "**/*_MembersInjector.*",
    "**/*_GeneratedInjector.*", "**/*Hilt_*.*",
    // Room generated
    "**/*_Impl.*", "**/*_Impl$*.*",
    // Kotlin compiler synthetic
    "**/*ComposableSingletons*.*",
)

tasks.register<JacocoReport>("jacocoMergedReport") {
    group = "verification"
    description = "Generates merged JaCoCo coverage report for unit + instrumented tests."

    dependsOn("testIsolatedUnitTest")
    mustRunAfter("connectedIsolatedAndroidTest")

    reports {
        html.required.set(true)
        xml.required.set(true)
        csv.required.set(false)
    }

    val kotlinClasses = fileTree("${layout.buildDirectory.get()}/tmp/kotlin-classes/isolated") {
        exclude(jacocoExcludes)
    }
    val javaClasses = fileTree("${layout.buildDirectory.get()}/intermediates/javac/isolated/classes") {
        exclude(jacocoExcludes)
    }
    classDirectories.setFrom(kotlinClasses, javaClasses)

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(fileTree(layout.buildDirectory) {
        include(
            // Unit test execution data
            "outputs/unit_test_code_coverage/isolatedUnitTest/testIsolatedUnitTest.exec",
            "jacoco/testIsolatedUnitTest.exec",
            // Instrumented test execution data
            "outputs/code_coverage/isolatedAndroidTest/connected/**/*.ec",
            "outputs/managed_device_code_coverage/isolated/ciDevice/**/*.ec",
        )
    })
}

tasks.register<JacocoCoverageVerification>("jacocoCoverageVerification") {
    group = "verification"
    description = "Enforces minimum line coverage threshold."

    dependsOn("jacocoMergedReport")

    val kotlinClasses = fileTree(
        "${layout.buildDirectory.get()}/tmp/kotlin-classes/isolated"
    ) { exclude(jacocoExcludes) }
    val javaClasses = fileTree(
        "${layout.buildDirectory.get()}/intermediates/javac/isolated/classes"
    ) { exclude(jacocoExcludes) }
    classDirectories.setFrom(kotlinClasses, javaClasses)

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))

    executionData.setFrom(fileTree(layout.buildDirectory) {
        include(
            "outputs/unit_test_code_coverage/isolatedUnitTest/testIsolatedUnitTest.exec",
            "jacoco/testIsolatedUnitTest.exec",
            "outputs/code_coverage/isolatedAndroidTest/connected/**/*.ec",
            "outputs/managed_device_code_coverage/isolated/ciDevice/**/*.ec",
        )
    })

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

tasks.register("jacocoPrintCoverage") {
    group = "verification"
    description = "Prints line coverage percentage from JaCoCo XML report."

    dependsOn("jacocoMergedReport")

    doLast {
        val xmlReport = file("${layout.buildDirectory.get()}/reports/jacoco/jacocoMergedReport/jacocoMergedReport.xml")
        if (!xmlReport.exists()) {
            logger.warn("JaCoCo XML report not found at: $xmlReport")
            return@doLast
        }
        val factory = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
        val doc = factory.newDocumentBuilder().parse(xmlReport)
        val counters = doc.getElementsByTagName("counter")
        for (i in 0 until counters.length) {
            val node = counters.item(i)
            if (node.parentNode.nodeName == "report" &&
                node.attributes.getNamedItem("type").nodeValue == "LINE"
            ) {
                val missed = node.attributes.getNamedItem("missed").nodeValue.toInt()
                val covered = node.attributes.getNamedItem("covered").nodeValue.toInt()
                val total = missed + covered
                val pct = if (total > 0) covered * 100.0 / total else 0.0
                println("Coverage (JaCoCo): %.2f%% (%d/%d lines)".format(pct, covered, total))
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
    androidTestImplementation(libs.uiautomator)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.turbine)
}
