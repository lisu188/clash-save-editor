plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij") version "1.17.3"
    id("idea")
}

group = "com.lis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

intellij {
    version.set("2023.1")
    instrumentCode.set(true)
}


sourceSets {
    main {
        java.srcDir("src")
        kotlin.srcDir("src")
        resources.srcDir("src")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "9"
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "9"
    targetCompatibility = "9"
}

val fatJar = tasks.register<Jar>("fatJar") {
    dependsOn(tasks.named("classes"))
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "com.lis.clash.ClashKt"
    }
    from(sourceSets.main.get().output)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith(".jar") }.map { zipTree(it) }
    })
}

tasks.build {
    dependsOn(fatJar)
}
