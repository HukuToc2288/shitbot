import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    kotlin("jvm") version "1.6.0"
}

group = "ru.hukutoc2288"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:5.7.1") {
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-databind")
        because("Too many CVEs")
    }
    implementation(kotlin("stdlib-jdk8"))
    
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    implementation("org.postgresql:postgresql:42.4.3")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.mariuszgromada.math:MathParser.org-mXparser:5.2.1")

    testImplementation("org.slf4j:slf4j-log4j12:2.0.7")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}


val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks.jar {
    manifest.attributes["Main-Class"] = "ru.hukutoc2288.howtoshitbot.MainKt"
    val dependencies = configurations.runtimeClasspath.get().map(::zipTree)
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}