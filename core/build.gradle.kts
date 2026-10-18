plugins { kotlin("jvm") }
kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks.test {
    // Gradle's worker bootstrap classpath is not the test runtime classpath.
    // Child JVM crash/recovery probes must load the same compiled core/tests.
    doFirst {
        systemProperty("clash.test.runtimeClasspath", sourceSets.test.get().runtimeClasspath.asPath)
    }
}
tasks.register<JavaExec>("writeAcceptanceFixture") {
    dependsOn("testClasses")
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.lis.clash.acceptance.WriteAcceptanceFixtureKt")
    args(rootProject.layout.projectDirectory.file("artifacts/acceptance/0.dat").asFile.absolutePath)
}
