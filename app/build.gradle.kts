import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("checkstyle")
    id("jacoco")
    id("application")
    id("io.freefair.lombok") version "8.13.1"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("org.sonarqube") version "6.2.0.5505"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "hexlet.code"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("hexlet.code.App")
}

repositories {
    mavenCentral()
}

tasks.compileJava {
    options.release.set(20)
    options.encoding = "UTF-8"
}

dependencies {
    implementation("io.javalin:javalin:6.6.0")
    implementation("io.javalin:javalin-rendering:6.6.0")
    implementation("org.slf4j:slf4j-simple:2.0.9"){
        exclude(group = "ch.qos.logback")
    }
    implementation("com.konghq:unirest-java:3.14.5")

    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")

    implementation("org.thymeleaf:thymeleaf:3.1.3.RELEASE")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.2.1")
    implementation("org.thymeleaf.extras:thymeleaf-extras-java8time:3.0.4.RELEASE")
    implementation("org.webjars:bootstrap:5.3.0")

    implementation("com.h2database:h2:2.3.232")
    implementation("org.postgresql:postgresql:42.7.3")

    implementation("org.jsoup:jsoup:1.15.4")
    implementation("com.zaxxer:HikariCP:5.0.1")

    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.5")
    implementation("javax.activation:activation:1.1.1")

    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.flywaydb:flyway-core:9.22.3")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation("io.javalin:javalin-testtools:6.1.3")

}

tasks {
    compileJava {
        options.release.set(20)
        options.encoding = "UTF-8"
    }

    withType<Test> {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(TestLogEvent.FAILED, TestLogEvent.PASSED, TestLogEvent.SKIPPED)
            showStandardStreams = true
        }
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

sonar {
    properties {
        property("sonar.projectKey", "CherepovAlex_java-project-72")
        property("sonar.organization", "aleksandrcherepov")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}