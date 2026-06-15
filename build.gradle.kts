plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
    id("idea")
}

group = "com.lis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://www.jetbrains.com/intellij-repository/releases/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    runtimeOnly("com.jetbrains.intellij.java:java-gui-forms-rt:231.8109.175")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

intellij {
    version.set("2023.1")
    instrumentCode.set(true)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    main {
        java {
            srcDir("src")
            include("**/*.java")
            exclude("test/**")
        }
        kotlin {
            srcDir("src")
            include("**/*.kt")
            exclude("test/**")
        }
        resources {
            srcDir("src")
            include("**/*.form")
            exclude("test/**")
        }
    }

    test {
        java.srcDir("src/test/kotlin")
        kotlin.srcDir("src/test/kotlin")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<org.jetbrains.intellij.tasks.InstrumentCodeTask>().configureEach {
    instrumentationLogs.set(false)
    logging.captureStandardOutput(LogLevel.INFO)
    logging.captureStandardError(LogLevel.INFO)
    doFirst {
        ant.lifecycleLogLevel = AntBuilder.AntMessagePriority.ERROR
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

val fatJar = tasks.register<Jar>("fatJar") {
    dependsOn(tasks.named("instrumentCode"))
    dependsOn(tasks.named("processResources"))
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.lis.clash.ClashKt"
    }
    from(layout.buildDirectory.dir("instrumented/instrumentCode"))
    from(layout.buildDirectory.dir("resources/main"))
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}

tasks.build {
    dependsOn(fatJar)
}

tasks.test {
    useJUnitPlatform()
}
