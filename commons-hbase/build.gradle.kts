/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.navercorp.pinpoint.gradle.plugins.toolchain.java11")
}

dependencies {
    api(project(":pinpoint-annotations"))
    api(project(":pinpoint-commons"))
    api(project(":pinpoint-commons-profiler"))
    implementation("org.apache.commons:commons-collections4")
    implementation(libs.jackson.core.asl)
    implementation(libs.spring.core) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(libs.spring.tx)
    implementation(libs.log4j.api)
    runtimeOnly("log4j:log4j")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j-impl:${Versions.log4jJDK8}")
    runtimeOnly(libs.log4j.core)
    runtimeOnly("org.apache.logging.log4j:log4j-jcl:${Versions.log4jJDK8}")

    implementation(libs.hbase.shaded.client) {
        exclude("org.slf4j:slf4j-log4j12")
        exclude("commons-logging:commons-logging")
    }
    implementation(libs.hbasewd) {
        exclude("log4j:log4j")
    }
}

description = "pinpoint-commons-hbase"
