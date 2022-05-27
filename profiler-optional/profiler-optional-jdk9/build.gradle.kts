/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.navercorp.pinpoint.gradle.plugins.toolchain.java8")
    id("com.navercorp.pinpoint.gradle.plugins.bom.asm")
}

dependencies {
    compileOnly(project(":pinpoint-profiler"))
    implementation(libs.log4j.api.jdk7)
    testImplementation(project(":pinpoint-test"))
    testImplementation(libs.spring.test)
    testImplementation(libs.spring.context)
    testImplementation("commons-io:commons-io")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:${Versions.log4jJDK7}")
    testImplementation(libs.log4j.core.jdk7)
}

description = "pinpoint-profiler-optional-jdk9"

sourceSets {
    main {
        java {
            srcDir("src/main/java9")
            srcDir("src/main/java11")
        }
    }
}