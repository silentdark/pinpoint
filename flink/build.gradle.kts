/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.navercorp.pinpoint.gradle.plugins.toolchain.java11")
    id("com.navercorp.pinpoint.gradle.plugins.bom.curator")
}

dependencies {
    api(project(":pinpoint-commons-hbase"))
    api(project(":pinpoint-commons-server"))
    api(project(":pinpoint-commons-server-cluster"))
    api(project(":pinpoint-thrift"))
    api(project(":pinpoint-rpc"))
    api(project(":pinpoint-grpc"))
    implementation("org.apache.thrift:libthrift:0.15.0")
    api(project(":pinpoint-web"))
    api(project(":pinpoint-collector"))
    implementation(libs.spring.core) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(libs.spring.context)
    implementation(libs.spring.context.support)
    implementation(libs.caffeine)
    implementation(libs.log4j.api)
    implementation(libs.metrics.core)
    implementation(libs.metrics.jvm)
    implementation(libs.metrics.servlets) {
        exclude(group = "com.papertrail", module = "profiler")
    }
    implementation("org.apache.zookeeper:zookeeper")
    runtimeOnly(libs.slf4j.api)
    testImplementation(libs.log4j.jcl)
    testImplementation(libs.log4j.slf4j.impl)
    testImplementation(libs.log4j.core)
    testImplementation(libs.spring.test)
    compileOnly("org.apache.flink:flink-java:1.14.2")
    compileOnly("org.apache.flink:flink-streaming-java_2.11:1.14.2")
    compileOnly("org.apache.flink:flink-clients_2.11:1.14.2")

    implementation(libs.hbase.shaded.client) {
        exclude("org.slf4j:slf4j-log4j12")
        exclude("commons-logging:commons-logging")
    }
    implementation(libs.hbasewd) {
        exclude("log4j:log4j")
    }
}

description = "pinpoint-flink"
