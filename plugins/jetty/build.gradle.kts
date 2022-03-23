/*
 * This file was generated by the Gradle 'init' task.
 *
 * This project uses @Incubating APIs which are subject to change.
 */

plugins {
    id("pinpoint.java7-conventions")
}

dependencies {
    api(project(":pinpoint-common-servlet"))
    compileOnly(project(":pinpoint-bootstrap-core"))
    compileOnly("org.eclipse.jetty:jetty-server:9.2.11.v20150529")
    compileOnly("org.eclipse.jetty:jetty-servlet:9.2.11.v20150529")
}

description = "pinpoint-jetty-plugin"

sourceSets {
    main {
        java {
            srcDir("src/main/java-jetty")
        }
    }
}
