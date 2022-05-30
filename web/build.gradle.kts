/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("com.navercorp.pinpoint.gradle.plugins.toolchain.java11")
    id("com.navercorp.pinpoint.gradle.plugins.bom.plugins-assembly")
    id("com.navercorp.pinpoint.gradle.plugins.bom.agent-plugins")
    id("com.navercorp.pinpoint.gradle.plugins.bom.curator")
    id("org.siouan.frontend-jdk8") version "6.0.0"
}

dependencies {
    api(project(":pinpoint-commons"))
    api(project(":pinpoint-commons-server"))
    api(project(":pinpoint-commons-server-cluster"))
    api(project(":pinpoint-commons-hbase"))
    api(project(":pinpoint-rpc"))
    api(project(":pinpoint-thrift"))
    api(project(":pinpoint-grpc"))
    implementation("com.google.guava:guava:30.1-jre")
    implementation(libs.netty)
    implementation("org.apache.zookeeper:zookeeper")
    implementation(libs.commons.lang3)
    implementation("org.apache.commons:commons-text:1.9")
    implementation(libs.commons.collections4)
    implementation("org.apache.thrift:libthrift:0.15.0")
    implementation(libs.spring.core) {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation(libs.spring.web)
    implementation(libs.spring.webmvc)
    implementation(libs.spring.websocket)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.context)
    implementation(libs.spring.context.support)
    implementation("org.springframework:spring-messaging:${Versions.spring}")
    implementation("org.springframework.security:spring-security-web:5.5.3")
    implementation("org.springframework.security:spring-security-config:5.5.3")
    implementation("org.springframework.security:spring-security-messaging:5.5.3")
    implementation("org.springframework.boot:spring-boot-starter-web:${Versions.springBoot}")
    implementation("org.springframework.boot:spring-boot-starter-log4j2:${Versions.springBoot}")
    implementation(libs.hikariCP)
    implementation(libs.mybatis)
    implementation(libs.mybatis.spring)
    implementation(libs.mysql.connector.java)
    implementation(libs.caffeine)
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.databind)
    implementation(libs.log4j.api)
    implementation("commons-codec:commons-codec")
    implementation("com.sun.mail:jakarta.mail")
    implementation("io.jsonwebtoken:jjwt:0.9.1")
    implementation("javax.xml.bind:jaxb-api:2.3.1")
    implementation("org.aspectj:aspectjweaver:1.9.5")
    runtimeOnly(libs.commons.lang)
    runtimeOnly(libs.slf4j.api)
    runtimeOnly(libs.log4j.jcl)
    runtimeOnly(libs.log4j.slf4j.impl)
    runtimeOnly(libs.log4j.core)
    testImplementation(libs.spring.test)
    testImplementation(libs.json.path)
    testImplementation(project(":pinpoint-profiler"))
    testImplementation(project(":pinpoint-collector"))
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.apache.curator:curator-test")
    testImplementation(project(":pinpoint-rpc"))
    testImplementation("org.skyscreamer:jsonassert:1.5.0")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")
    compileOnly("org.springframework.boot:spring-boot-starter-tomcat:${Versions.springBoot}")

    implementation(libs.hbase.shaded.client) {
        exclude("org.slf4j:slf4j-log4j12")
        exclude("commons-logging:commons-logging")
    }
    implementation(libs.hbasewd) {
        exclude("log4j:log4j")
    }
}

description = "pinpoint-web"

//frontend {
//    nodeVersion.set("14.18.1")
//    assembleScript.set("run build:real")
//    cleanScript.set("run clean")
//    checkScript.set("run check")
//}