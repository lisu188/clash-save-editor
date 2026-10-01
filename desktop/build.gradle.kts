import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins { kotlin("jvm"); kotlin("plugin.compose"); id("org.jetbrains.compose") }
val packagingJdk = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) }
kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies {
    implementation(project(":core"))
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4:1.12.1")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

// Optional images from the same rendered layout tests used in CI; no game assets required.
tasks.withType<Test>().configureEach {
    providers.gradleProperty("studioPreviewDir").orNull?.let { systemProperty("clash.preview.dir", it) }
}
compose.desktop {
    application {
        mainClass = "com.lis.clash.desktop.MainKt"
        javaHome = packagingJdk.get().metadata.installationPath.asFile.absolutePath
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Dmg)
            packageName = "ClashSaveEditor"
            packageVersion = "2.0.0"
            description = "Unofficial save and scenario editor compatible with Clash"
            vendor = "Unofficial Clash Save Editor contributors"
            modules("java.desktop", "java.logging", "java.prefs", "jdk.charsets", "jdk.unsupported")
        }
    }
}
// Compose strips native JDK commands. Restore the matching console launcher for stdio MCP.
// The JVM and jli libraries already belong to this exact JDK's packaged runtime.
val prepareMcpRuntime by tasks.registering(Copy::class) {
    dependsOn("createDistributable")
    from(packagingJdk.map { it.executablePath })
    into(layout.buildDirectory.dir("compose/binaries/main/app/ClashSaveEditor/runtime/bin"))
}
tasks.register<Zip>("portableZip") {
    dependsOn(prepareMcpRuntime)
    dependsOn(":mcp:installDist")
    archiveFileName.set("ClashSaveEditor-windows-x64.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("compose/binaries/main/app"))
    from(project(":mcp").layout.buildDirectory.dir("install/mcp")) { into("mcp") }
    from(rootProject.file("packaging/ClashSaveMcp.cmd"))
}
