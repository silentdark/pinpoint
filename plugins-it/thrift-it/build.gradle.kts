/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.navercorp.pinpoint.gradle.plugins.toolchain.java7")
}

dependencies {
    api(project(":pinpoint-plugin-it-utils"))
    testImplementation(libs.libthrift.v012)
    testImplementation("org.eclipse.jetty:jetty-server:9.2.11.v20150529")
}

description = "pinpoint-thrift-plugin-it"
