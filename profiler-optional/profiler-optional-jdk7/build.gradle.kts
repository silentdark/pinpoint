/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.navercorp.pinpoint.gradle.plugins.toolchain.java7")
    id("com.navercorp.pinpoint.gradle.plugins.bom.asm")
}

dependencies {
    compileOnly(project(":pinpoint-profiler"))
    implementation(libs.log4j.api.jdk7)
    testImplementation("org.ow2.asm:asm")
    testImplementation("org.ow2.asm:asm-commons")
    testImplementation("org.ow2.asm:asm-util")
    testImplementation("org.ow2.asm:asm-tree")
    testImplementation("org.apache.logging.log4j:log4j-slf4j-impl:${Versions.log4jJDK7}")
    testImplementation(libs.log4j.core.jdk7)
}

description = "pinpoint-profiler-optional-jdk7"

sourceSets {
    main {
        java {
            srcDir("src/main/java-ibm")
            srcDir("src/main/java-oracle")
        }
    }
}